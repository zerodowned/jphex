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

import java.awt.image.BufferedImage;
import java.io.IOException;

public class SLGumps {
    private final SLDataFile gumps;

    public class GumpEntry {
        public int id;
        public BufferedImage image;
    }

    public SLGumps(String gumpsPath) throws IOException {
        gumps = new SLDataFile(gumpsPath, true);
    }

    public synchronized GumpEntry getGump(int id) {
        GumpEntry res = new GumpEntry();

        // skip previous gumps as we don't have a GUMPIDX file
        gumps.seek(0);
        for(int i = 0; i < id; i++) {
            gumps.readUByte(); // unknown
            int width = gumps.readUWord();
            int height = gumps.readUWord();
            gumps.skip(width * height * 2);
        }

        gumps.readUByte(); // unknown
        int width = gumps.readUWord();
        int height = gumps.readUWord();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                int color = gumps.readUWord();
                image.setRGB(x, y, SLColor.convert555(color, 0xFF));
            }
        }
        res.id = id;
        res.image = image;
        return res;
    }
}
