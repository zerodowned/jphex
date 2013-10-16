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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.solhost.folko.jphex.DayNightCycle.TimeListener;
import org.solhost.folko.jphex.ObjectRegistry.SerialObserver;
import org.solhost.folko.jphex.network.*;
import org.solhost.folko.jphex.scripting.*;
import org.solhost.folko.jphex.types.*;
import org.solhost.folko.uosl.data.*;
import org.solhost.folko.uosl.network.packets.*;
import org.solhost.folko.uosl.types.*;
import org.solhost.folko.uosl.util.ObjectLister;

public class World implements ObjectObserver, SerialObserver, ObjectLister, TimeListener {
    public static final int VISIBLE_RANGE = 15;
    public static final int SPEECH_RANGE = 10;
    public static final int ENTER_AREA_RANGE = 5;
    public static final int STAT_REFRESH_DELAY = 1200;

    public static final int SECONDS_PER_INGAME_HOUR = 120;

    // Sweet Dreams Inn
    public static final int NEW_CHAR_X = 553;
    public static final int NEW_CHAR_Y = 575;

    // temple in Britain
    public static final int RESURRECT_POSITION_X = 507;
    public static final int RESURRECT_POSITION_Y = 584;

    private static final Logger log = Logger.getLogger("jphex.world");
    private final String savePath;

    private ObjectRegistry registry;
    private final Map<Client, Player> onlinePlayers;
    private final DayNightCycle dayNightCycle;

    private World(String savePath) {
        this.onlinePlayers = new HashMap<Client, Player>();
        this.savePath = savePath;
        this.dayNightCycle = new DayNightCycle(this, SECONDS_PER_INGAME_HOUR);
    }

    public static World loadOrCreateNew(String savePath) throws Exception {
        File file = new File(savePath + "/save.ser");
        World world = new World(savePath);
        Map<Long, SLObject> objects = new HashMap<Long, SLObject>();
        Map<SLObject, Long> orphans = new HashMap<SLObject, Long>();
        if(!file.exists()) {
            log.config("Creating a fresh save");
        } else {
            log.config("Loading an existing save");
            FileInputStream saveFile = new FileInputStream(file);
            ObjectInputStream objIn = new ObjectInputStream(saveFile);
            int num = objIn.readInt();
            for(int i = 0; i < num; i++) {
                SLObject obj = (SLObject) objIn.readObject();
                objects.put(obj.getSerial(), obj);
                long parentSerial = objIn.readLong();
                orphans.put(obj, parentSerial);
            }
            objIn.close();
            saveFile.close();
        }
        ObjectRegistry.init(SLData.get().getStatics().getAllStatics(), objects);
        world.registry = ObjectRegistry.get();

        for(SLObject orphan : orphans.keySet()) {
            long parentSerial = orphans.get(orphan);
            if(parentSerial == -1) {
                continue;
            }

            SLObject parent = world.registry.findObject(parentSerial);
            if(parent == null) {
                log.severe(String.format("Couldn't find parent %08X for %08X, deleting orphan", parentSerial, orphan.getSerial()));
                world.registry.removeObject(orphan.getSerial());
                orphan.delete();
            } else {
                parent.foundOrphan(orphan);
            }
        }

        world.registry.addObserver(world);
        return world;
    }

    public synchronized boolean save() {
        log.info("Saving world state...");
        broadcast("Saving world state...");
        try {
            FileOutputStream saveFile = new FileOutputStream(savePath + "/save.ser", false);
            ObjectOutputStream objOut = new ObjectOutputStream(saveFile);
            Collection<SLObject> all = registry.allObjects();
            objOut.writeInt(all.size());
            for(SLObject obj : all) {
                objOut.writeObject(obj);
                SLObject parent = obj.getParent();
                if(parent != null) {
                    objOut.writeLong(obj.getParent().getSerial());
                } else {
                    objOut.writeLong(-1);
                }
            }
            objOut.close();
            saveFile.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error saving world: " + e.getMessage(), e);
            return false;
        }
        broadcast("Done saving");
        return true;
    }

    // must be called after creating a new world or after loading a world, do initialization here
    // scripts must be able to execute when calling this
    public synchronized void init() {
        // second pass: call init scripts etc.
        for(SLObject obj : registry.allObjects()) {
            obj.onLoad();
            if(obj.isDeleted()) {
                registry.removeObject(obj.getSerial());
                continue;
            }
            if(obj instanceof Mobile) {
                Mobile mob = (Mobile) obj;
                mob.setOpponent(null);
                mob.setRefreshRunning(false);
                runRefresh(mob);
            }
            obj.addObserver(this);
        }
        dayNightCycle.start();
    }

    public synchronized Collection<Player> getOnlinePlayersInRange(Point2D point, int range) {
        List<Player> res = new LinkedList<Player>();
        for(Player p : onlinePlayers.values()) {
            if(p.isOnline() && p.inRange(point, range)) {
                res.add(p);
            }
        }
        return res;
    }

