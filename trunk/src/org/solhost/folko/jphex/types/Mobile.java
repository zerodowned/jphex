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
package org.solhost.folko.jphex.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.solhost.folko.jphex.Util;
import org.solhost.folko.jphex.scripting.MobileBehavior;
import org.solhost.folko.jphex.scripting.ScriptManager;
import org.solhost.folko.uosl.data.SLTiles;
import org.solhost.folko.uosl.network.SendableMobile;
import org.solhost.folko.uosl.types.Attribute;
import org.solhost.folko.uosl.types.Direction;

public class Mobile extends SLObject implements SendableMobile {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger("jphex.mobile");
    private boolean refreshRunning;
    protected Direction facing;
    protected Set<Item> equipped;
    protected Map<Attribute, Long> attributes;
    protected short hairHue, hairStyle;
    protected Mobile opponent;
    private String behavior;

    public Mobile(long serial) {
        super(serial);
        this.name = new String("");
        this.attributes = new HashMap<Attribute, Long>();
        this.equipped = new CopyOnWriteArraySet<Item>();
    }

    @Override
    public void delete() {
        for(Item equip : equipped) {
            equip.delete();
        }
        super.delete();
    }

    public void setRefreshRunning(boolean running) {
        this.refreshRunning = running;
    }

    public boolean isRefreshRunning() {
        return refreshRunning;
    }

    public void setOpponent(Mobile what) {
        this.opponent = what;
        for(ObjectObserver o : observers) o.onOpponentChanged(this, what);
    }

    public Mobile getOpponent() {
        return opponent;
    }

    public void setBehavior(String behavior) {
        if(ScriptManager.instance().getMobileBehaviour(behavior) != null) {
            this.behavior = behavior;
        }
    }

    public String getBehavior() {
        return behavior;
    }

    public void setAttribute(Attribute a, long value) {
        if(a == Attribute.MAX_HITS || a == Attribute.MAX_FATIGUE || a == Attribute.MAX_MANA || a == Attribute.NEXT_LEVEL) {
            throw new IllegalArgumentException("not writable");
        }
        attributes.put(a,  value);
        for(ObjectObserver o : observers) o.onAttributeChanged(this, a);
    }

