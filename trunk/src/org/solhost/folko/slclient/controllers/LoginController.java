package org.solhost.folko.slclient.controllers;

import java.io.IOException;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.solhost.folko.slclient.models.Connection;
import org.solhost.folko.slclient.models.Connection.ConnectionHandler;
import org.solhost.folko.slclient.views.LoginView;
import org.solhost.folko.uosl.network.packets.LoginPacket;
import org.solhost.folko.uosl.network.packets.SLPacket;

public class LoginController implements ConnectionHandler {
   private final Stage stage;
   private final LoginView loginView;
   private Connection connection;
   private String name, password;

    public LoginController(Stage stage) {
        this.stage = stage;
        this.loginView = new LoginView(this);
    }

    public void showLogin() {
        stage.setScene(loginView.getScene());
    }

    public void onLogin(String host, String name, String password) {
        this.name = name;
        this.password = password;

        loginView.setBusy(true);
        try {
            connection = new Connection(this, host);
        } catch (IllegalArgumentException e) {
            loginView.showError("Invalid server");
            loginView.setBusy(false);
            return;
        } catch (IOException e) {
            loginView.showError("Couldn't connect: " + e);
            loginView.setBusy(false);
            return;
        }
    }

    @Override
    public void onConnected() {
        SLPacket login = new LoginPacket();
        connection.sendPacket(login);
    }

    @Override
    public void onError(String reason) {
        Platform.runLater(() -> {
            loginView.showError("Error: " + reason);
            loginView.setBusy(false);
            connection.disconnect();
        });
        return;
    }

    @Override
    public void onIncomingPacket(SLPacket packet) {
        System.out.println("got packet: " + packet.toString());
    }

    @Override
    public void onRemoteDisconnect() {
        Platform.runLater(() -> {
            loginView.showError("Kicked by server");
            loginView.setBusy(false);
        });
    }
}