    public synchronized Collection<Player> getOnlinePlayers() {
        List<Player> res = new LinkedList<Player>();
        for(Player p : onlinePlayers.values()) {
            if(p.isOnline()) {
                res.add(p);
            }
        }
        return res;
    }

    // objects on ground
    public synchronized Collection<SLObject> getObjectsInRange(Point2D point, int range) {
        List<SLObject> res = new LinkedList<SLObject>();
        for(SLObject obj : registry.allObjects()) {
            if(obj instanceof Item && !((Item) obj).isOnGround()) continue;

            if(obj.isVisible() && obj.inRange(point, range)) {
                res.add(obj);
            }
        }
        return res;
    }

    public synchronized void sendInitSequence(Player player) {
        SLPacket init = new InitPlayerPacket(player, player.getSeed());
        player.getClient().send(init);
        player.getClient().send(new SendInitialStatsPacket(player));

        SLPacket locationPacket = new LocationPacket(player);
        player.getClient().send(locationPacket);
    }

    public synchronized void loginPlayer(Player player) {
        onlinePlayers.put(player.getClient(), player);
        log.info(player.getName() + " logged in, " + onlinePlayers.size() + " online");

        sendFullEquipment(player, player);

        // send scene to player
        for(SLObject obj : getObjectsInRange(player.getLocation(), VISIBLE_RANGE)) {
            if(obj.isVisible() && obj != player) {
                sendObject(player, obj);
                if(obj instanceof Mobile) {
                    sendFullEquipment(player, (Mobile) obj);
                }
                if(obj instanceof NPC) {
                    ((NPC) obj).onEnterArea(player);
                }
            }
        }
        runRefresh(player);

        GlobalLightLevelPacket packet = new GlobalLightLevelPacket(dayNightCycle.getLightLevel());
        player.sendPacket(packet);

        player.sendSysMessage("Welcome to JPhex");
    }

    public synchronized void logoutPlayer(Player player) {
        onlinePlayers.remove(player.getClient());
        log.info(player.getName() + " logged out, " + onlinePlayers.size() + " online");
    }

    public synchronized Player getPlayer(Client client) {
        return onlinePlayers.get(client);
    }

    public synchronized void onCreatePlayer(Player player) {
        player.setLocation(new Point3D(NEW_CHAR_X, NEW_CHAR_Y, 0));

        Item backpack = Item.createEquipped(player, Items.GFX_BACKPACK, 0);
        Item.createEquipped(player, Items.GFX_DAGGER, 0);
        Item.createEquipped(player, Items.GFX_TUNIC, Util.random(23, 40));
        if(player.getGraphic() == Mobiles.MOBTYPE_HUMAN_MALE) {
            if(player.getHairStyle() != 2) {
                Item.createEquipped(player, Items.GFX_HAIR_START + player.getHairStyle(), player.getHairHue() + 7);
            }
            Item.createEquipped(player, Items.GFX_PANTS, Util.random(23, 40));
        } else {
            Item.createEquipped(player, Items.GFX_HAIR_START + 3 + player.getHairStyle(), player.getHairHue() + 7);
            Item.createEquipped(player, Items.GFX_SKIRT, Util.random(23, 40));
        }
        Item.createInContainer(backpack, Items.GFX_GOLD, 100);

        if(player.getSerial() == 1) {
            player.setCommandLevel(CommandLevel.ADMIN);
            Item spellbook = Item.createInContainer(backpack, Items.GFX_SPELLBOOK, 1);
            Item.createInContainer(spellbook, Items.GFX_SCROLL_LIGHTSOURCE, 1);
            Item.createInContainer(spellbook, Items.GFX_SCROLL_DARKSOURCE, 1);
            Item.createInContainer(spellbook, Items.GFX_SCROLL_GREATLIGHT, 1);
            Item.createInContainer(spellbook, Items.GFX_SCROLL_LIGHT, 1);
            Item.createInContainer(spellbook, Items.GFX_SCROLL_HEALING, 1);
            Item.createInContainer(spellbook, Items.GFX_SCROLL_FIREBALL, 1);
            Item.createInContainer(spellbook, Items.GFX_SCROLL_CREATEFOOD, 1);
        }
    }

    public synchronized void broadcast(String message) {
        for(Player player : getOnlinePlayers()) {
            player.sendSysMessage(message);
        }
    }

    @Override
    public synchronized List<SLStatic> getStaticAndDynamicsAtLocation(Point2D loc) {
        List<SLStatic> res = new LinkedList<SLStatic>(SLData.get().getStatics().getStatics(loc));
        for(SLObject obj : getObjectsInRange(loc, 0)) {
            if(obj instanceof Item && loc.equals(obj.getLocation())) {
                SLStatic dynamic = new SLStatic(obj.getSerial());
                dynamic.setLocation(obj.getLocation());
                dynamic.setStaticID(obj.getGraphic());
            }
        }
        return res;
    }

