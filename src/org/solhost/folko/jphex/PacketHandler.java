/*******************************************************************************
 * Copyright (c) 2013 Folke Will <folke.will@gmail.com>
 * 
 * This file is part of JPhex.
 * 
 * JPhex is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JPhex is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.solhost.folko.jphex;

import java.util.logging.Logger;

import org.solhost.folko.jphex.network.*;
import org.solhost.folko.jphex.types.*;
import org.solhost.folko.uosl.data.SLStatic;
import org.solhost.folko.uosl.network.packets.*;
import org.solhost.folko.uosl.types.Attribute;
import org.solhost.folko.uosl.types.Direction;
import org.solhost.folko.uosl.types.Mobiles;
import org.solhost.folko.uosl.types.Point3D;
import org.solhost.folko.uosl.types.Spell;

// translates from network packets to world actions
public class PacketHandler implements IPacketHandler {
    private static final Logger log = Logger.getLogger("jphex.packethandler");
    private final World world;

    public PacketHandler(World world) {
        this.world = world;
    }

    public void onIncomingPacket(Client client, SLPacket packet) {
        switch(packet.getID()) {
        case LoginPacket.ID:            onLoginRequest(client, (LoginPacket) packet); break;
        case RequestPacket.ID:          onRequest(client, (RequestPacket) packet); break;
        case MoveRequestPacket.ID:      onMoveRequest(client, (MoveRequestPacket) packet); break;
        case SingleClickPacket.ID:      onSingleClick(client, (SingleClickPacket) packet); break;
        case DoubleClickPacket.ID:      onDoubleClick(client, (DoubleClickPacket) packet); break;
        case AttackPacket.ID:           onAttack(client, (AttackPacket) packet); break;
        case SpeechRequestPacket.ID:    onSpeechRequest(client, (SpeechRequestPacket) packet); break;
        case DragPacket.ID:             onDrag(client, (DragPacket) packet); break;
        case DropPacket.ID:             onDrop(client, (DropPacket) packet); break;
        case EquipReqPacket.ID:         onEquip(client, (EquipReqPacket) packet);  break;
        case ActionPacket.ID:           onAction(client, (ActionPacket) packet); break;
        case ShopPacket.ID:             onShopAction(client, (ShopPacket) packet); break;
        default:
            log.fine("Unknown packet from " + client.getRemoteAddress() + ": " + packet);
        }
    }

    public void onNewConnection(Client client) {
        log.info("Connection from " + client.getRemoteAddress());
    }

    public void onDisconnect(Client client) {
        log.info("Disconnect from " + client.getRemoteAddress());
        Player player = world.getPlayer(client);
        if(player != null) {
            world.logoutPlayer(player);
            player.setClient(null);
        }
    }

    private void onLoginRequest(Client client, LoginPacket packet) {
        long serial = packet.getSerial();
        if(serial == 0) {
            onCreatePlayer(client, packet);
        } else if(world.findObject(serial) != null) {
            // existing player
            SLObject obj = world.findObject(serial);
            if(!(obj instanceof Player)) {
                // something's wrong
                client.send(new LoginErrorPacket(LoginErrorPacket.REASON_CHAR_NOT_FOUND));
                return;
            }
            Player player = (Player) obj;
            if(player.getSeed() != packet.getSeed()) {
                // client has old version of the player or something
                client.send(new LoginErrorPacket(LoginErrorPacket.REASON_CHAR_NOT_FOUND));
                return;
            }
            if(!player.getPassword().equals(packet.getPassword())) {
                client.send(new LoginErrorPacket(LoginErrorPacket.REASON_PASSWORD));
                return;
            }
            if(player.isOnline()) {
                client.send(new LoginErrorPacket(LoginErrorPacket.REASON_OTHER));
                return;
            }

            // everything ok
            player.setClient(client);
            world.sendInitSequence(player);
            world.loginPlayer(player);
        } else {
            // unknown player
            client.send(new LoginErrorPacket(LoginErrorPacket.REASON_CHAR_NOT_FOUND));
        }
    }

    private void onCreatePlayer(Client client, LoginPacket packet) {
        int statSum = packet.getStrength() + packet.getDexterity() + packet.getIntelligence();
        if(statSum == 0) {
            // this happens if a previous creation failed
            client.send(new LoginErrorPacket(LoginErrorPacket.REASON_CHAR_NOT_FOUND));
            return;
        }

        long serial = world.registerMobileSerial();
        Player player = new Player(serial);

        // TODO: verify values (stat sum ~100, max each stat = 75 etc)

        player.setSeed(Util.random(0, Integer.MAX_VALUE));
        player.setName(packet.getName());
        player.setHomepage(packet.getHomepage());
        player.setEmail(packet.getEmail());
        player.setRealName(packet.getRealName());
        player.setPcSpecs(packet.getPcSpecs());
        player.setPassword(packet.getPassword());
        player.setGraphic(packet.getGender());
        player.setAttribute(Attribute.STRENGTH, packet.getStrength());
        player.setAttribute(Attribute.DEXTERITY, packet.getDexterity());
        player.setAttribute(Attribute.INTELLIGENCE, packet.getIntelligence());
        player.setHue(packet.getSkinHue());
        player.setHairHue(packet.getHairHue());
        player.setHairStyle(packet.getHairStyle());

        player.setAttribute(Attribute.EXPERIENCE, 0);
        player.setAttribute(Attribute.LEVEL, 1);
        player.refreshStats();

        player.setGraphic(packet.getGender());
        player.setLocation(new Point3D(379, 607, 0));
        player.setFacing(Direction.EAST);
        player.setMale(player.getGraphic() == Mobiles.MOBTYPE_HUMAN_MALE);

        player.setClient(client);
        world.registerObject(player);
        world.sendInitSequence(player);
        world.onCreatePlayer(player);
        world.loginPlayer(player);
    }

    private void onMoveRequest(Client client, MoveRequestPacket packet) {
        Player player = world.getPlayer(client);
        if(world.onPlayerRequestMove(player, packet.getDirection())) {
            client.send(new AllowMovePacket(packet.getSequence()));
        } else {
            client.send(new DenyMovePacket(player, packet.getSequence()));
        }
    }

    private void onSingleClick(Client client, SingleClickPacket packet) {
        Player player = world.getPlayer(client);
        SLObject object = world.findObject(packet.getSerial());
        if(object != null) {
            world.onSingleClick(player, object);
        }
    }

    private void onDoubleClick(Client client, DoubleClickPacket packet) {
        Player player = world.getPlayer(client);
        SLObject obj = world.findObject(packet.getSerial());
        if(obj != null) {
            world.onDoubleClick(player, obj);
        } else {
            SLStatic stat = world.findStatic(packet.getSerial());
            if(stat != null) {
                world.onDoubleClickStatic(player, stat);
            } else {
                log.warning(String.format("Player %s doubleclicks unknown %08X", player.getName(), packet.getSerial()));
            }
        }
    }

    private void onSpeechRequest(Client client, SpeechRequestPacket packet) {
        Player player = world.getPlayer(client);
        world.onSpeech(player, packet.getText(), packet.getColor());
    }

    private void onRequest(Client client, RequestPacket packet) {
        Player player = world.getPlayer(client);
        if(packet.getMode() == RequestPacket.MODE_SKILLS) {
            SLObject what = world.findObject(packet.getSerial());
            if(!(what instanceof Mobile)) {
                return;
            }
            world.onSkillRequest(player, (Mobile) what);
        } else if(packet.getMode() == RequestPacket.MODE_STATS) {
            SLObject what = world.findObject(packet.getSerial());
            if(!(what instanceof Mobile)) {
                return;
            }
            world.onStatusRequest(player, (Mobile) what);
        } else if(packet.getMode() == RequestPacket.MODE_COUNT0) {
            // the client sends this 1024 times at login with serial being the count
        } else if(packet.getMode() == RequestPacket.MODE_COUNT1) {
            // the client sends this 32 times after login with serial being the count
        } else {
            log.warning(String.format("Player %s requesting unknown mode %d on 0x%08X", player.getName(), packet.getMode(), packet.getSerial()));
        }
    }

    private void onDrag(Client client, DragPacket packet) {
        Player player = world.getPlayer(client);
        SLObject object = world.findObject(packet.getSerial());
        if(object == null || !(object instanceof Item)) {
            player.sendSysMessage("I cannot move that.");
            client.send(new CancelDragPacket());
            return;
        }
        Item item = (Item) object;
        if(!world.onDrag(player, item, packet.getAmount())) {
            client.send(new CancelDragPacket());
            return;
        }
    }

    private void onDrop(Client client, DropPacket packet) {
        Player player = world.getPlayer(client);
        SLObject object = world.findObject(packet.getSerial());
        if(object == null || !(object instanceof Item)) {
            return;
        }
        Item item = (Item) object;
        SLObject dropOn = world.findObject(packet.getContainer());
        if(dropOn == null || !(dropOn instanceof Item)) {
            world.onDrop(player, item, null, packet.getLocation());
        } else {
            world.onDrop(player, item, (Item) dropOn, packet.getLocation());
        }
    }

    private void onEquip(Client client, EquipReqPacket packet) {
        Player player = world.getPlayer(client);
        SLObject item = world.findObject(packet.getItemSerial());
        SLObject mob = world.findObject(packet.getMobSerial());
        short layer = packet.getLayer();
        if(item instanceof Item && mob instanceof Mobile) {
            if(!world.onEquip(player, (Item) item, (Mobile) mob, layer)) {
                world.cancelDrag(player, (Item) item);
            }
        } else {
            world.cancelDrag(player, (Item) item);
        }
    }

    private void onAction(Client client, ActionPacket packet) {
        Player player = world.getPlayer(client);
        String action = packet.getAction();
        String parts[] = action.split(" ");

        if(packet.getMode() == ActionPacket.MODE_OPEN_SPELLBOOK) {
            world.doOpenSpellbook(player);
        } else if(action.startsWith("In Mani Ylem")){
            world.onCastSpell(player, Spell.CREATEFOOD, null, null);
        } else if(action.startsWith("Por Flam")){
            long serial = Long.valueOf(parts[2]);
            SLObject target = world.findObject(serial);
            if(target != null && target instanceof Mobile) {
                world.onCastSpell(player, Spell.FIREBALL, null, (Mobile) target);
            }
        } else if(action.startsWith("Mani")){
            long serial = Long.valueOf(parts[1]);
            SLObject target = world.findObject(serial);
            if(target != null && target instanceof Mobile) {
                world.onCastSpell(player, Spell.HEALING, null, (Mobile) target);
            }
        } else if(action.startsWith("In Lor")){
            world.onCastSpell(player, Spell.LIGHT, null, null);
        } else if(action.startsWith("In Vas Lor")){
            world.onCastSpell(player, Spell.GREATLIGHT, null, null);
        } else if(action.startsWith("Quas An Lor")){
            int x = Integer.valueOf(parts[3]);
            int y = Integer.valueOf(parts[4]);
            int z = Integer.valueOf(parts[5]);
            world.onCastSpell(player, Spell.DARKSOURCE, new Point3D(x, y, z), null);
        } else if(action.startsWith("Quas In Lor")){
            int x = Integer.valueOf(parts[3]);
            int y = Integer.valueOf(parts[4]);
            int z = Integer.valueOf(parts[5]);
            if(player.hasLocationTarget()) {
                player.onTargetLocation(new Point3D(x, y, z));
            } else {
                world.onCastSpell(player, Spell.LIGHTSOURCE, new Point3D(x, y, z), null);
            }
        } else {
            log.finer("Player " + player.getName() + " requesting unknown action: " + action + " (mode " + packet.getMode() + ")");
        }
    }

    private void onShopAction(Client client, ShopPacket packet) {
        Player player = world.getPlayer(client);
        SLObject shop = world.findObject(packet.getShopSerial());
        if(shop instanceof Mobile) {
            world.onShopAction(player, (Mobile) shop, packet.getAction());
        }
    }

    private void onAttack(Client client, AttackPacket packet) {
        Player player = world.getPlayer(client);
        SLObject victim = world.findObject(packet.getVictimSerial());
        if(victim instanceof Mobile) {
            world.onAttack(player, (Mobile) victim);
        }
    }
}
