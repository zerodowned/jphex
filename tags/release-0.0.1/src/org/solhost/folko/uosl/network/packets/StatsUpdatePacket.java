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
package org.solhost.folko.uosl.network.packets;

import org.solhost.folko.uosl.network.SendableMobile;
import org.solhost.folko.uosl.types.Attribute;

public class StatsUpdatePacket extends SLPacket {
    public static final short ID = 0x4C;

    public StatsUpdatePacket(SendableMobile mob, boolean relativeHitsOnly) {
        initWrite(ID, 0x14);

        addUDWord(mob.getSerial());

        int hits = (int) mob.getAttribute(Attribute.HITS),
            maxHits = (int) mob.getAttribute(Attribute.MAX_HITS);

        if(relativeHitsOnly) {
            hits = hits * 100 / maxHits;
            maxHits = 100;
        }

        addUWord(maxHits);
        addUWord(hits);
        if(relativeHitsOnly) {
            addUWord(0);
            addUWord(0);
            addUWord(0);
            addUWord(0);
        } else {
            addUWord((int) mob.getAttribute(Attribute.MAX_MANA));
            addUWord((int) mob.getAttribute(Attribute.MANA));
            addUWord((int) mob.getAttribute(Attribute.MAX_FATIGUE));
            addUWord((int) mob.getAttribute(Attribute.FATIGUE)); //needs to be > 5 or client won't be able to walk
        }
    }

    @Override
    public short getID() {
        return ID;
    }
}