    public synchronized  boolean onPlayerRequestMove(Player player, Direction dir) {
        if(player.getFacing() != dir) {
            // player is only turning and not actually moving -> always allow
            player.setWalking(true);
            player.setFacing(dir);
            player.setWalking(false);
        } else {
            Point3D dest = canWalk(player, dir);
            if(dest == null) {
                return false;
            }
            // player is actually moving, calculate new location
            player.setWalking(true);
            player.setLocation(dest);
            player.setWalking(false);
        }
        return true;
    }

    // if yes, returns dest point, otherwise null
    public Point3D canWalk(Mobile who, Direction dir) {
        Point3D dest = SLData.get().getElevatedPoint(who.getLocation(), dir, this);
        return dest;
    }

    public synchronized void onDoubleClick(Player player, SLObject obj) {
        if(player.hasObjectTarget()) {
            player.onTargetObject(obj);
            return;
        }

        if(obj instanceof Player) {
            sendPaperdoll(player, (Player) obj);
        } else if(obj instanceof NPC) {
            NPC npc = (NPC) obj;
            if(npc.onDoubleClick(player)) {
                sendPaperdoll(player, npc);
            }
        } else if (obj instanceof Item) {
            Item itm = (Item) obj;
            if(!player.tryAccess(itm)) {
                return;
            }
            if(itm.isContainer()) {
                sendContainer(player, itm, true);
            } else {
                itm.onUse(player);
            }
        }
    }

    public synchronized void onSkillRequest(Player player, Mobile what) {
        if(player == what) {
            sendSkills(player, true);
        } else {
            player.sendSysMessage("You can't examine their skills!");
        }
    }

    public synchronized void onStatusRequest(Player player, Mobile what) {
        sendStats(player, what);
    }

    public synchronized void onDoubleClickStatic(Player player, SLStatic stat) {
        log.finer(String.format("%s doubleclicks static 0x%08X", player.getName(), stat.getSerial()));
        return;
    }

    public synchronized void sendPaperdoll(Player requester, Mobile whose) {
        OpenGumpPacket gump = new OpenGumpPacket(whose, OpenGumpPacket.GUMP_ID_PAPERDOLL);
        requester.getClient().send(gump);
    }

    public synchronized void sendSkills(Player player,  boolean openWindow) {
        SkillsPacket skills = new SkillsPacket(player, openWindow);
        player.getClient().send(skills);
    }

    public synchronized void sendStats(Player player, Mobile obj) {
        StatsPacket stats;
        if(player == obj) {
            stats = new StatsPacket(player);
        } else {
            int hits = (int) obj.getAttribute(Attribute.HITS);
            int maxHits = (int) obj.getAttribute(Attribute.MAX_HITS);
            stats = new StatsPacket(obj, hits * 100 / maxHits, 100);
        }
        player.getClient().send(stats);
    }

    public synchronized void sendObject(Player player, SLObject obj) {
        if(player == obj) {
            player.sendLocation();
            return;
        }

        if(obj instanceof Item) {
            Item i = (Item) obj;
            if(i.isOnGround()) {
                SendObjectPacket packet;
                packet = new SendObjectPacket(i);
                player.getClient().send(packet);
            } else if(i.isWorn()) {
                Mobile wearer = (Mobile) i.getParent();
                EquipPacket equip = new EquipPacket(wearer, i);
                player.getClient().send(equip);
            } else if(i.isInContainer()) {
                Item container = (Item) i.getParent();
                ItemInContainerPacket packet = new ItemInContainerPacket(i, container);
                player.getClient().send(packet);
            }
        } else if(obj instanceof Mobile) {
            Mobile m = (Mobile) obj;
            SendObjectPacket packet;
            packet = new SendObjectPacket(m);
            player.getClient().send(packet);
        } else {
            throw new RuntimeException("sendObject: don't know how to send " + obj);
        }
    }

    private synchronized void sendFullEquipment(Player player, Mobile mob) {
        log.finer(String.format("sending %d equipped items of %s to %s", mob.getEquippedItems().size(), mob.getName(), player.getName()));
        for(Item item : mob.getEquippedItems()) {
            log.finer(String.format("%s wears %08X", mob.getName(), item.getSerial()));
            sendObject(player, item);
        }
    }

    public synchronized void sendDelete(Player player, SLObject obj) {
        player.getClient().send(new RemoveObjectPacket(obj));
    }

    public synchronized boolean onDrag(Player player, Item item, int amount) {
        if(!player.tryAccess(item)) {
            return false;
        }
        if(item.getAmount() < amount) {
            return false;
        }
        item.setDragged(player);
        player.setDragAmount(amount);

        if(amount != 0 && amount < item.getAmount()) {
            // not dragging the whole pile -> need to create new pile
            Item newPile = item.createCopy(registry.registerItemSerial());
            newPile.setAmount(item.getAmount() - amount);
            registry.registerObject(newPile);
            if(item.getParent() != null) {
                Item container = (Item) item.getParent();
                container.addChild(newPile, newPile.getLocation());
            }
        }

        // remove the item from screen because it's being dragged
        item.rememberParent();
        SLObject parent = item.getParent();
        if(parent != null) {
            if(parent instanceof Item) {
                Item container = (Item) parent;
                container.removeChild(item);
            } else if(parent instanceof Mobile) {
                Mobile mob = (Mobile) parent;
                mob.unequipItem(item);
            }
        }

        return true;
    }

