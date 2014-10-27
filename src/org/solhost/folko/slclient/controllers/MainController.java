package org.solhost.folko.slclient.controllers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.solhost.folko.slclient.models.GameState;
import org.solhost.folko.slclient.models.GameState.State;
import org.solhost.folko.slclient.views.GameView;
import org.solhost.folko.slclient.views.LoginView;
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

    public void showLoginScreen() {
        loginView = new LoginView(this);
        stage.setScene(loginView.getScene());
        stage.show();

        new Thread(() -> {
            // game.getPlayer().setLocation(new Point3D(0, 0, 0));
            game.getPlayer().setLocation(new Point3D(379, 607, 0));
            game.onLoginSuccess();
        }).start();
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
        // stop JavaFX, start LWJGL
        FutureTask<Void> task = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            stage.hide();
        }, null);

        if(Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }

        try {
            task.get(); // wait for task to complete
            gameView = new GameView(this);
            new Thread(() -> gameView.run()).start();
        } catch (InterruptedException | ExecutionException e) {
            log.log(Level.FINE, "Login stopped: " + e.getMessage(), e);
        }
    }

    public void onGameClosed() {
        log.fine("Stopping game...");
        networkController.stopNetwork();
        System.exit(0);
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

    public void onGameError(String reason) {
        gameView.dispose();
        Platform.runLater(() -> {
            showLoginScreen();
            loginView.showError(reason);
            Platform.setImplicitExit(true);
        });
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
        Point3D newLoc = new Point3D(oldLoc.getTranslated(dir), oldLoc.getZ());
        game.getPlayer().setLocation(newLoc);
    }

    public void onUpdateRangeChange(int sceneRadius) {
        game.setUpdateRange(sceneRadius);
    }

    public void onReportFPS(long fps) {
        System.out.println(fps);
    }
}
