package org.solhost.folko.slclient.views;

import org.solhost.folko.slclient.models.ImageCache;
import org.solhost.folko.slclient.models.SLItem;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

public class ItemView extends MeshView {

    public ItemView(SLItem itm) {
        int graphic = itm.getGraphic();
        Image itemImage = ImageCache.getStaticImage(graphic);
        if(itemImage == null) {
            return;
        }

        float w = (float) (itemImage.getWidth() / 44.0);
        float h = (float) (itemImage.getHeight() / 44.0);
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
                    0f, 0f, -0.1f,
                    0f, h, -0.1f,
                    w, h, -0.1f,
                    w, 0f, -0.1f
                );
        mesh.getTexCoords().addAll(
                    0f, 0f,
                    0f, 1f,
                    1f, 1f,
                    1f, 0f
                );
        mesh.getFaces().addAll(
                    0, 0, 1, 1, 3, 3,
                    1, 1, 2, 2, 3, 3
                );

        setMesh(mesh);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(itemImage);
        setMaterial(material);
    }
}
