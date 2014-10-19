package org.solhost.folko.slclient.views;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javafx.collections.MapChangeListener.Change;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import org.solhost.folko.slclient.controllers.MainController;
import org.solhost.folko.slclient.models.GameState;
import org.solhost.folko.slclient.models.SLItem;
import org.solhost.folko.slclient.models.SLObject;
import org.solhost.folko.uosl.types.Direction;
import org.solhost.folko.uosl.types.Point2D;
import org.solhost.folko.uosl.types.Point3D;

public class GameView {
    private static final Logger log = Logger.getLogger("slclient.gameview");
    private static final int TILE_SIZE = 44;
    private final MainController mainController;
    private final PerspectiveCamera cam;
    private final Scene scene;
    private final Group group;
    private final GameState game;
    private final PointLight pointLight;

    private final Map<Point2D, TileView> visibleTiles;
    private final Map<Long, ItemView> visibleItems;

    public GameView(MainController mainController) {
        this.mainController = mainController;
        this.game = mainController.getGameState();

        visibleTiles = new HashMap<>();
        visibleItems = new HashMap<>();

        group = new Group();
        group.setScaleX(TILE_SIZE);
        group.setScaleY(TILE_SIZE);
        group.setScaleZ(8.0);

        AmbientLight ambient = new AmbientLight(Color.WHITE);
        pointLight = new PointLight(new Color(0.2, 0.2, 0.2, 1));
        group.getChildren().addAll(ambient, pointLight);

        cam = new PerspectiveCamera(true);
        cam.setRotate(-45.0);
        cam.setNearClip(-128);
        cam.setFarClip(128);
        cam.setTranslateZ(-2000);

        scene = new Scene(group);
        scene.setFill(Color.BLACK);
        scene.setCamera(cam);

        game.getPlayer().locationProperty().addListener((p, o, n) -> render());
        game.objectsInRangeProperty().addListener((Change<? extends Long, ? extends SLObject> c) -> onObjectsChange(c));
        scene.widthProperty().addListener((o, ow, nw) -> onResize());
        scene.heightProperty().addListener((o, ow, nw) -> onResize());
        scene.setOnKeyPressed(e -> onKeyPress(e.getCode()));

        render();
    }

    private void onKeyPress(KeyCode code) {
        switch(code) {
        case UP:        mainController.onRequestMove(Direction.NORTH_WEST); break;
        case DOWN:      mainController.onRequestMove(Direction.SOUTH_EAST); break;
        case LEFT:      mainController.onRequestMove(Direction.SOUTH_WEST); break;
        case RIGHT:     mainController.onRequestMove(Direction.NORTH_EAST); break;
        default:        log.finer("Unhandled key pressed: " + code);
        }
    }

    private void onResize() {
        int xRadius = (int) (scene.getWidth() / 2.0 / TILE_SIZE) + 1;
        int yRadius = (int) (scene.getHeight() / 2.0 / TILE_SIZE) + 1;
        int sceneRadius = Math.max(xRadius, yRadius) * 2;
        mainController.onUpdateRangeChange(sceneRadius);
        render();
    }

    private void render() {
        Point3D sceneCenter = game.getPlayer().getLocation();
        int centerX = sceneCenter.getX();
        int centerY = sceneCenter.getY();
        int sceneRadius = game.getUpdateRange();

        cam.setTranslateX(centerX);
        cam.setTranslateY(centerY);
        pointLight.setTranslateX(-(centerX - sceneRadius));
        pointLight.setTranslateY(-(centerY + sceneRadius));

        for(Iterator<Point2D> it = visibleTiles.keySet().iterator(); it.hasNext(); ) {
            Point2D point = it.next();
            if(sceneCenter.distanceTo(point) > sceneRadius) {
                group.getChildren().remove(visibleTiles.get(point));
                it.remove();
            }
        }

        game.visitUpdateRange(point -> {
            if(!visibleTiles.containsKey(point)) {
                TileView tile = new TileView(point);
                visibleTiles.put(point, tile);
                group.getChildren().add(tile);
            }
        });
    }

    private void onObjectsChange(Change<? extends Long, ? extends SLObject> change) {
        long serial = change.getKey();
        if(change.wasAdded() && !visibleItems.containsKey(serial)) {
            SLObject obj = change.getValueAdded();
            if(obj instanceof SLItem) {
                ItemView iv = new ItemView((SLItem) obj);
                visibleItems.put(serial, iv);
                // group.getChildren().add(iv);
            }
        } else if(change.wasRemoved() && visibleItems.containsKey(serial)) {
            group.getChildren().remove(visibleItems.get(serial));
        }
    }

    public Scene getScene() {
        return scene;
    }
}
