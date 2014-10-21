package org.solhost.folko.slclient.views;

import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import org.solhost.folko.slclient.models.ImageCache;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLMap;
import org.solhost.folko.uosl.data.SLTiles;
import org.solhost.folko.uosl.data.SLTiles.LandTile;
import org.solhost.folko.uosl.types.Point2D;

public class TileView extends MeshView {
    private final Point2D location;
    private final TriangleMesh mesh;
    private final SLMap map;
    private final SLTiles tiles;

    public TileView(Point2D location) {
        this.location = location;
        this.mesh = new TriangleMesh();
        map = SLData.get().getMap();
        tiles = SLData.get().getTiles();

        computeMesh();
        setMesh(mesh);
    }

    private int getZ(int x, int y) {
        return SLData.get().getMap().getTileElevation(x, y);
    }

    private void computeMesh() {
        int x = location.getX();
        int y = location.getY();
        int z = getZ(x, y);
        int eastZ = getZ(x + 1, y);
        int southZ = getZ(x, y + 1);
        int southEastZ = getZ(x + 1, y + 1);

        mesh.getPoints().addAll(
                x,      y + 1,  0,//southZ,
                x + 1,  y + 1,  0,//southEastZ,
                x + 1,  y,      0,//eastZ,
                x,      y,      0//z
            );

        int landID = map.getTextureID(location);
        LandTile landTile = tiles.getLandTile(landID);
        Image textureImage;

        boolean isRegular = (eastZ == z && southZ == z && southEastZ == z);
        boolean canProject = (landTile.textureID != 0);

        if(!isRegular && canProject) {
            textureImage = ImageCache.getStaticImage(landTile.textureID);
            mesh.getTexCoords().addAll(
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 0.0f
                );
        } else {
            textureImage = ImageCache.getLandImage(landID);
            mesh.getTexCoords().addAll(
                    0.0f, 0.5f,
                    0.5f, 1.0f,
                    1.0f, 0.5f,
                    0.5f, 0.0f
                );
        }

        mesh.getFaces().addAll(
                    0, 0,   2, 2,   3, 3,
                    0, 0,   1, 1,   2, 2
                );

        if(textureImage != null) {
            PhongMaterial material = new PhongMaterial();
            material.setDiffuseMap(textureImage);
            setMaterial(material);
        }
    }

    public Point2D getLocation() {
        return location;
    }
}