    public synchronized void onDrop(Player player, Item item, Item dropOn, Point3D loc) {
        int amount = player.getDragAmount();
        player.setDragAmount(0);
        item.setAmount(amount);
        item.dropped();
        if(dropOn == null) {
            // to groud
            item.clearParent();
            item.setLocation(loc);
        } else {
            // to container or stack
            if(player.tryAccess(dropOn)) {
                if(dropOn.isContainer()) {
                    if(dropOn.acceptsChild(item)) {
                        dropOn.addChild(item, loc);
                    } else {
                        player.sendSysMessage("You can't put that there.");
                        cancelDrag(player, item);
                    }
                    return;
                } else {
                    dropOn.setAmount(dropOn.getAmount() + amount);
                    item.delete();
                    return;
                }
            } else {
                cancelDrag(player, item);
                return;
            }
        }
    }

    public synchronized void cancelDrag(Player player, Item item) {
        player.setDragAmount(0);
        item.dropped();
        item.restoreParent();
        // trigger observers
        if(item.isOnGround()) {
            log.finest("cancelDrag -> toGround");
            item.setLocation(item.getLocation());
        } else {
            SLObject parent = item.getParent();
            if(parent instanceof Item) {
                log.finest("cancelDrag -> toContainer");
                ((Item) parent).addChild(item, item.getLocation());
            } else if(parent instanceof Mobile) {
                log.finest("cancelDrag -> toEquip");
                ((Mobile) parent).equipItem(item);
            }
        }
    }

    public synchronized boolean onEquip(Player player, Item item, Mobile mob, short layer) {
        if(player == mob) {
            if(layer != item.getLayer()) {
                log.warning(String.format("Client requested equip on layer %d but default is %d", layer, item.getLayer()));
            }
            player.equipItem(item);
            return true;
        } else {
            return false;
        }
    }

    public synchronized void sendContainer(Player player, Item container, boolean open) {
        if(open) {
            OpenGumpPacket gump = new OpenGumpPacket(container, container.getGumpID());
            player.getClient().send(gump);
        }

        List<Item> items = container.getChildren();
        if(items.size() > 0) {
            FullItemsContainerPacket packet = new FullItemsContainerPacket(container.getChildren(), container);
            player.getClient().send(packet);
        }
    }

    public synchronized void onSpeech(Player src, String text, long color) {
        if(text.startsWith("#")) {
            // text command
            ScriptManager.instance().handleTextCommand(src, text.substring(1));
            return;
        }

        boolean isHello = text.toLowerCase().startsWith("hello");
        // remember nearest NPC
        int minDist = Integer.MAX_VALUE;
        NPC helloNPC = null;

        // normal speech -> send to players and NPCs
        SendTextPacket packet = new SendTextPacket(src, SendTextPacket.MODE_SAY, color, text);
        for(SLObject obj : getObjectsInRange(src.getLocation(), SPEECH_RANGE)) {
            if(obj instanceof Player) {
                ((Player) obj).getClient().send(packet);
            } else if(obj instanceof NPC) {
                if(isHello) {
                    if(src.distanceTo(obj) < minDist) {
                        minDist = src.distanceTo(obj);
                        helloNPC = (NPC) obj;
                    }
                } else {
                    ((NPC) obj).onSpeech(src, text.toLowerCase());
                }
            }
        }

        if(isHello && helloNPC != null && minDist < ENTER_AREA_RANGE) {
            helloNPC.onHello(src);
        }
    }

    // who can "see" an item?
    public synchronized Collection<Player> getInterestedPlayers(SLObject obj) {
        List<Player> empty = new LinkedList<Player>();

        if(registry.findObject(obj.getSerial()) == null) {
            return empty;
        }

        // a player is always interested in itself
        if(!obj.isVisible()) {
            if(obj instanceof Player && ((Player) obj).isOnline()) {
                List<Player> res = new LinkedList<Player>();
                res.add((Player) obj);
                return res;
            }
        }

        if(obj instanceof Mobile) {
            return getOnlinePlayersInRange(obj.getLocation(), VISIBLE_RANGE);
        } else if(obj instanceof Item) {
            Item itm = (Item) obj;
            if(itm.isOnGround()) {
                return getOnlinePlayersInRange(obj.getLocation(), VISIBLE_RANGE);
            } else if(itm.isWorn()) {
                return getOnlinePlayersInRange(obj.getParent().getLocation(), VISIBLE_RANGE);
            } else if(itm.isInContainer()) {
                Item container = (Item) itm.getParent();
                if(container.isOnGround()) {
                    return getOnlinePlayersInRange(obj.getParent().getLocation(), VISIBLE_RANGE);
                } else {
                    SLObject root = container.getRoot();
                    if(root instanceof Player) {
                        List<Player> res = new LinkedList<Player>();
                        res.add((Player) root);
                        return res;
                    } else {
                        // inside a mobile's backpack -> no one can see it
                        return empty;
                    }
                }
            } else {
                // no one can see it
                return new ArrayList<Player>();
            }
        } else {
            throw new RuntimeException("don't know who can see " + obj);
        }
    }

