package org.solhost.folko.slclient.views;

import org.solhost.folko.slclient.models.SLItem;
import org.solhost.folko.uosl.data.SLArt.ArtEntry;
import org.solhost.folko.uosl.data.SLArt;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLTiles.StaticTile;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ItemView extends ImageView {

    public ItemView(SLItem itm) {
        SLArt art = SLData.get().getArt();
        int graphic = itm.getGraphic();
        ArtEntry entry = art.getStaticArt(graphic, (itm.getTileInfo().flags & StaticTile.FLAG_TRANSLUCENT) != 0);
        Image itemImage = SwingFXUtils.toFXImage(entry.image, null);
        setImage(itemImage);

        setScaleX(1 / 44.0);
        setScaleY(1 / 44.0);
        setTranslateX(itm.getLocation().getX());
        setTranslateY(itm.getLocation().getY());
        setTranslateZ(itm.getLocation().getZ());
    }
}