    public long getAttribute(Attribute a) {
        switch(a) {
        case MAX_HITS:
            return 50 + getAttribute(Attribute.STRENGTH) / 2;
        case MAX_FATIGUE:
            return getAttribute(Attribute.DEXTERITY);
        case MAX_MANA:
            return getAttribute(Attribute.INTELLIGENCE);
        case NEXT_LEVEL:
            return getAttribute(Attribute.LEVEL) * 10;
        default:
            if(attributes.containsKey(a)) {
                return attributes.get(a);
            } else {
                return 0;
            }
        }
    }

    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        if(this.facing == facing) {
            return;
        }
        this.facing = facing;
        for(ObjectObserver o : observers) o.onLocationChanged(this, location);
    }

    public void lookAt(SLObject other) {
        setFacing(getLocation().getDirectionTo(other.getLocation()));
    }

    public short getHairHue() {
        return hairHue;
    }

    public void setHairHue(short hairColor) {
        this.hairHue = hairColor;
        for(ObjectObserver o : observers) o.onObjectUpdate(this);
    }

    public short getHairStyle() {
        return hairStyle;
    }

    public void setHairStyle(short hairStyle) {
        this.hairStyle = hairStyle;
        for(ObjectObserver o : observers) o.onObjectUpdate(this);
    }

    public void refreshStats() {
        setAttribute(Attribute.HITS, getAttribute(Attribute.MAX_HITS));
        setAttribute(Attribute.MANA, getAttribute(Attribute.MAX_MANA));
        setAttribute(Attribute.FATIGUE, getAttribute(Attribute.MAX_FATIGUE));
    }

    public Item getBackpack() {
        return getEquipmentByLayer(SLTiles.StaticTile.LAYER_BACKPACK);
    }

    public void equipItem(Item item) {
        item.setParent(this);
        equipped.add(item);
        for(ObjectObserver o : observers) o.onItemEquipped(item, this);
    }

    public Item getEquipmentByLayer(short layer) {
        for(Item equip : equipped) {
            if(equip.getLayer() == layer) {
                return equip;
            }
        }
        return null;
    }

    public void unequipItem(Item item) {
        item.setParent(null);
        equipped.remove(item);
    }

    public Set<Item> getEquippedItems() {
        return equipped;
    }

    public void onEnterArea(Player player) {
        MobileBehavior be = ScriptManager.instance().getMobileBehaviour(behavior);
        if(be != null) {
            try {
                be.onEnterArea(this, player);
            } catch(Exception e) {
                log.log(Level.SEVERE, "Script error in onEnterArea: " + e.getMessage(), e);
            }
        }
    }

    public void onSpeech(Player player, String line) {
        MobileBehavior be = ScriptManager.instance().getMobileBehaviour(behavior);
        try {
            be.onSpeech(this, player, line);
        } catch(Exception e) {
            log.log(Level.SEVERE, "Script error in onEnterArea: " + e.getMessage(), e);
        }
    }

    public void onHello(Player player) {
        MobileBehavior be = ScriptManager.instance().getMobileBehaviour(behavior);
        try {
            be.onHello(this, player);
        } catch(Exception e) {
            log.log(Level.SEVERE, "Script error in onHello: " + e.getMessage(), e);
        }
    }

    public boolean onDoubleClick(Player player) {
        MobileBehavior be = ScriptManager.instance().getMobileBehaviour(behavior);
        try {
            return be.onDoubleClick(this, player);
        } catch(Exception e) {
            log.log(Level.SEVERE, "Script error in onEnterArea: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean checkSkill(Attribute skill, long minRequired, long maxUntilNoGain) {
        long value = getAttribute(skill);
        if(value < minRequired) {
            log.fine(String.format("%s: Checking %s for %d -> failure due to no chance", getName(), skill, minRequired));
            return false;
        } else if(value >= maxUntilNoGain) {
            log.fine(String.format("%s: Checking %s for %d -> success due to high chance", getName(), skill, minRequired));
            return true;
        }

        // Interesting range where gains happen
        double successChance = (double) (value - minRequired) / (double) (maxUntilNoGain - minRequired);
        boolean success = Util.tryChance(successChance);

        double gainChance;
        if(value < 50 && minRequired == 0) {
            // allow quick initial gain up to 5%
            gainChance = 0.5;
        } else {
            gainChance = Math.max(1 - successChance, 0.025);
        }
        boolean gain = Util.tryChance(gainChance);

        if(gain) {
            log.fine(String.format("%s: Checking %s for %d -> success chance %.2f, gain chance %.2f -> gain", getName(), skill, minRequired, successChance, gainChance));
            setAttribute(skill, value + 1);
        } else {
            log.fine(String.format("%s: Checking %s for %d -> success chance %.2f, gain chance %.2f -> no gain", getName(), skill, minRequired, successChance, gainChance));
        }

        return success;
    }

    // when hitting
    public Integer getHitSound() {
        Item weapon = getEquipmentByLayer(SLTiles.StaticTile.LAYER_WEAPON);
        if(weapon != null) {
            switch(weapon.getGraphic()) {
            // Axes
            case 0x292:
            case 0x293:
            case 0x294:
            case 0x295:
            case 0x296:
            case 0x297:
            case 0x298:
            case 0x299:
            case 0x29A:
            case 0x29B:
                return Util.randomElement(new Integer[] {0xB2, 0xB3});
            // Small blades
            case 0x2A0:
            case 0x2A1:
                return 0x86;
            // Mace
            case 0x2A2:
            case 0x2A3:
                return Util.randomElement(new Integer[] {0xB2, 0xB3});
            // Swords
            case 0x2A4:
            case 0x2A5:
            case 0x2A6:
            case 0x2A7:
                return Util.randomElement(new Integer[] {0xB7, 0xB8});
            }
        } else {
            switch(getGraphic()) {
            // Humans
            case 0x00:
            case 0x01:
                return Util.randomElement(new Integer[] {0x7C, 0x7D, 0x81, 0x85});
            // Skeleton
            case 0x2A:
                return Util.randomElement(new Integer[] {0x86, 0x88});
            }
        }
        return null;
    }

    public Integer getMissSound() {
        Item weapon = getEquipmentByLayer(SLTiles.StaticTile.LAYER_WEAPON);
        if(weapon != null) {
            switch(weapon.getGraphic()) {
            // Axes
            case 0x292:
            case 0x293:
            case 0x294:
            case 0x295:
            case 0x296:
            case 0x297:
            case 0x298:
            case 0x299:
            case 0x29A:
            case 0x29B:
                return 0xAF;
            // Small blades
            case 0x2A0:
            case 0x2A1:
                return 0xB6;
            // Mace
            case 0x2A2:
            case 0x2A3:
                return 0xB0;
            // Swords
            case 0x2A4:
            case 0x2A5:
            case 0x2A6:
            case 0x2A7:
                return Util.randomElement(new Integer[] {0xB4, 0xB5});
            }
        } else {
            switch(getGraphic()) {
            // Humans
            case 0x00:
            case 0x01:
                return 0xB0;
            // Skeleton
            case 0x2A:
                return 0xB0;
            }
        }
        return null;
    }

    public Integer getPainSound() {
        switch(getGraphic()) {
        case 0x00: // human male
            return Util.randomElement(new Integer[] {0x95, 0x96, 0x97, 0x98, 0x99, 0x9A});
        case 0x01: // human female
            return Util.randomElement(new Integer[] {0x8B, 0x8C, 0x8D, 0x8E, 0x8F, 0x90});
        }
        return null;
    }

    public Integer getDeathSound() {
        switch(getGraphic()) {
        case 0x00: // human male
            return Util.randomElement(new Integer[] {0x9B, 0x9C, 0x9D, 0x9E});
        case 0x01: // human female
            return Util.randomElement(new Integer[] {0x91, 0x92, 0x93, 0x94});
        case 0x2A: // skeleton
            return 0x7E;
        }
        return null;
    }

    public void kill() {
        setAttribute(Attribute.HITS, 0);
        setAttribute(Attribute.MANA, 0);
        setAttribute(Attribute.FATIGUE, 0);
        setOpponent(null);
        for(ObjectObserver o : observers) o.onDeath(this);
    }

    public void dealDamage(int damage) {
        long oldHits = getAttribute(Attribute.HITS);
        if(damage >= oldHits) {
            kill();
        } else {
            setAttribute(Attribute.HITS, oldHits - damage);
        }
    }

    public int getCorpseGraphic() {
        switch(getGraphic()) {
        case 0x00: // Human Male
        case 0x01: // Human Female
            return 0x3D67;
        case 0x2A: // Skeleton
            return 0x3D66;
        case 0x34: // Deer
            return 0x3D69;
        case 0x28: // orc
            return 0x3D65;
        case 0x29: // orc captain
            return 0x3D64;
        case 0x35: // rabbit
            return 0x3D6B;
        case 0x32: // wolf
            return 0x3D6A;
        default:
            return 0x3D68;
        }
    }

    public boolean canFight() {
        return !deleted;
    }

    public boolean canRefresh() {
        return !deleted;
    }

    public boolean needsRefresh() {
        return  getAttribute(Attribute.HITS)    < getAttribute(Attribute.MAX_HITS) ||
                getAttribute(Attribute.MANA)    < getAttribute(Attribute.MAX_MANA) ||
                getAttribute(Attribute.FATIGUE) < getAttribute(Attribute.MAX_FATIGUE);
    }

    public void doRefreshStep() {
        long hits = getAttribute(Attribute.HITS);
        long mana = getAttribute(Attribute.MANA);
        long fatigue = getAttribute(Attribute.FATIGUE);

        if(hits < getAttribute(Attribute.MAX_HITS)) {
            setAttribute(Attribute.HITS, hits + 1);
        }
        if(mana < getAttribute(Attribute.MAX_MANA)) {
            setAttribute(Attribute.MANA, mana + 1);
        }
        if(fatigue < getAttribute(Attribute.MAX_FATIGUE)) {
            setAttribute(Attribute.FATIGUE, fatigue + 1);
        }

    }
}
