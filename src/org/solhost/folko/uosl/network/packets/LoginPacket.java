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

import java.nio.ByteBuffer;

public class LoginPacket extends SLPacket {
    public static final short ID = 0x01;
    private long serial, seed, version;
    private String name, homepage, email, realName, pcSpecs, password;
    private short gender;
    private short strength, dexterity, intelligence, skinHue, hairHue, hairStyle;

    public static LoginPacket read(ByteBuffer buffer, int length) {
        LoginPacket res = new LoginPacket();
        res.serial          = readUDWord(buffer);
        res.seed            = readUDWord(buffer);
        res.name            = readString(buffer, 30);
        res.homepage        = readString(buffer, 128);
        res.version         = readUDWord(buffer);
        res.email           = readString(buffer, 128);
        res.realName        = readString(buffer, 128);
        res.pcSpecs         = readString(buffer, 128);
        res.password        = readString(buffer, 30);
        res.gender          = readUByte(buffer);
        res.strength        = readUByte(buffer);
        res.dexterity       = readUByte(buffer);
        res.intelligence    = readUByte(buffer);
        res.skinHue         = readUByte(buffer);
        res.hairHue         = readUByte(buffer);
        res.hairStyle       = readUByte(buffer);

        return res;
    }

    @Override
    public short getID() {
        return ID;
    }

    public long getSerial() {
        return serial;
    }

    public long getSeed() {
        return seed;
    }

    public long getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getHomepage() {
        return homepage;
    }

    public String getEmail() {
        return email;
    }

    public String getRealName() {
        return realName;
    }

    public String getPcSpecs() {
        return pcSpecs;
    }

    public String getPassword() {
        return password;
    }

    public short getGender() {
        return gender;
    }

    public short getStrength() {
        return strength;
    }

    public short getDexterity() {
        return dexterity;
    }

    public short getIntelligence() {
        return intelligence;
    }

    public short getSkinHue() {
        return skinHue;
    }

    public short getHairHue() {
        return hairHue;
    }

    public short getHairStyle() {
        return hairStyle;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("<LoginPacket: ");
        builder.append(String.format("serial = %08X, ", serial));
        builder.append(String.format("seed = %08X, ", seed));
        builder.append(String.format("str = %d, ", strength));
        builder.append(String.format("dex = %d, ", dexterity));
        builder.append(String.format("int = %d, ", intelligence));
        builder.append(String.format("skinHue = %d, ", skinHue));
        builder.append(String.format("hairHue = %d, ", hairHue));
        builder.append(String.format("hairStyle = %d", hairStyle));
        builder.append(">");

        return builder.toString();
    }
}
