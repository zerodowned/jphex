package org.solhost.folko.slclient.views;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

import org.solhost.folko.slclient.controllers.MainController;
import org.solhost.folko.slclient.models.GameState;

public class GameView {
    private final MainController mainController;
    private final Scene scene;
    private final GameState game;

    public GameView(MainController mainController) {
        this.mainController = mainController;
        this.game = mainController.getGameState();

        StackPane pane = new StackPane();
        scene = new Scene(pane);
    }

    public Scene getScene() {
        return scene;
    }
}
