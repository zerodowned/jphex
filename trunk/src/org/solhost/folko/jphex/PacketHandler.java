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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.solhost.folko.jphex.network.*;
import org.solhost.folko.jphex.types.*;
import org.solhost.folko.uosl.data.SLStatic;
import org.solhost.folko.uosl.network.packets.*;
import org.solhost.folko.uosl.types.Attribute;
import org.solhost.folko.uosl.types.Direction;
import org.solhost.folko.uosl.types.Items;
import org.solhost.folko.uosl.types.Point3D;
import org.solhost.folko.uosl.types.Spell;

// translates from network packets to world actions
public class PacketHandler implements IPacketHandler {
    private static final Logger log = Logger.getLogger("jphex.packethandler");
    private final Map<Client, Player> playerClients;
    private final ObjectRegistry registry;
    private final World world;

    public PacketHandler(World world) {
        this.world = world;
        this.playerClients = new HashMap<Client, Player>();
        this.registry = ObjectRegistry.get();
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
        Player player = playerClients.get(client);
        if(player != null) {
            world.logoutPlayer(player);
            player.setClient(null);
            playerClients.remove(client);
        }
    }

    private void onLoginRequest(Client client, LoginPacket packet) {
        long serial = packet.getSerial();
        if(serial == 0) {
            onCreatePlayer(client, packet);
        } else if(registry.findPlayer(serial) != null) {
            // existing player
            Player player = registry.findPlayer(serial);
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
            playerClients.put(client, player);
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

        long serial = registry.registerMobileSerial();
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
        player.setFacing(Direction.EAST);

        playerClients.put(client, player);
        player.setClient(client);
        world.onCreatePlayer(player);
        registry.registerObject(player);
        world.sendInitSequence(player);
        world.loginPlayer(player);
    }

    private void onMoveRequest(Client client, MoveRequestPacket packet) {
        Player player = playerClients.get(client);
        if(world.onPlayerRequestMove(player, packet.getDirection())) {
            client.send(new AllowMovePacket(packet.getSequence()));
        } else {
            client.send(new DenyMovePacket(player, packet.getSequence()));
        }
    }

    private void onSingleClick(Client client, SingleClickPacket packet) {
        Player player = playerClients.get(client);
        SLObject object = registry.findObject(packet.getSerial());
        if(object != null) {
            world.onSingleClick(player, object);
        }
    }

    private void onDoubleClick(Client client, DoubleClickPacket packet) {
        Player player = playerClients.get(client);
        SLObject obj = registry.findObject(packet.getSerial());
        if(obj != null) {
            world.onDoubleClick(player, obj);
        } else {
            SLStatic stat = registry.findStatic(packet.getSerial());
            if(stat != null) {
                world.onDoubleClickStatic(player, stat);
            } else {
                log.warning(String.format("Player %s doubleclicks unknown %08X", player.getName(), packet.getSerial()));
            }
        }
    }

    private void onSpeechRequest(Client client, SpeechRequestPacket packet) {
        Player player = playerClients.get(client);
        world.onSpeech(player, packet.getText(), packet.getColor());
    }

    private void onRequest(Client client, RequestPacket packet) {
        Player player = playerClients.get(client);
        if(packet.getMode() == RequestPacket.MODE_SKILLS) {
            Mobile who = registry.findMobile(packet.getSerial());
            if(who == null) {
                return;
            }
            world.onSkillRequest(player, who);
        } else if(packet.getMode() == RequestPacket.MODE_STATS) {
            Mobile who = registry.findMobile(packet.getSerial());
            if(who == null) {
                return;
            }
            world.onStatusRequest(player, who);
        } else if(packet.getMode() == RequestPacket.MODE_COUNT0) {
            // the client sends this 1024 times at login with serial being the count
        } else if(packet.getMode() == RequestPacket.MODE_COUNT1) {
            // the client sends this 32 times after login with serial being the count
        } else {
            log.warning(String.format("Player %s requesting unknown mode %d on 0x%08X", player.getName(), packet.getMode(), packet.getSerial()));
        }
    }

    private void onDrag(Client client, DragPacket packet) {
        Player player = playerClients.get(client);
        Item item = registry.findItem(packet.getSerial());
        if(item == null) {
            player.sendSysMessage("I cannot move that.");
            client.send(new CancelDragPacket());
            return;
        }
        if(!world.onDrag(player, item, packet.getAmount())) {
            client.send(new CancelDragPacket());
            return;
        }
    }

    private void onDrop(Client client, DropPacket packet) {
        Player player = playerClients.get(client);
        Item item = registry.findItem(packet.getSerial());
        if(item == null) {
            return;
        }
        Item dropOn = registry.findItem(packet.getContainer());
        if(dropOn == null) {
            world.onDrop(player, item, null, packet.getLocation());
        } else {
            world.onDrop(player, item, dropOn, packet.getLocation());
        }
    }

    private void onEquip(Client client, EquipReqPacket packet) {
        Player player = playerClients.get(client);
        Item item = registry.findItem(packet.getItemSerial());
        Mobile mob = registry.findMobile(packet.getMobSerial());
        short layer = packet.getLayer();
        if(item == null || mob == null || !world.onEquip(player, item, mob, layer)) {
            world.cancelDrag(player, item);
        }
    }

    private void onAction(Client client, ActionPacket packet) {
        Player player = playerClients.get(client);
        String action = packet.getAction();
        String parts[] = action.split(" ");

        if(packet.getMode() == ActionPacket.MODE_OPEN_SPELLBOOK) {
            world.doOpenSpellbook(player);
        } else if(packet.getMode() == ActionPacket.MODE_CAST_SPELL) {
            if(action.startsWith("In Mani Ylem")){
                world.onCastSpell(player, Spell.CREATEFOOD, null, null, null);
            } else if(action.startsWith("Por Flam")){
                long serial = Long.valueOf(parts[2]);
                Mobile target = registry.findMobile(serial);
                if(target != null) {
                    world.onCastSpell(player, Spell.FIREBALL, null, null, target);
                }
            } else if(action.startsWith("Mani")){
                long serial = Long.valueOf(parts[1]);
                Mobile target = registry.findMobile(serial);
                if(target != null) {
                    world.onCastSpell(player, Spell.HEALING, null, null, target);
                }
            } else if(action.startsWith("In Lor")){
                world.onCastSpell(player, Spell.LIGHT, null, null, null);
            } else if(action.startsWith("In Vas Lor")){
                world.onCastSpell(player, Spell.GREATLIGHT, null, null, null);
            } else if(action.startsWith("Quas An Lor")){
                int x = Integer.valueOf(parts[3]);
                int y = Integer.valueOf(parts[4]);
                int z = Integer.valueOf(parts[5]);
                world.onCastSpell(player, Spell.DARKSOURCE, null, new Point3D(x, y, z), null);
            } else if(action.startsWith("Quas In Lor")){
                int x = Integer.valueOf(parts[3]);
                int y = Integer.valueOf(parts[4]);
                int z = Integer.valueOf(parts[5]);
                if(player.hasLocationTarget()) {
                    player.onTargetLocation(new Point3D(x, y, z));
                } else {
                    world.onCastSpell(player, Spell.LIGHTSOURCE, null, new Point3D(x, y, z), null);
                }
            }
        } else if(packet.getMode() == ActionPacket.MODE_USE_SCROLL) {
            Item scroll = registry.findItem(Long.valueOf(parts[0]));
            if(scroll == null) {
                return;
            }
            switch(scroll.getGraphic()) {
            case Items.GFX_SCROLL_CREATEFOOD: {
                world.onCastSpell(player, Spell.CREATEFOOD, scroll, null, null);
                break;
            }
            case Items.GFX_SCROLL_FIREBALL: {
                long serial = Long.valueOf(parts[1]);
                Mobile target = registry.findMobile(serial);
                if(target != null) {
                    world.onCastSpell(player, Spell.FIREBALL, scroll, null, target);
                }
                break;
            }
            case Items.GFX_SCROLL_HEALING: {
                long serial = Long.valueOf(parts[1]);
                Mobile target = registry.findMobile(serial);
                if(target != null) {
                    world.onCastSpell(player, Spell.HEALING, scroll, null, target);
                }
                break;
            }
            case Items.GFX_SCROLL_LIGHT: {
                world.onCastSpell(player, Spell.LIGHT, scroll, null, null);
                break;
            }
            case Items.GFX_SCROLL_GREATLIGHT: {
                world.onCastSpell(player, Spell.GREATLIGHT, scroll, null, null);
                break;
            }
            case Items.GFX_SCROLL_DARKSOURCE: {
                int x = Integer.valueOf(parts[1]);
                int y = Integer.valueOf(parts[2]);
                int z = Integer.valueOf(parts[3]);
                world.onCastSpell(player, Spell.DARKSOURCE, scroll, new Point3D(x, y, z), null);
                break;
            }
            case Items.GFX_SCROLL_LIGHTSOURCE: {
                int x = Integer.valueOf(parts[1]);
                int y = Integer.valueOf(parts[2]);
                int z = Integer.valueOf(parts[3]);
                world.onCastSpell(player, Spell.LIGHTSOURCE, scroll, new Point3D(x, y, z), null);
                break;
            }}
        } else {
            log.finer("Player " + player.getName() + " requesting unknown action: " + action + " (mode " + packet.getMode() + ")");
        }
    }

    private void onShopAction(Client client, ShopPacket packet) {
        Player player = playerClients.get(client);
        Mobile shop = registry.findMobile(packet.getShopSerial());
        if(shop != null) {
            world.onShopAction(player, shop, packet.getAction());
        }
    }

    private void onAttack(Client client, AttackPacket packet) {
        Player player = playerClients.get(client);
        Mobile victim = registry.findMobile(packet.getVictimSerial());
        if(victim != null) {
            world.onAttack(player, victim);
        }
    }
}
