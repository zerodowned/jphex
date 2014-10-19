package org.solhost.folko.slclient.views;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import org.solhost.folko.uosl.data.SLArt;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLMap;
import org.solhost.folko.uosl.data.SLArt.ArtEntry;
import org.solhost.folko.uosl.data.SLTiles;
import org.solhost.folko.uosl.data.SLTiles.LandTile;
import org.solhost.folko.uosl.data.SLTiles.StaticTile;
import org.solhost.folko.uosl.types.Point2D;

public class TileView extends MeshView {
    private final Point2D location;
    private final TriangleMesh mesh;
    private final SLMap map;
    private final SLArt art;
    private final SLTiles tiles;

    public TileView(Point2D location) {
        this.location = location;
        this.mesh = new TriangleMesh();
        map = SLData.get().getMap();
        art = SLData.get().getArt();
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
                x,      y + 1,  -southZ,
                x + 1,  y + 1,  -southEastZ,
                x + 1,  y,      -eastZ,
                x,      y,      -z
            );

        int landID = map.getTextureID(location);
        LandTile landTile = tiles.getLandTile(landID);
        ArtEntry artEntry;

        boolean isRegular = (eastZ == z && southZ == z && southEastZ == z);
        boolean canProject = (landTile.textureID != 0);

        if(!isRegular && canProject) {
            StaticTile tile = tiles.getStaticTile(landTile.textureID);
            artEntry = art.getStaticArt(landTile.textureID, (tile.flags & StaticTile.FLAG_TRANSLUCENT) != 0);
            mesh.getTexCoords().addAll(
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 0.0f
                );
        } else {
            artEntry = art.getLandArt(landID);
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

        Image textureImage = SwingFXUtils.toFXImage(artEntry.image, null);
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseMap(textureImage);
        setMaterial(material);
    }
}
