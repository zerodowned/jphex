package org.solhost.folko.slclient.models;

import java.util.logging.Logger;

import org.solhost.folko.uosl.network.packets.LoginPacket;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;

public class GameState {
    public enum State {DISCONNECTED, CONNECTED, LOGGED_IN};

    private static final Logger log = Logger.getLogger("slclient.game");
    private Connection connection;
    private long playerSerial;
    private String name, password;
    private final Property<State> state;

    public GameState() {
        state = new SimpleObjectProperty<GameState.State>(State.DISCONNECTED);
    }

    public ReadOnlyProperty<State> stateProperty() {
        return state;
    }

    public State getState() {
        return state.getValue();
    }

    public void setLoginData(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public void setPlayerSerial(long serial) {
        log.fine(String.format("Our serial: %08X", serial));
        this.playerSerial = serial;
    }

    public long getPlayerSerial() {
        return playerSerial;
    }

    public void onConnect(Connection connection) {
        this.connection = connection;
        state.setValue(State.CONNECTED);
    }

    public void onDisconnect() {
        this.connection = null;
        state.setValue(State.DISCONNECTED);
    }

    public void tryLogin() {
        LoginPacket login = new LoginPacket();
        login.setName(name);
        login.setPassword(password);
        login.setSeed(LoginPacket.LOGIN_BY_NAME);
        login.setSerial(LoginPacket.LOGIN_BY_NAME);
        login.prepareSend();
        connection.sendPacket(login);
    }

    public void onLoginSuccess() {
        state.setValue(State.LOGGED_IN);
    }
}
