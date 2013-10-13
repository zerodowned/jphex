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
package org.solhost.folko.jphex.scripting;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.runtime.builtin.IRubyObject;
import org.solhost.folko.jphex.Timer;
import org.solhost.folko.jphex.TimerQueue;
import org.solhost.folko.jphex.Util;
import org.solhost.folko.jphex.World;
import org.solhost.folko.jphex.types.*;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.types.Attribute;
import org.solhost.folko.uosl.types.Direction;
import org.solhost.folko.uosl.types.Items;
import org.solhost.folko.uosl.types.Mobiles;
import org.solhost.folko.uosl.types.Point2D;
import org.solhost.folko.uosl.types.Point3D;
import org.solhost.folko.uosl.types.Spell;
import org.solhost.folko.uosl.util.Pathfinder;

public class ScriptAPIImpl implements ScriptAPI {
    private static final Logger log = Logger.getLogger("jphex.scriptapi");
    private final World world;

    public ScriptAPIImpl(World world) {
        this.world = world;
    }

    @Override
    public void sendSysMessage(Player player, String message) {
        player.sendSysMessage(message);
    }

    @Override
    public void moveObject(SLObject obj, int x, int y) {
        byte z = SLData.get().getMap().getElevation(new Point2D(x, y));
        moveObject(obj, x, y, z);
    }

    @Override
    public void moveObject(SLObject obj, int x, int y, int z) {
        obj.setLocation(new Point3D(x, y, z));
    }

    @Override
    public void sendHexPacket(Player player, short packetID, String data) {
        player.sendHexPacket(packetID, data);
    }

    @Override
    public boolean reloadScripts() {
        return ScriptManager.instance().reload();
    }

    @Override
    public void saveWorld() {
        world.save();
    }

    @Override
    public void playSoundNearObj(SLObject obj, int soundID) {
        Point2D location = null;
        if(obj instanceof Item) {
            Item itm = (Item) obj;
            if(itm.isOnGround()) {
                location = obj.getLocation();
            } else if(itm.isWorn()) {
                location = obj.getParent().getLocation();
            } else if(itm.isInContainer()) {
                playSoundNearObj(itm, soundID);
                return;
            }
        } else {
            location = obj.getLocation();
        }

        for(Player player : world.getOnlinePlayersInRange(location, World.VISIBLE_RANGE)) {
            player.sendSound(soundID);
        }
    }

    @Override
    public void sendSound(Player player, int soundID) {
        player.sendSound(soundID);
    }

    @Override
    public Item createItemInBackpack(Mobile mob, int graphic) {
        Item backpack = mob.getBackpack();
        if(backpack != null) {
            Item item = new Item(world.registerItemSerial(), graphic);
            world.registerObject(item);
            backpack.addChild(item, new Point2D(50, 50));
            return item;
        } else {
            return createItemAtMobile(mob, graphic);
        }
    }

    @Override
    public Item createItemAtMobile(Mobile mob, int graphic) {
        Item item = new Item(world.registerItemSerial(), graphic);
        item.setLocation(mob.getLocation());
        world.registerObject(item);
        return item;
    }

    @Override
    public Item createItemAtLocation(int x, int y, int z, int graphic) {
        Item item = new Item(world.registerItemSerial(), graphic);
        item.setLocation(new Point3D(x, y, z));
        world.registerObject(item);
        return item;
    }

	@Override
	public Item createItemInBackpack(Mobile mob, String behavior) {
        Item backpack = mob.getBackpack();
        if(backpack != null) {
            try {
                Item item = new Item(world.registerItemSerial(), behavior);
                world.registerObject(item);
                backpack.addChild(item, new Point2D(50, 50));
                return item;
            } catch (Exception e) {
                return null;
            }
        } else {
            return createItemAtMobile(mob, behavior);
        }
	}

	@Override
	public Item createItemAtMobile(Mobile mob, String behavior) {
	    try {
            Item item = new Item(world.registerItemSerial(), behavior);
            item.setLocation(mob.getLocation());
            world.registerObject(item);
            return item;
	    } catch(Exception e) {
	        return null;
	    }
	}

	@Override
	public Item createItemAtLocation(int x, int y, int z, String behavior) {
	    try {
            Item item = new Item(world.registerItemSerial(), behavior);
            item.setLocation(new Point3D(x, y, z));
            world.registerObject(item);
            return item;
	    } catch(Exception e) {
	        return null;
	    }
	}

	@Override
    public void setGraphic(SLObject obj, int graphic) {
        obj.setGraphic(graphic);
    }

    @Override
    public void setHue(SLObject obj, int hue) {
        obj.setHue(hue);
    }

    @Override
    public void targetObject(Player player, final RubyProc block) {
        player.targetObject(new TargetObjectHandler() {
            public void onTarget(SLObject obj) {
                IRubyObject[] args = {ScriptManager.instance().toRubyObject(obj)};
                block.call(ScriptManager.instance().getContext(), args);
            }
        });
    }

    @Override
    public void targetLocation(Player player, final RubyProc block) {
        player.targetLocation(new TargetLocationHandler() {
            public void onTarget(Point3D point) {
                IRubyObject[] args = {
                            ScriptManager.instance().toRubyObject(point.getX()),
                            ScriptManager.instance().toRubyObject(point.getY()),
                            ScriptManager.instance().toRubyObject(point.getZ())
                        };
                block.call(ScriptManager.instance().getContext(), args);
            }
        });
    }

    @Override
    public void deleteObject(SLObject obj) {
        obj.delete();
    }

