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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.solhost.folko.jphex.ObjectRegistry;
import org.solhost.folko.jphex.scripting.ItemBehavior;
import org.solhost.folko.jphex.scripting.ScriptManager;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLTiles.StaticTile;
import org.solhost.folko.uosl.network.SendableItem;
import org.solhost.folko.uosl.types.Gumps;
import org.solhost.folko.uosl.types.Items;
import org.solhost.folko.uosl.types.Point2D;
import org.solhost.folko.uosl.types.Point3D;
import org.solhost.folko.uosl.types.Spell;

public class Item extends SLObject implements SendableItem {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger("jphex.types");
    private boolean isContainer, isWearable, isStackable;
    private short weight, defaultLayer;
    private byte lightLevel;
    private transient List<Item> children;
    private Player draggedBy;
    private int amount, price, height;
    private String behavior;

    public Item(long serial, int graphic) {
        super(serial);
        this.graphic = graphic;
        setBasicAttributes();
    }

    public Item(long serial, int graphic, String behavior) {
        super(serial);
        this.graphic = graphic; // might be overridden by onCreate
        this.behavior = behavior;
        ItemBehavior ib = ScriptManager.instance().getItemBehavior(behavior);
        if(ib == null) {
            throw new UnsupportedOperationException("invalid behavior");
        } else {
            try {
                ib.onCreate(this);
            } catch(Exception e) {
                log.log(Level.SEVERE, "Script error in onCreate: " + e.getMessage(), e);
                delete();
            }
        }
        setBasicAttributes();
    }

    @Override
    public void setGraphic(int graphic) {
        super.setGraphic(graphic);
        setBasicAttributes();
    }

    protected void setBasicAttributes() {
        this.children = new CopyOnWriteArrayList<Item>();
        this.amount = 1;
        StaticTile tile = SLData.get().getTiles().getStaticTile(graphic);
        if(tile != null) {
            this.name = tile.name;
            this.weight = tile.weight;
            this.price = tile.price;
            this.height = tile.height;
            this.defaultLayer = tile.layer;
            this.isContainer = tile.isContainer();
            this.isWearable = tile.isWearable();
            this.isStackable = tile.isStackable();
        }
    }

    @Override
    public void delete() {
        if(children != null) {
            for(Item child : children) {
                child.delete();
            }
            children.clear();
        }
        super.delete();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        ItemBehavior ib = ScriptManager.instance().getItemBehavior(behavior);
        if(ib != null) {
            try {
                ib.onLoad(this);
            } catch(Exception e) {
                log.log(Level.SEVERE, "Script error in onLoad: " + e.getMessage(), e);
            }
        }
    }

    public int getHeight() {
        return height;
    }

    public void setBehavior(String behavior) {
        this.behavior = behavior;
        ItemBehavior ib = ScriptManager.instance().getItemBehavior(behavior);
        if(ib != null) {
            try {
                ib.onBehaviorChange(this);
            } catch(Exception e) {
                log.log(Level.SEVERE, "Script error in onBehaviorSet: " + e.getMessage(), e);
            }
        }
    }

    public String getBehavior() {
        return behavior;
    }

    public Item createCopy(long serial) {
        Item res = new Item(serial, graphic);
        res.weight = weight;
        res.amount = amount;
        res.hue = hue;
        res.location = new Point3D(location.getX(), location.getY(), location.getZ());
        res.name = new String(name);
        if(behavior != null) {
            res.behavior = new String(behavior);
            for(String prop : scriptProperties.keySet()) {
                res.setProperty(prop, getProperty(prop));
            }
        }
        return res;
    }

    public Item getItemAtLocation(Point2D location) {
        for(Item item : children) {
            if(item.getLocation().getX() == location.getX() && item.getLocation().getY() == location.getY()) {
                return item;
            }
        }
        return null;
    }

    public void addChild(Item child, Point2D location) {
        child.setParent(this);
        if(location.getX() == 0 && location.getY() == 0) {
            location = new Point2D(50, 50);
        }
        if(this.graphic == Items.GFX_SPELLBOOK) {
            child.setAmount(Spell.fromScrollGraphic(child.getGraphic()).toByte());
        }

        child.location = new Point3D(location, 0);
        children.add(child);
        for(ObjectObserver o : observers) o.onChildAdded(this, child);
    }

    // adding or removing has no effect
    public List<Item> getChildren() {
        List<Item> res = new ArrayList<Item>();
        for(Item child : children) {
            res.add(child);
        }
        return res;
    }

    public void removeChild(Item child) {
        children.remove(child);
        for(ObjectObserver o : observers) o.onChildRemoved(this, child);
    }

    public boolean isOnGround() {
        return getParent() == null && location != null;
    }

    public boolean isWorn() {
        return getParent() instanceof Mobile;
    }

    public boolean isInContainer() {
        return getParent() instanceof Item;
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && (draggedBy == null);
    }

    public int getAmount() {
        return amount;
    }

    public int getPrice() {
        return price;
    }

    public byte getLightLevel() {
        return lightLevel;
    }

    public void setLightLevel(byte level) {
        this.lightLevel = level;
        for(ObjectObserver o : observers) o.onObjectUpdate(this);
    }