    public synchronized void onSingleClick(Player player, SLObject object) {
        long color;
        String name = object.getName();
        if(object instanceof Player) {
            color = SendTextPacket.COLOR_SEE_PLAYER;
        } else if (object instanceof NPC) {
            color = SendTextPacket.COLOR_SEE_NPC;
            name = ((NPC) object).getDecoratedName();
        } else {
            color = SendTextPacket.COLOR_SYSTEM;
        }
        SendTextPacket packet = new SendTextPacket(object, SendTextPacket.MODE_SEE, color, name);
        player.getClient().send(packet);
    }

    public synchronized void sayAbove(SLObject obj, String text) {
        SendTextPacket packet = new SendTextPacket(obj, SendTextPacket.MODE_SAY, SendTextPacket.COLOR_SEE_NPC, text);
        for(Player p : getOnlinePlayersInRange(obj.getLocation(), World.VISIBLE_RANGE)) {
            p.sendPacket(packet);
        }
    }

    public synchronized void sayAbove(Mobile mob, String text, long color) {
        SendTextPacket packet = new SendTextPacket(mob, SendTextPacket.MODE_SAY, color, text);
        for(Player p : getOnlinePlayersInRange(mob.getLocation(), World.VISIBLE_RANGE)) {
            p.sendPacket(packet);
        }
    }

    public synchronized void sendShop(Player player, Mobile shop) {
        Item backpack = shop.getBackpack();
        if(backpack == null) {
            sayAbove(shop, "I'm afraid, but I can't sell you anything");
            return;
        }

        if(backpack.getChildren().size() == 0) {
            sayAbove(shop, "I'm afraid, but I don't have anything left for sale");
            return;
        }

        player.initShopping(shop.getBackpack());
        sendCurrentShopList(player, shop);
        OpenGumpPacket gump = new OpenGumpPacket(shop, OpenGumpPacket.GUMP_ID_SHOP);
        player.sendPacket(gump);
    }

    private synchronized void sendCurrentShopList(Player player, Mobile shop) {
        Item shopContainer = shop.getBackpack();
        if(shopContainer == null || shopContainer.isDeleted() || !player.isShopping()) {
            return;
        }
        List<Item> items = player.getShopItems(shopContainer);
        if(items == null) {
            log.warning(String.format("Player doing shopping without a shop list? %s with %08X", player.getName(), shop.getSerial()));
            return;
        }

        FullItemsContainerPacket packet = new FullItemsContainerPacket(items, shop.getBackpack());
        player.sendPacket(packet);
    }

    public synchronized void onShopAction(Player player, Mobile shop, short action) {
        log.finest(String.format("%s: shop action %d", player.getName(), action));
        Item shopContainer = shop.getBackpack();
        if(shopContainer == null || shopContainer.isDeleted() || !player.isShopping()) {
            return;
        }
        List<Item> items = player.getShopItems(shopContainer);
        if(items == null) {
            log.warning(String.format("Player doing shopping without a shop list? %s with %08X", player.getName(), shop.getSerial()));
            return;
        }
        if(action == 0) {
            // cancelled
            player.finishShopping(shopContainer);
            ShopPacket cancel = new ShopPacket(shop, (short) 0);
            player.sendPacket(cancel);
            sayAbove(shop, "Maybe next time");
        } else if(action == 1) {
            // finish
            player.finishShopping(shopContainer);
            ShopPacket cancel = new ShopPacket(shop, (short) 0);
            player.sendPacket(cancel);

            int sum = 0;
            for(Item item : items) {
                sum += item.getAmount() * item.getPrice();
            }
            if(sum == 0) {
                sayAbove(shop, "Maybe next time");
                return;
            }
            if(sum < 0 || sum >= 60000) {
                sayAbove(shop, "I cannot sell you items worth this amount.");
                return;
            }
            if(player.getBackpack() != null && player.getBackpack().getAmountByType(Items.GFX_GOLD) < sum) {
                sayAbove(shop, "I beg thy pardon but thou cannot pay the " + sum + " gp");
                return;
            }
            player.getBackpack().consumeByType(Items.GFX_GOLD, sum);
            sayAbove(shop, "The total of thy purchase is " + sum + " gp");
            for(Item item : items) {
                if(item.getAmount() == 0) continue;

                if(item.isStackable()) {
                    Item article = item.createCopy(registry.registerItemSerial());
                    article.setAmount(item.getAmount());
                    player.getBackpack().addChild(article, new Point2D(0, 0));
                    registry.registerObject(article);
                } else {
                    for(int i = 0; i < item.getAmount(); i++) {
                        Item article = item.createCopy(registry.registerItemSerial());
                        article.setAmount(1);
                        player.getBackpack().addChild(article, new Point2D(0, 0));
                        registry.registerObject(article);
                    }
                }
            }
            if(sum < 10) {
                playSound(0x0A, shop.getLocation());
            } else if(sum < 100) {
                playSound(0x0B, shop.getLocation());
            } else {
                playSound(0x0C, shop.getLocation());
            }
        } else if(action >= 100) {
            int index;
            boolean increase;
            if(action % 2 == 0) {
                increase = false;
                index = items.size() - 1 - ((action - 100) / 2);
            } else {
                increase = true;
                index = items.size() - 1 - ((action - 101) / 2);
            }
            if(index < 0 || index >= items.size()) {
                log.warning(String.format("Player %s sent invalid shop list index %d", player.getName(), index));
                return;
            }
            Item selected = items.get(index);
            if((selected.getAmount() == 0 && !increase) || (selected.getAmount() == 60000 && increase)) {
                return;
            }
            log.finer((increase ? "inc " : "dec ") + selected.getName());
            selected.setAmount(selected.getAmount() + (increase ? +1 : -1));
            sendCurrentShopList(player, shop);
        } else {
            log.finer(player.getName() + "doing unknown shop action: " + action);
        }
    }

