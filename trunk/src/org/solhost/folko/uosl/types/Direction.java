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
package org.solhost.folko.uosl.types;

public enum Direction {
    NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST;

    public static Direction parse(short b) {
        switch(b) {
        case 0: return NORTH;
        case 1: return NORTH_EAST;
        case 2: return EAST;
        case 3: return SOUTH_EAST;
        case 4: return SOUTH;
        case 5: return SOUTH_WEST;
        case 6: return WEST;
        case 7: return NORTH_WEST;
        default: return NORTH;
        }
    }

    public short toByte() {
        switch(this) {
        case NORTH:         return 0;
        case NORTH_EAST:    return 1;
        case EAST:          return 2;
        case SOUTH_EAST:    return 3;
        case SOUTH:         return 4;
        case SOUTH_WEST:    return 5;
        case WEST:          return 6;
        case NORTH_WEST:    return 7;
        default:            return 0;
        }
    }
}
