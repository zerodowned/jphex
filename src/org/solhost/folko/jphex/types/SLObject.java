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
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jruby.RubyObject;
import org.solhost.folko.uosl.network.SendableObject;
import org.solhost.folko.uosl.types.Point2D;
import org.solhost.folko.uosl.types.Point3D;

public abstract class SLObject implements Serializable, SendableObject {
    private static final long serialVersionUID = 1L;
    protected transient Set<ObjectObserver> observers;
    protected transient SLObject parent, backupParent; // backup for drag cancelling
    protected Map<String, RubyObject> scriptProperties;
    protected long serial;
    protected Point3D location;
    protected int graphic, hue;
    protected String name;
    protected boolean deleted, hidden;

    public SLObject(long serial) {
        this.serial = serial;
        this.deleted = false;
        this.scriptProperties = new HashMap<String, RubyObject>();
        this.observers = new CopyOnWriteArraySet<ObjectObserver>();
    }

    public void addObserver(ObjectObserver o) {
        observers.add(o);
    }

    public void removeObserver(ObjectObserver o) {
        observers.remove(o);
    }

    // called on first creation and after loading on server start
    public abstract void onLoad();

    public void setVisible(boolean visible) {
        this.hidden = !visible;
        for(ObjectObserver o : observers) o.onObjectUpdate(this);
    }

    public void delete() {
        for(ObjectObserver o : observers) o.onObjectDelete(this);
        deleted = true;
        parent = null;
        backupParent = null;
    }

    public void setParent(SLObject parent) {
        this.parent = parent;
    }

    public SLObject getParent() {
        return parent;
    }

    public void rememberParent() {
        this.backupParent = parent;
    }

    public void restoreParent() {
        this.parent = backupParent;
    }

    public void clearParent() {
        parent = null;
    }

    public SLObject getRoot() {
        SLObject iter = getParent(), prevIter = this;
        while(iter != null) {
            prevIter = iter;
            iter = iter.getParent();
        }
        return prevIter;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isVisible() {
        return !deleted && !hidden;
    }

    public int distanceTo(SLObject other) {
        return location.distanceTo(other.getLocation());
    }

    public int distanceTo(Point2D p) {
        return location.distanceTo(p);
    }

    public boolean inRange(Point2D p, int range) {
        return distanceTo(p) <= range;
    }

    public int getGraphic() {
        return graphic;
    }

    public void setGraphic(int graphic) {
        this.graphic = graphic;
        for(ObjectObserver o : observers) o.onObjectUpdate(this);
    }

    public int getHue() {
        return hue;
    }

    public void setHue(int hue) {
        this.hue = hue;
        for(ObjectObserver o : observers) o.onObjectUpdate(this);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(Point3D newPosition) {
        Point3D old = this.location;
        this.location = newPosition;
        for(ObjectObserver o : observers) o.onLocationChanged(this, old);
    }

    public Point3D getLocation() {
        return location;
    }

    public long getSerial() {
        return serial;
    }

    public void setProperty(String name, RubyObject value) {
        scriptProperties.put(name, value);
    }

    public RubyObject getProperty(String name) {
        return scriptProperties.get(name);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.observers = new CopyOnWriteArraySet<ObjectObserver>();
    }

    // at startup
    public abstract void foundOrphan(SLObject orphan);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (serial ^ (serial >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SLObject other = (SLObject) obj;
        if (serial != other.serial)
            return false;
        return true;
    }
}