    public synchronized void onCastSpell(Player player, Spell spell, Item scroll, Point3D at, Mobile on) {
        if(scroll != null && !player.tryAccess(scroll)) {
            return;
        } else if(!player.hasSpell(spell)) {
            player.sendSysMessage("You do not have that spell");
            return;
        }

        SpellHandler handler = ScriptManager.instance().getSpellHandler(spell);
        try {
            switch(spell) {
                case CREATEFOOD:    handler.cast(player, scroll); break;
                case DARKSOURCE:    handler.castAt(player, scroll, at); break;
                case FIREBALL:      handler.castOn(player, scroll, on); break;
                case GREATLIGHT:    handler.cast(player, scroll); break;
                case HEALING:       handler.castOn(player, scroll, on); break;
                case LIGHT:         handler.cast(player, scroll); break;
                case LIGHTSOURCE:   handler.castAt(player, scroll, at); break;
            }
        } catch(Exception e) {
            log.log(Level.SEVERE, "Exception in magery: " + e.getMessage(), e);
        }
    }

    public synchronized void doOpenSpellbook(Player player) {
        Item spellbook = player.getSpellbook();
        if(spellbook == null) {
            player.sendSysMessage("You do not have a spellbook");
            return;
        }
        sendContainer(player, player.getBackpack(), false); // prevents client crash
        sendContainer(player, spellbook, true);
    }

    public synchronized void onAttack(final Player player, final Mobile victim) {
        player.setOpponent(victim);
    }

    // returns whether there could be a next round
    private synchronized boolean doFightRound(Mobile attacker, Mobile defender) {
        int deltaZ = Math.abs(attacker.getLocation().getZ() - defender.getLocation().getZ());
        if(deltaZ > SLData.CHARACHTER_HEIGHT / 2) {
            return false;
        }
        if(attacker.distanceTo(defender) > 1 || !defender.isVisible()) {
            return false;
        }
        log.finer("fight ongoing between " + attacker.getName() + " and " + defender.getName());
        attacker.lookAt(defender);

        int damage = 0;
        Integer attackSound = null, painSound = null;
        if(attacker.checkSkill(Attribute.MELEE, 0, 1100)) {
            damage = Util.random(10, 20); // TODO: something smart
            if(defender.checkSkill(Attribute.BATTLE_DEFENSE, 0, 1100)) {
                damage = damage / 3;
            }
        }

        if(damage > 0) {
            attackSound = attacker.getHitSound();
            painSound = defender.getPainSound();
            defender.dealDamage(damage);
        } else {
            attackSound = attacker.getMissSound();
        }

        FightPacket packet = new FightPacket(false, attacker, defender);
        for(Player p : getInterestedPlayers(attacker)) {
            p.sendPacket(packet);
        }
        if(attackSound != null)  {
            playSound(attackSound, attacker.getLocation());
        }
        if(painSound != null) {
            playSound(painSound, defender.getLocation());
        }
        return true;

    }

    public synchronized void playSound(int id, Point2D where) {
        for(Player p : getOnlinePlayersInRange(where, VISIBLE_RANGE)) {
            p.sendSound(id);
        }
    }

    public synchronized void npcPlayerSearch(NPC npc) {
        for(SLObject obj : getObjectsInRange(npc.getLocation(), ENTER_AREA_RANGE)) {
            if(obj instanceof Player && obj.isVisible()) {
                npc.onEnterArea((Player) obj);
            }
        }
    }

