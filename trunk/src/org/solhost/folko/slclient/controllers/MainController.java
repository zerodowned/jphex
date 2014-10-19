package org.solhost.folko.slclient.controllers;

import java.util.Collections;
import java.util.logging.Logger;

import org.solhost.folko.slclient.models.GameState;
import org.solhost.folko.slclient.models.GameState.State;
import org.solhost.folko.slclient.views.GameView;
import org.solhost.folko.slclient.views.LoginView;
import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.types.Direction;
import org.solhost.folko.uosl.types.Point3D;

import javafx.application.Platform;
import javafx.stage.Stage;

public class MainController {
    private static final Logger log = Logger.getLogger("slclient.main");
    private final GameState game;
    private final NetworkController networkController;
    private final Stage stage;
    private LoginView loginView;
    private GameView gameView;

    public MainController(Stage stage) {
        this.stage = stage;
        this.game = new GameState();
        this.networkController = new NetworkController(this);

        game.stateProperty().addListener((g, from, to) -> onGameStateChange(from, to));
    }

    public void startGame() {
        loginView = new LoginView(this);
        stage.setScene(loginView.getScene());
    }

    private void onGameStateChange(State oldState, State newState) {
        log.fine("Game state changed from " + oldState + " to " + newState);
        switch(newState) {
        case DISCONNECTED:
            onDisconnect(oldState);
            break;
        case CONNECTED:
            onConnected(oldState);
            break;
        case LOGGED_IN:
            onLogin(oldState);
            break;
        default:
            break;
        }
    }

    // user entered login data
    public void onLoginRequest(String host, String name, String password) {
        game.getPlayer().setName(name);
        game.getPlayer().setPassword(password);
        loginView.setBusy(true);
        networkController.tryConnect(host);
    }

    private void onDisconnect(GameState.State oldState) {
        if(oldState == State.CONNECTED) {
            // login view still active, but server kicked us
            Platform.runLater(() -> {
                loginView.setBusy(false);
            });
        }
    }

    private void onConnected(GameState.State oldState) {
        log.info("Connected to server");
        game.tryLogin();
    }

    public void onLoginFail(String message) {
        networkController.stopNetwork();
        Platform.runLater(() -> {
            loginView.showError("Login failed: " + message);
            loginView.setBusy(false);
        });
    }

    private void onLogin(State oldState) {
        Platform.runLater(() -> {
            gameView = new GameView(this);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.centerOnScreen();
            stage.setScene(gameView.getScene());
        });
    }

    public void onNetworkError(String reason) {
        if(game.getState() == State.DISCONNECTED || game.getState() == State.CONNECTED) {
            // means we couldn't connect or were kicked while logging in
            Platform.runLater(() -> {
                loginView.showError("Couldn't connect: " + reason);
                loginView.setBusy(false);
            });
        }
    }

    public Stage getStage() {
        return stage;
    }

    public GameState getGameState() {
        return game;
    }

    public void onRequestMove(Direction dir) {
        // just testing
        Point3D oldLoc = game.getPlayer().getLocation();
        Point3D newLoc = SLData.get().getElevatedPoint(oldLoc, dir, p -> Collections.emptyList());
        if(newLoc == null) {
            // move not allowed
            return;
        }
        game.getPlayer().setLocation(newLoc);
    }

    public void onUpdateRangeChange(int sceneRadius) {
        game.setUpdateRange(sceneRadius);
    }
}
