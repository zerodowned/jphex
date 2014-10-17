package org.solhost.folko.slclient;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.solhost.folko.common.LogFormatter;
import org.solhost.folko.slclient.controllers.MainController;

import javafx.application.Application;
import javafx.stage.Stage;

public class SLClient extends Application {
    private static final Logger log = Logger.getLogger("slclient");

    @Override
    public void start(Stage stage) throws Exception {
        setupLogger(Level.FINEST);

        MainController mainController = new MainController(stage);
        mainController.startGame();

        stage.setTitle("Ultima Online: Shattered Legacy");
        stage.show();
    }

    private void setupLogger(Level level) {
        Handler handler = new ConsoleHandler();
        handler.setLevel(level);
        handler.setFormatter(new LogFormatter());

        log.setUseParentHandlers(false);
        log.addHandler(handler);
        log.setLevel(level);
        log.info("SLClient 0.0.1 starting...");
        log.info("Copyright 2014 by Folke Will");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