    private synchronized void runRefresh(final Mobile mob) {
        if(mob.isRefreshRunning() || !mob.needsRefresh()) {
            return;
        }
        Runnable refreshAction = new Runnable() {
            public void run() {
                if(!mob.needsRefresh() || !mob.canRefresh()) {
                    mob.setRefreshRunning(false);
                    return;
                }

                mob.doRefreshStep();

                // check if it needs another refresh after this step
                if(mob.needsRefresh()) {
                    TimerQueue.get().addTimer(new Timer(STAT_REFRESH_DELAY, this));
                } else {
                    mob.setRefreshRunning(false);
                }
            }
        };
        mob.setRefreshRunning(true);
        TimerQueue.get().addTimer(new Timer(STAT_REFRESH_DELAY, refreshAction));
    }

    @Override
    public void onTimeChange(boolean phaseChanged) {
        log.fine("Ingame hour: " + dayNightCycle.getHour() + ", light level: " + dayNightCycle.getLightLevel());
        GlobalLightLevelPacket packet = new GlobalLightLevelPacket(dayNightCycle.getLightLevel());
        for(Player player : getOnlinePlayers()) {
            player.sendPacket(packet);
        }
    }

    @Override
    public synchronized void onObjectUpdate(SLObject obj) {
        // something basic like graphic or amount changed, but not location
        for(Player player : getInterestedPlayers(obj)) {
            log.finer(String.format("sending change of %08X to %s", obj.getSerial(), player.getName()));
            if(obj.isVisible() || obj == player) {
                sendObject(player, obj);
                if(obj instanceof Mobile) {
                    sendFullEquipment(player, (Mobile) obj);
                } else if(obj instanceof Item) {
                    Item itm = (Item) obj;
                    if(itm.getLightLevel() != 0) {
                        LightLevelPacket packet = new LightLevelPacket(itm, itm.getLightLevel());
                        player.sendPacket(packet);
                    }
                }
            } else {
                // probably invisible, but maybe just being dragged
                if(obj instanceof Item && ((Item) obj).getDraggingPlayer() != player) {
                    sendDelete(player, obj);
                } else if(obj instanceof Player) {
                    // player death, hiding staff etc.
                    sendDelete(player, obj);
                }
            }
        }
    }

    @Override
    public synchronized void onLocationChanged(SLObject obj, Point3D oldLoc) {
        Point3D newLoc = obj.getLocation();

        // special case: an item that's not on ground doesn't have a real location
        if(obj instanceof Item && !((Item) obj).isOnGround()) {
            for(Player player : getInterestedPlayers(obj)) {
                if(obj.isVisible()) {
                    sendObject(player, obj);
                } else {
                    sendDelete(player, obj);
                }
            }
            return;
        }

        // send remove to players that no longer see it
        for(Player player : getOnlinePlayersInRange(oldLoc, VISIBLE_RANGE)) {
            if(player.distanceTo(newLoc) > VISIBLE_RANGE && obj != player) {
                log.finer(String.format("sending move of %08X to %s -> delete", obj.getSerial(), player.getName()));
                sendDelete(player, obj);
            }
        }

        // send update to players that (still) see it now
        for(Player player : getInterestedPlayers(obj)) {
            if(obj != player) {
                log.finer(String.format("sending move of %08X to %s -> update", obj.getSerial(), player.getName()));
                sendObject(player, obj);
                if(obj instanceof Mobile && player.distanceTo(oldLoc) > VISIBLE_RANGE) {
                    // wasn't visible before -> also send equip
                    sendFullEquipment(player, (Mobile) obj);
                }
            }
        }

        // if a player was moved, delete all their old items and send new ones
        if(obj instanceof Player) {
            Player movedPlayer = (Player) obj;
            boolean wasForced = !movedPlayer.isWalking();
            if(movedPlayer.isOnline()) {
                if(wasForced) {
                    movedPlayer.sendLocation();
                }

                // delete no longer visible objects
                for(SLObject oldObj : getObjectsInRange(oldLoc, VISIBLE_RANGE)) {
                    if(oldObj.distanceTo(movedPlayer) > VISIBLE_RANGE && oldObj != movedPlayer) {
                        log.finer(String.format("%s moved, %08X left scene", movedPlayer.getName(), oldObj.getSerial()));
                        sendDelete(movedPlayer, oldObj);
                    }
                }

                // send now visible objects
                for(SLObject newObj : getObjectsInRange(newLoc, VISIBLE_RANGE)) {
                    if(newObj == movedPlayer) {
                        // player always sees itself anyways
                        continue;
                    }

                    // inform NPCs that are now in range
                    if(newObj instanceof NPC && newObj.distanceTo(oldLoc) > SPEECH_RANGE && newObj.distanceTo(newLoc) <= SPEECH_RANGE) {
                        ((NPC) newObj).onEnterArea(movedPlayer);
                    }

                    if(!wasForced && newObj.distanceTo(oldLoc) <= VISIBLE_RANGE) {
                        // not really new because it was also visible before
                        continue;
                    }

                    if(newObj.isVisible()) {
                        log.finer(String.format("%s moved, %08X entered scene", movedPlayer.getName(), newObj.getSerial()));
                        sendObject(movedPlayer, newObj);
                        if(newObj instanceof Mobile) {
                            sendFullEquipment(movedPlayer, (Mobile) newObj);
                        }
                    }
                }
            }
        }
    }

