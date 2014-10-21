package org.solhost.folko.slclient.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javafx.collections.MapChangeListener.Change;
import javafx.scene.AmbientLight;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
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
    private final Scene scene;
    private final Group group;
    private final GameState game;

    private final Set<Point2D> visiblePoints;
    private final Map<Long, ItemView> visibleItems;

    public GameView(MainController mainController) {
        this.mainController = mainController;
        this.game = mainController.getGameState();

        visiblePoints = new HashSet<>();
        visibleItems = new HashMap<>();

        group = new Group();
        group.setAutoSizeChildren(false);

        AmbientLight ambient = new AmbientLight(Color.WHITE);
        group.getChildren().add(ambient);

        Camera cam = new PerspectiveCamera(true);
        cam.setRotate(-45.0);
        cam.setScaleX(TILE_SIZE);
        cam.setScaleY(TILE_SIZE);
        cam.setScaleZ(-1);

        cam.setNearClip(0);
        cam.setFarClip(-20);
        cam.setTranslateZ(1);

        scene = new Scene(group);
        scene.setFill(Color.BLACK);
        scene.setCamera(cam);

        game.getPlayer().locationProperty().addListener((p, o, n) -> updateScene());
        game.objectsInRangeProperty().addListener((Change<? extends Long, ? extends SLObject> c) -> onObjectsChange(c));
        scene.widthProperty().addListener((o, ow, nw) -> onResize());
        scene.heightProperty().addListener((o, ow, nw) -> onResize());
        scene.setOnKeyPressed(e -> onKeyPress(e.getCode()));
        scene.setOnScroll(e -> onScroll(e.getDeltaX(), e.getDeltaY()));
        scene.setOnZoom(e -> onZoom(e.getZoomFactor()));
    }

    private void onZoom(double zoomFactor) {
        double newFactor = group.getScaleX() * zoomFactor;
        if(newFactor > 0.5 && newFactor < 4) {
            group.setScaleX(newFactor);
            group.setScaleY(newFactor);
            recalculateUpdateRadius();
        }
    }

    private void onScroll(double deltaX, double deltaY) {
        double angle = Math.toDegrees(Math.atan2(-deltaX, deltaY));
        if(angle < 0) {
            angle += 360;
        }
        mainController.onRequestMove(Direction.fromAngle(angle));
    }

    private void onKeyPress(KeyCode code) {
        switch(code) {
        case UP:        mainController.onRequestMove(Direction.NORTH_WEST); break;
        case DOWN:      mainController.onRequestMove(Direction.SOUTH_EAST); break;
        case LEFT:      mainController.onRequestMove(Direction.SOUTH_WEST); break;
        case RIGHT:     mainController.onRequestMove(Direction.NORTH_EAST); break;
        case CLOSE_BRACKET: onZoom(1.1); break;
        case SLASH:         onZoom(0.9); break;
        default:        log.finer("Unhandled key pressed: " + code);
        }
    }

    private void onResize() {
        recalculateUpdateRadius();
    }

    private void recalculateUpdateRadius() {
        int xRadius = (int) (scene.getWidth() / 2.0 / TILE_SIZE / group.getScaleX()) + 1;
        int yRadius = (int) (scene.getHeight() / 2.0 / TILE_SIZE / group.getScaleY()) + 1;
        int sceneRadius = Math.max(xRadius, yRadius) * 2;
        mainController.onUpdateRangeChange(sceneRadius);
        updateScene();
    }

    private void updateScene() {
        Point3D sceneCenter = game.getPlayer().getLocation();
        int centerX = sceneCenter.getX();
        int centerY = sceneCenter.getY();
        int sceneRadius = game.getUpdateRange();

        group.setTranslateX(-centerX);
        group.setTranslateY(-centerY);

        for(Iterator<Node> it = group.getChildren().iterator(); it.hasNext(); ) {
            Node node = it.next();
            if(!(node instanceof TileView)) {
                continue;
            }
            TileView tv = (TileView) node;
            if(tv.getLocation().distanceTo(sceneCenter) > sceneRadius) {
                visiblePoints.remove(tv.getLocation());
                it.remove();
            }
        }

        game.visitUpdateRange(point -> {
            if(!visiblePoints.contains(point)) {
                TileView tile = new TileView(point);
                group.getChildren().add(tile);
                visiblePoints.add(point);
            }
        });
    }

    private void onObjectsChange(Change<? extends Long, ? extends SLObject> change) {
        long serial = change.getKey();
        if(change.wasAdded() && !visibleItems.containsKey(serial)) {
            SLObject obj = change.getValueAdded();
            if(obj instanceof SLItem) {
//                ItemView iv = new ItemView((SLItem) obj);
//                iv.setTranslateX(obj.getLocation().getX());
//                iv.setTranslateY(obj.getLocation().getY());
//                iv.setTranslateZ(0);
//                visibleItems.put(serial, iv);
//                group.getChildren().add(iv);
            }
        } else if(change.wasRemoved() && visibleItems.containsKey(serial)) {
            group.getChildren().remove(visibleItems.get(serial));
        }
    }

    public Scene getScene() {
        return scene;
    }
}
