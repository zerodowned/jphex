package org.solhost.folko.slclient.views;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public class Texture {
    private final int id, width, height;

    public Texture(BufferedImage image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        if(image.getType() != BufferedImage.TYPE_INT_ARGB) {
            throw new RuntimeException("Can only open INT_ARGB images");
        }

        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                buffer.put((byte) ((argb >> 16) & 0xFF));
                buffer.put((byte) ((argb >> 8) & 0xFF));
                buffer.put((byte) ((argb >> 0) & 0xFF));
                buffer.put((byte) ((argb >> 24) & 0xFF));
            }
        }
        buffer.rewind();

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(id, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(id, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    public void setTextureUnit(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public int getTextureId() {
        return id;
    }
}
