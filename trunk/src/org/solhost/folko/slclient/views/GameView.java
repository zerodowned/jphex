package org.solhost.folko.slclient.views;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;

import org.solhost.folko.slclient.controllers.MainController;
import org.solhost.folko.slclient.models.GameState;
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

    private final Map<Point2D, Tile> tileCache;

    public GameView(MainController mainController) {
        this.mainController = mainController;
        this.game = mainController.getGameState();

        tileCache = new HashMap<>();

        group = new Group();
        scene = new Scene(group);
        scene.setFill(Color.BLACK);

        cam = new PerspectiveCamera(true);
        cam.setNearClip(0.1);
        cam.setFarClip(10000);
        cam.setTranslateZ(-2000);
        cam.setRotate(-45.0);
        scene.setCamera(cam);

        AmbientLight light = new AmbientLight(Color.WHITE);
        group.getChildren().add(light);

        render();
        game.getPlayer().locationProperty().addListener((p, o, n) -> render());
        scene.widthProperty().addListener((o, ow, nw) -> render());
        scene.heightProperty().addListener((o, ow, nw) -> render());
        scene.setOnKeyPressed(e -> onKeyPress(e.getCode()));
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

    private void render() {
        Point3D sceneCenter = game.getPlayer().getLocation();
        group.setScaleX(TILE_SIZE);
        group.setScaleY(TILE_SIZE);

        int startX = sceneCenter.getX();
        int startY = sceneCenter.getY();
        int numX = 20;
        int numY = 20;

        for(int x = startX - numX; x < startX + numX; x++) {
            for(int y = startY - numY; y < startY + numY; y++) {
                Point2D point = new Point2D(x, y);
                renderMap(point, sceneCenter);
            }
        }
    }

    private void renderMap(Point2D point, Point3D center) {
        // cache is just for testing, replace by something smart later
        Tile tile = tileCache.get(point);
        if(tile == null) {
            tile = new Tile(point);
            tileCache.put(point, tile);
        }
        tile.setTranslateX(-center.getX());
        tile.setTranslateY(-center.getY());
        tile.setTranslateZ(-center.getZ());
        if(!group.getChildren().contains(tile)) {
            group.getChildren().add(tile);
        }
    }

    public Scene getScene() {
        return scene;
    }
}