    public void consume(int count) {
        if(amount > count) {
            setAmount(amount - count);
        } else if(amount == count) {
            delete();
        } else {
            log.warning(String.format("Consuming %d of %d available for item %08X", count, amount, serial));
        }
    }

    public Item findChildByType(int graphicID) {
        for(Item child : getChildren()) {
            if(child.getGraphic() == graphicID) {
                return child;
            }
        }
        return null;
    }

    public int getAmountByType(int graphicID) {
        int total = 0;

        for(Item child : getChildren()) {
            if(child.getGraphic() == graphicID) {
                total += child.getAmount();
            }
            if(child.isContainer()) {
                total += child.getAmountByType(graphicID);
            }
        }
        return total;
    }

    public void consumeByType(int graphicID, int count) {
        if(getAmountByType(graphicID) < count) {
            log.warning(String.format("Consuming %d of %d available (type %04X) for item %08X", count, getAmountByType(graphicID), graphicID, serial));
            return;
        }

        for(Item child : getChildren()) {
            if(child.getGraphic() == graphicID) {
                if(count >= child.getAmount()) {
                    // Less than we need, take everything from here and go on
                    count -= child.getAmount();
                    child.consume(child.getAmount());
                } else {
                    // More or exactly what we need, take what we need and stop
                    child.consume(count);
                    count = 0;
                    break;
                }
            }
            if(child.isContainer()) {
                int av = child.getAmountByType(graphicID);
                if(count >= av) {
                    child.consumeByType(graphicID, av);
                    count -= av;
                } else {
                    child.consumeByType(graphicID, count);
                    count = 0;
                    break;
                }
            }
        }
        if(count != 0) {
            log.severe("Logic error in consumeByType: " + count + " left of ID " + graphicID);
        }
    }

    public int getGumpID() {
        switch(graphic) {
        case Items.GFX_BACKPACK:      return Gumps.ID_BACKPACK;

        case Items.GFX_CORPSE_DEER:
        case Items.GFX_CORPSE_HUMAN:
        case Items.GFX_CORPSE_ORC:
        case Items.GFX_CORPSE_ORC_CAPTAIN:
        case Items.GFX_CORPSE_RABBIT:
        case Items.GFX_CORPSE_SKELETON:
        case Items.GFX_CORPSE_WOLF:
                                      return Gumps.ID_CORPSE;

        case Items.GFX_SPELLBOOK:     return Gumps.ID_SPELLBOOK;

        default:                      return 0;
        }
    }

    public void setDragged(Player who) {
        this.draggedBy = who;
        for(ObjectObserver o : observers) o.onDragItem(this, who);
    }

    public Player getDraggingPlayer() {
        return draggedBy;
    }

    public void dropped() {
        this.draggedBy = null;
        // dropping will cause setLocation to happen, so no observer here
    }

    public void setAmount(int amount) {
        this.amount = amount;
        for(ObjectObserver o : observers) o.onObjectUpdate(this);
    }

    public boolean isContainer() {
        return isContainer;
    }

    public boolean isWearable() {
        return isWearable;
    }

    public boolean isStackable() {
        return isStackable;
    }

    public short getWeight() {
        return weight;
    }

    public void setWeight(short weight) {
        this.weight = weight;
    }

    public short getLayer() {
        if(graphic == Items.GFX_SHOP_CONTAINER){
            return 9;
        }
        return defaultLayer;
    }

    public static Item createAtLocation(Point3D location, int graphic, int amount) {
        long serial = ObjectRegistry.get().registerItemSerial();

        Item item = new Item(serial, graphic);
        item.setLocation(location);
        item.setAmount(amount);
        ObjectRegistry.get().registerObject(item);

        return item;
    }

    public static Item createEquipped(Mobile on, int graphic, int hue) {
        long serial = ObjectRegistry.get().registerItemSerial();

        Item item = new Item(serial, graphic);
        item.setParent(on);
        item.setHue(hue);
        item.setLocation(new Point3D(0, 0, 0));
        ObjectRegistry.get().registerObject(item);
        on.equipItem(item);

        return item;
    }

    public static Item createInContainer(Item container, int graphic, int amount) {
        long serial = ObjectRegistry.get().registerItemSerial();
        if(container == null) {
            log.severe("tried to create item in null-container");
            return null;
        }

        Item item = new Item(serial, graphic);
        item.setLocation(new Point3D(50, 50, 0));
        item.setParent(container);
        item.setAmount(amount);
        ObjectRegistry.get().registerObject(item);
        container.addChild(item, item.getLocation());

        return item;
    }

    public boolean acceptsChild(Item item) {
        if(graphic == Items.GFX_SPELLBOOK && Spell.fromScrollGraphic(item.getGraphic()) == null) {
            return false;
        }

        return true;
    }

    public void onUse(Player player) {
        ItemBehavior ib = ScriptManager.instance().getItemBehavior(behavior);
        if(ib != null) {
            try {
                ib.onUse(player, this);
            } catch(Exception e) {
                log.log(Level.SEVERE, "Exception in onUse: " + e.getMessage(), e);
            }
        }
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.children = new CopyOnWriteArrayList<Item>();
    }

    @Override
    public short getFacingOverride() {
        if(graphic == Items.GFX_DARKSOURCE) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public void foundOrphan(SLObject orphan) {
        addChild((Item) orphan, orphan.getLocation());
    }
}
