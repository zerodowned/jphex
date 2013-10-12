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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.solhost.folko.jphex.network.Client;
import org.solhost.folko.uosl.network.packets.LocationPacket;
import org.solhost.folko.uosl.network.packets.SLPacket;
import org.solhost.folko.uosl.network.packets.SendTextPacket;
import org.solhost.folko.uosl.network.packets.SoundPacket;
import org.solhost.folko.uosl.types.Items;
import org.solhost.folko.uosl.types.Point3D;
import org.solhost.folko.uosl.types.Spell;

public class Player extends Mobile {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger("jphex.types");
    public static final int ACCESS_ITEM_RANGE = 2;
    private transient Client client;
    private transient TargetLocationHandler targetLocation;
    private transient TargetObjectHandler targetObject;
    private boolean isWalking, male;
    private CommandLevel commandLevel;
    private long seed;
    private int dragAmount;
    private String email, homepage, realName, password, pcSpecs;
    private transient Map<Long, List<Item>> shopList; // current shop listing with amount etc for certain shop npc

    public Player(long serial) {
        super(serial);
    }

    public void setMale(boolean male) {
        this.male = male;
    }

    public boolean isMale() {
        return male;
    }

    public void setWalking(boolean walking) {
        this.isWalking = walking;
    }

    public boolean isWalking() {
        return isWalking;
    }

    public void setCommandLevel(CommandLevel level) {
        this.commandLevel = level;
    }

    public CommandLevel getCommandLevel() {
        return commandLevel;
    }

    public boolean isAdmin() {
        return commandLevel == CommandLevel.ADMIN;
    }

    public boolean isOnline() {
        return client != null;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
        for(ObjectObserver o : observers) o.onObjectUpdate(this);
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPcSpecs() {
        return pcSpecs;
    }

    public void setPcSpecs(String pcSpecs) {
        this.pcSpecs = pcSpecs;
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() && isOnline();
    }

    public int getDragAmount() {
        return dragAmount;
    }

    public void setDragAmount(int dragAmount) {
        this.dragAmount = dragAmount;
    }

    public void sendPacket(SLPacket packet) {
        if(isOnline()) {
            client.send(packet);
        }
    }

    public void sendSysMessage(String text) {
        SendTextPacket packet = new SendTextPacket(null, SendTextPacket.MODE_SYSMSG,
               SendTextPacket.COLOR_SYSTEM, text);
        sendPacket(packet);
    }

    public boolean tryAccess(Item item) {
        if(isDead()) {
            sendSysMessage("I am dead and cannot do that.");
            return false;
        }

        if(item.isOnGround() && !item.inRange(getLocation(), ACCESS_ITEM_RANGE)) {
            sendSysMessage("That is too far away.");
            return false;
        }

        if(!item.isOnGround()) {
            SLObject root = item.getRoot();
            if(root instanceof Mobile && root != this) {
                sendSysMessage("That doesn't belong to you.");
                return false;
            }
        }

        return true;
    }

    public void sendSound(int soundID) {
        SoundPacket packet = new SoundPacket(soundID);
        sendPacket(packet);
    }

    public void sendHexPacket(short id, String data) {
        SLPacket packet = SLPacket.fromHexString(id, data);
        sendPacket(packet);
    }

    public void targetObject(TargetObjectHandler handler) {
        sendSysMessage("Target an object by doubleclicking it");
        this.targetObject = handler;
    }

    public void targetLocation(TargetLocationHandler handler) {
        sendSysMessage("Target a location using Quas In Lor");
        this.targetLocation = handler;
    }

    public boolean hasObjectTarget() {
        return targetObject != null;
    }

    public boolean hasLocationTarget() {
        return targetLocation != null;
    }

    public void onTargetLocation(Point3D target) {
        try {
            TargetLocationHandler handler = targetLocation;
            targetLocation = null;
            handler.onTarget(target);
        } catch(Exception e) {
            log.log(Level.SEVERE, "Error in onTargetLocation: " + e.getMessage(), e);
        }
    }

    public void onTargetObject(SLObject obj) {
        try {
            TargetObjectHandler handler = targetObject;
            targetObject = null;
            handler.onTarget(obj);
        } catch(Exception e) {
            log.log(Level.SEVERE, "Error in onTarget: " + e.getMessage(), e);
        }
    }

    public boolean isShopping() {
        return shopList == null || shopList.size() != 0;
    }

    public void initShopping(Item shopContainer) {
        if(shopList == null) {
            this.shopList = new HashMap<Long, List<Item>>();
        }
        Collection<Item> items = shopContainer.getChildren();
        List<Item> inventory = new ArrayList<Item>(items.size());
        for(Item itm : items) {
            Item entry = itm.createCopy(itm.getSerial());
            entry.setAmount(0);
            inventory.add(entry);
        }
        shopList.put(shopContainer.getSerial(), inventory);
    }

    public List<Item> getShopItems(Item shopContainer) {
        return shopList.get(shopContainer.getSerial());
    }

    public void finishShopping(Item shopContainer) {
        shopList.remove(shopContainer);
    }

    public void sendLocation() {
        SLPacket locationPacket = new LocationPacket(this);
        sendPacket(locationPacket);
    }

    public Item getSpellbook() {
        Item backpack = getBackpack();
        if(backpack == null) {
            return null;
        }
        Item book = backpack.findChildByType(Items.GFX_SPELLBOOK);
        if(book == null) {
            return null;
        }
        return book;
    }

    public boolean hasSpell(Spell spell) {
        Item book = getSpellbook();
        if(book == null) {
            return false;
        }
        if(book.findChildByType(spell.toScrollType()) == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean canFight() {
        return super.canFight() && isOnline();
    }

    @Override
    public boolean canRefresh() {
        return super.canRefresh() && isOnline();
    }
}
