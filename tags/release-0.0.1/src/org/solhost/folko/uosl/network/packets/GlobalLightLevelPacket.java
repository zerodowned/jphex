package org.solhost.folko.uosl.network.packets;

public class GlobalLightLevelPacket extends SLPacket {
    public static final short ID = 0xA9;

    public GlobalLightLevelPacket(byte level) {
        initWrite(ID, 5);
        addSByte(level);
    }

    @Override
    public short getID() {
        return ID;
    }
}
