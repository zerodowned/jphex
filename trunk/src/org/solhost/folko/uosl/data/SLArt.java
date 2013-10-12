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
import java.util.LinkedList;
import java.util.List;

public class SLArt {
    private final SLDataFile artData, artIdx;

    public SLArt(String artPath, String idxPath) throws IOException {
        artData = new SLDataFile(artPath, false);
        artIdx = new SLDataFile(idxPath, true);
    }

    public class ArtEntry {
        public int id;
        public long unknown;
        public BufferedImage image;
    }

    public class AnimationEntry {
        public int id;
        public List<ArtEntry> frames;
    }

    private synchronized Integer getOffset(int artID) {
        long idxOffset = artID * 12;
        artIdx.seek((int) idxOffset);
        long offset = artIdx.readUDWord();
        long length = artIdx.readUDWord();

        if(offset == -1 || length == -1) {
            return null;
        }
        return (int) offset;
    }

    public synchronized ArtEntry getLandArt(int landID) {
        Integer offset = getOffset(landID);
        if(offset == null) {
            return null;
        }
        artData.seek(offset);
        ArtEntry entry = new ArtEntry();
        entry.unknown = artIdx.readUDWord();
        entry.id = landID;
        entry.image = new BufferedImage(44, 44, BufferedImage.TYPE_INT_ARGB);
        for(int y = 0; y < 44; y++) {
            int width;
            if(y < 22) {
                width = 2 * (y + 1);
            } else {
                width = 2 * (44 - y);
            }
            for(int x = 0; x < 44; x++) {
                if(x < 22 - width / 2) {
                    entry.image.setRGB(x, y, 0);
                } else if (x >= 22 - width / 2 && x < 22 + width / 2) {
                    entry.image.setRGB(x, y, SLColor.convert555(artData.readUWord(), 0xFF));
                } else {
                    entry.image.setRGB(x, y, 0);
                }
            }
        }
        return entry;
    }

    public synchronized ArtEntry getStaticArt(int staticID, boolean translucent) {
        Integer offset = getOffset(staticID + 0x4000);
        if(offset == null) {
            return null;
        }
        artData.seek(offset);
        ArtEntry entry = new ArtEntry();
        entry.unknown = artIdx.readUDWord();
        entry.id = staticID;

        int alpha = 0xFF;
        if(translucent){
            alpha = 0xD0;
        }

        artData.readUDWord(); // unknown, probably size of encoded data
        int width = artData.readUWord();
        int height = artData.readUWord();

        if(width == 0 || height == 0) {
            entry.image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        } else {
            entry.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            int[] rowStarts = new int[height];
            for(int i = 0; i < height; i++) {
                rowStarts[i] = artData.readUWord();
            }
            offset = artData.getPosition();
            for(int y = 0; y < height; y++) {
                int x = 0;
                artData.seek(offset + rowStarts[y] * 2);
                int numTransparent = artData.readUWord();
                int numVisible = artData.readUWord();

                // transparent start
                x += numTransparent;

                // real pixels
                for(int i = 0; i < numVisible; i++) {
                    int color = artData.readUWord();
                    if(color != 0) { // 0 means transparent here
                        entry.image.setRGB(x, y, SLColor.convert555(color, alpha));
                    }
                    x++;
                }

                // transparent end -> nothing to do
            }
        }

        return entry;
    }

    public AnimationEntry getAnimationEntry(int id) {
        AnimationEntry res = new AnimationEntry();
        res.id = id;
        res.frames = new LinkedList<ArtEntry>();
        for(int i = 0; i < getNumFrames(id); i++) {
            ArtEntry entry = getStaticArt(0x4000 + id + i, false);
            if(entry == null) {
                break;
            }
            res.frames.add(entry);
        }
        if(res.frames.size() == 0) {
            return null;
        }

        return res;
    }

    private int getNumFrames(int id) {
        // TODO: Fill
        switch(id) {
        case 0:         return 1;
        default:        return 6;
        }
    }
}