    @Override
    public synchronized void onObjectDelete(SLObject obj) {
        for(Player player : getInterestedPlayers(obj)) {
            sendDelete(player, obj);
        }

        if(obj instanceof Player) {
            Player p = (Player) obj;
            if(p.isOnline()) {
                p.sendSysMessage("You have been deleted.");
                p.getClient().disconnect();
            }
        } else if(obj instanceof Item) {
            SLObject parent = obj.getParent();
            if(parent instanceof Item) {
                ((Item) parent).removeChild((Item) obj);
            } else if(parent instanceof Mobile) {
                ((Mobile) parent).unequipItem((Item) obj);
                for(Player p : getInterestedPlayers(parent)) {
                    p.sendLocation(); // fix redraw
                }
            }
        }

        obj.removeObserver(this);
        registry.removeObject(obj.getSerial());
    }

    @Override
    public synchronized void onDragItem(Item itm, Player who) {
        for(Player player : getInterestedPlayers(who)) {
            if(player != who) {
                sendDelete(player, itm);
                if(itm.isWorn()) {
                    player.sendLocation(); // fix redraw error
                }
            }
        }
    }

    @Override
    public synchronized void onAttributeChanged(Mobile mob, Attribute a) {
        if(a == Attribute.HITS || a == Attribute.MAX_HITS) {
            for(Player player : getInterestedPlayers(mob)) {
                sendStats(player, mob);
            }
        } else if(a.isSkill() && mob instanceof Player) {
            sendSkills((Player) mob, false);
        } else if(a.isStat() && mob instanceof Player) {
            sendStats((Player) mob, mob);
        }
        runRefresh(mob);
    }

    @Override
    public synchronized void onItemEquipped(Item item, Mobile mob) {
        for(Player player : getOnlinePlayersInRange(mob.getLocation(), VISIBLE_RANGE)) {
            sendObject(player, item);
            player.sendLocation(); // fix redraw error
        }
    }

    @Override
    public synchronized void onChildAdded(Item container, Item child) {
        for(Player player : getInterestedPlayers(child)) {
            log.finer(String.format("item %08X in container %08X -> to %s", child.getSerial(), container.getSerial(), player.getName()));
            sendObject(player, child);
        }
    }

    @Override
    public synchronized void onChildRemoved(Item container, Item child) {
        Player dragger = child.getDraggingPlayer();
        for(Player player : getInterestedPlayers(container)) {
            if(player != dragger) {
                log.finer(String.format("child %08X removed for %08X", child.getSerial(), player.getSerial()));
                sendDelete(player, child);
            }
        }
    }

    @Override
    public synchronized void onDeath(Mobile mob) {
        Integer sound = mob.getDeathSound();
        if(sound != null) {
            playSound(sound, mob.getLocation());
        }

        Item corpse = Item.createAtLocation(mob.getLocation(), mob.getCorpseGraphic(), 1);
        if(mob instanceof Player) {
            log.fine(mob.getName() + " died");
            Player player = (Player) mob;
            for(Item equip : player.getEquippedItems()) {
                if(equip.getLayer() == SLTiles.StaticTile.LAYER_HAIR) {
                    continue;
                }
                player.unequipItem(equip);
                corpse.addChild(equip, new Point2D(0, 0));
            }
            DeathPacket packet = new DeathPacket();
            player.sendPacket(packet);

            player.setLocation(new Point3D(RESURRECT_POSITION_X, RESURRECT_POSITION_Y, 0));
            playSound(0xA4, mob.getLocation()); // resurrect sound
        } else {
            mob.delete();
        }
    }

    @Override
    public synchronized void onOpponentChanged(final Mobile attacker, final Mobile defender, final Mobile oldDefender) {
        if(defender == null) {
            // cleared
            return;
        } else if(defender == attacker) {
            attacker.setOpponent(null);
            return;
        } else if(defender == oldDefender) {
            // already doing this fight
            return;
        }

        Runnable fight = new Runnable() {
            public void run() {
                if(attacker.getOpponent() == defender && attacker.canFight() && defender.canFight()) {
                    boolean canGoOn = doFightRound(attacker, defender);
                    if(!canGoOn || !defender.canFight() || !attacker.canFight()) {
                        attacker.setOpponent(null);
                    } else {
                        TimerQueue.get().addTimer(new Timer(1500, this));
                    }
                }
            }
        };

        // start first round delayed to avoid exploiting by opponent switching
        TimerQueue.get().addTimer(new Timer(200, fight));
    }

    @Override
    public void onObjectRegistered(SLObject object) {
        object.addObserver(this);
        onObjectUpdate(object);
    }
}
