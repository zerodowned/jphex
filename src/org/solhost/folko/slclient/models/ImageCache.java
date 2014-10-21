package org.solhost.folko.slclient.models;

import org.solhost.folko.uosl.data.SLArt.ArtEntry;
import org.solhost.folko.uosl.data.SLTiles.StaticTile;
import org.solhost.folko.uosl.data.SLData;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

public class ImageCache {
    private static final int NUM_ENTRIES = 0x4000;
    private static Image[] mapTileImages, staticImages;

    private ImageCache() {
    }

    public static Image getLandImage(int landID) {
        return mapTileImages[landID];
    }

    public static Image getStaticImage(int staticID) {
        return staticImages[staticID];
    }

    public static void init() {
        mapTileImages = new Image[NUM_ENTRIES];
        staticImages = new Image[NUM_ENTRIES];

        for(int i = 0; i < NUM_ENTRIES; i++) {
            ArtEntry art = SLData.get().getArt().getLandArt(i);
            if(art != null && art.image != null) {
                mapTileImages[i] = SwingFXUtils.toFXImage(art.image, null);
            }

            StaticTile tile = SLData.get().getTiles().getStaticTile(i);
            if(tile != null) {
                art = SLData.get().getArt().getStaticArt(i, true);
                if(art != null && art.image != null) {
                    staticImages[i] = SwingFXUtils.toFXImage(art.image, null);
                }
            }
        }
    }
}
