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

import java.io.IOException;

import org.solhost.folko.uosl.types.Point2D;

public class SLMap {
    private final SLDataFile mapFile;

    private class Tile {
        int textureID;
        byte elevation;
    }

    public SLMap(String map0Path) throws IOException {
        mapFile = new SLDataFile(map0Path, true);
    }

    private synchronized Tile readTile(Point2D pos) {
        Tile res = new Tile();
        int cell = pos.getCellIndex();
        int tile = pos.getTileIndex();
        int offset = cell * 196 + 4 + tile * 3;

        mapFile.seek(offset);
        res.textureID = mapFile.readUWord();
        res.elevation = mapFile.readSByte();

        return res;
    }

    public long getColor(int cellID) {
        int offset = cellID * 196;
        mapFile.seek(offset);
        return mapFile.readUDWord();
    }

    public byte getElevation(int x, int y) {
        Tile tile = readTile(new Point2D(x, y));
        return tile.elevation;
    }

    public byte getElevation(Point2D pos) {
        Tile tile = readTile(pos);
        return tile.elevation;
    }

    public int getTextureID(Point2D pos) {
        Tile tile = readTile(pos);
        return tile.textureID;
    }
}
