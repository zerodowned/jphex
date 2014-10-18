package org.solhost.folko.slclient.views;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import org.solhost.folko.uosl.data.SLArt;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLMap;
import org.solhost.folko.uosl.data.SLArt.ArtEntry;
import org.solhost.folko.uosl.types.Point2D;

public class Tile extends MeshView {
    private final Point2D location;
    private final TriangleMesh mesh;

    public Tile(Point2D location) {
        this.location = location;
        this.mesh = new TriangleMesh();

        computeMesh();
        applyTexture();
        setMesh(mesh);
    }

    private int getZ(int x, int y) {
        return SLData.get().getMap().getTileElevation(x, y);
    }

    private void computeMesh() {
        int x = location.getX();
        int y = location.getY();

        float vertices[] = {
                x,      y + 1,  getZ(x, y + 1),
                x + 1,  y + 1,  getZ(x + 1, y + 1),
                x + 1,  y,      getZ(x + 1, y),
                x,      y,      getZ(x, y)
        };

        float texels[] = {
                0.0f, 0.5f,
                0.5f, 1.0f,
                1.0f, 0.5f,
                0.5f, 0.0f,
        };

        int faces[] = {
                0, 0,   2, 2,   3, 3,
                0, 0,   1, 1,   2, 2,
        };

        mesh.getPoints().setAll(vertices);
        mesh.getTexCoords().setAll(texels);
        mesh.getFaces().setAll(faces);
    }

    private void applyTexture() {
        SLMap map = SLData.get().getMap();
        SLArt art = SLData.get().getArt();

        int landID = map.getTextureID(location);
        ArtEntry artEntry = art.getLandArt(landID);

        WritableImage textureImage = new WritableImage(artEntry.image.getWidth(), artEntry.image.getHeight());
        SwingFXUtils.toFXImage(artEntry.image, textureImage);

        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(textureImage);

        setMaterial(material);
    }
}
