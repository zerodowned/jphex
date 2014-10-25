package org.solhost.folko.slclient.models;

import org.solhost.folko.slclient.views.Texture;
import org.solhost.folko.uosl.data.SLArt;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLArt.ArtEntry;

public class TexturePool {
    private static Texture[] landTextures;
    private static Texture[] staticTextures;

    private TexturePool() {
    }

    public static void load() {
        landTextures = new Texture[SLArt.NUM_LAND_ARTS];
        staticTextures = new Texture[SLArt.NUM_STATIC_ARTS];

        SLArt art = SLData.get().getArt();

        for(int i = 0; i < SLArt.NUM_LAND_ARTS; i++) {
            ArtEntry entry = art.getLandArt(i);
            if(entry != null && entry.image != null) {
                landTextures[i] = new Texture(entry.image);
            }
        }

        for(int i = 0; i < SLArt.NUM_STATIC_ARTS; i++) {
            ArtEntry entry = art.getStaticArt(i, false);
            if(entry != null && entry.image != null) {
                staticTextures[i] = new Texture(entry.image);
            }
        }
    }

    public static Texture getLandTexture(int id) {
        return landTextures[id];
    }

    public static Texture getStaticTexture(int id) {
        return staticTextures[id];
    }
}
