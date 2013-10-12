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
package org.solhost.folko.uosl.data;

import org.solhost.folko.uosl.types.Point3D;

public class SLStatic {
    private long serial;
    private int staticID;
    private int hue;
    private Point3D location;

    public SLStatic(long serial) {
        this.serial = serial;
    }

    public long getSerial() {
        return serial;
    }

    public void setSerial(long serial) {
        this.serial = serial;
    }

    public int getStaticID() {
        return staticID;
    }

    public void setStaticID(int staticID) {
        this.staticID = staticID;
    }

    public int getHue() {
        return hue;
    }

    public void setHue(int hue) {
        this.hue = hue;
    }

    public void setLocation(Point3D position) {
        this.location = position;
    }

    public Point3D getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("<SLStatic: serial = %08X, static = %04X, hue = %04X, position = %s>",
                serial, staticID, hue, location.toString());
    }
}
