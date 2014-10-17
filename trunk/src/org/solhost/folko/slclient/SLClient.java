package org.solhost.folko.slclient;

import org.solhost.folko.slclient.controllers.LoginController;

import javafx.application.Application;
import javafx.stage.Stage;

public class SLClient extends Application {
    private LoginController loginController;

    @Override
    public void start(Stage stage) throws Exception {
        loginController = new LoginController(stage);
        loginController.showLogin();

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