    @Override
    public boolean setItemBehavior(Item item, String behavior) {
        if(ScriptManager.instance().getItemBehavior(behavior) != null) {
            item.setBehavior(behavior);
            return true;
        } else if(behavior.equals("null")) {
            item.setBehavior(null);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getItemBehavior(Item item) {
        return item.getBehavior();
    }

    @Override
    public void setObjectProperty(SLObject object, String name, IRubyObject value) {
        object.setProperty(name, (RubyObject) value);
    }

    @Override
    public IRubyObject getObjectProperty(SLObject object, String name) {
        Serializable value = object.getProperty(name);
        if(value != null) {
            return (RubyObject) value;
        } else {
            return ScriptManager.instance().toRubyObject(null);
        }
    }

    @Override
    public void addTimer(long millis, final RubyProc block) {
        TimerQueue.get().addTimer(new Timer(millis, new Runnable() {
            public void run() {
                IRubyObject args[] = {};
                block.call(ScriptManager.instance().getContext(), args);
            }
        }));
    }

    @Override
    public boolean spawnMobileAtPlayer(Player near, String behavior) {
        MobileBehavior be = ScriptManager.instance().getMobileBehaviour(behavior);
        if(be == null) {
            return false;
        }
        Mobile mob = new Mobile(world.registerMobileSerial());
        mob.setLocation(near.getLocation());
        mob.setFacing(near.getFacing());
        mob.setBehavior(behavior);
        be.onSpawn(mob);
        world.registerObject(mob);
        world.npcPlayerSearch(mob);
        return true;
    }

    @Override
    public void setName(SLObject obj, String name) {
        obj.setName(name);
    }

    @Override
    public void assignRandomName(Mobile mob, String suffix) {
        String name;
        switch(mob.getGraphic()) {
        case 0x0000: { // human male
            String[] randomNames = {"Adam", "Bob", "Charles", "David", "Eric", "Frank", "George", "Thomas", "James"};
            name = Util.randomElement(randomNames);
            break;
        }
        case 0x0001: { // human female
            String[] randomNames = {"Arlene", "Charlotte"};
            name = Util.randomElement(randomNames);
            break;
        }
        default:
            name = "unnamed";
            break;
        }
        if(suffix.length() > 0) {
            mob.setName(name + " " + suffix);
        } else {
            mob.setName(name);
        }
    }

    @Override
    public int randomHairStyle(int graphic) {
        if(graphic == 0) {
            return Items.GFX_HAIR_START + Util.random(0, 2);
        } else {
            return Items.GFX_HAIR_START + 3 + Util.random(0, 3);
        }
    }

    @Override
    public int randomHairHue() {
        return Util.random(7, 15);
    }

    @Override
    public int randomClothColor() {
        return Util.random(23, 40);
    }

    @Override
    public void createClothes(Mobile mob) {
        Item.createEquipped(world, mob, Items.GFX_INVIS_PACK, 0);
        Item.createEquipped(world, mob, Items.GFX_SHOP_CONTAINER, 0);
        Item.createEquipped(world, mob, Items.GFX_TUNIC, randomClothColor());
        Item.createEquipped(world, mob, randomHairStyle(mob.getGraphic()), randomHairHue());
        if(mob.getGraphic() == Mobiles.MOBTYPE_HUMAN_MALE) {
            Item.createEquipped(world, mob, Items.GFX_PANTS, randomClothColor());
        } else {
            Item.createEquipped(world, mob, Items.GFX_SKIRT, randomClothColor());
        }
    }

    @Override
    public void say(Mobile mob, String text) {
        world.sayAbove(mob, text);
    }

    @Override
    public void offerShop(Mobile mob, Player player) {
        world.sendShop(player, mob);
    }

    @Override
    public void speakPowerWords(Player player, Spell spell) {
        world.sayAbove(player, Character.toString((char) 0x0F) + " " + spell.getPowerWords() + " " + Character.toString((char) 0x0F), 0x00887766);
    }

    @Override
    public void setAttribute(Mobile mob, Attribute attr, long value) {
        mob.setAttribute(attr, value);
    }

    @Override
    public void refreshStats(Mobile mob) {
        mob.refreshStats();
    }

    @Override
    public int getDistance(SLObject o1, SLObject o2) {
        return o1.distanceTo(o2);
    }

    @Override
    public void runToward(Mobile who, Mobile to) {
        int distance = who.distanceTo(to);
        if(distance > 0 && distance < World.VISIBLE_RANGE) {
            Pathfinder finder = new Pathfinder(who.getLocation(), to.getLocation(), world);
            if(finder.findPath(500)) {
                Direction dir = finder.getPath().get(0);
                who.setFacing(dir);
                Point3D newLoc = world.canWalk(who, dir);
                if(newLoc == null) {
                    log.severe("Pathfinder returned illegal path");
                    return;
                }
                who.setLocation(newLoc);
            }
        }
    }

    @Override
    public void attack(Mobile attacker, Mobile defender) {
        attacker.setOpponent(defender);
    }

    @Override
    public void kill(Mobile what) {
        what.kill();
    }

    @Override
    public void lookAt(Mobile who, SLObject what) {
        who.setFacing(who.getLocation().getDirectionTo(what.getLocation()));
    }

    @Override
    public Collection<Player> getNearbyPlayers(Mobile who) {
        Collection<Player> online = world.getOnlinePlayersInRange(who.getLocation(), World.VISIBLE_RANGE);
        Iterator<Player> iter = online.iterator();
        while(iter.hasNext()) {
            Player p = iter.next();
            if(!p.isVisible()) {
                iter.remove();
            }
        }
        return online;
    }

	@Override
	public long getTimerTicks() {
		return Timer.getCurrentTicks();
	}
}
