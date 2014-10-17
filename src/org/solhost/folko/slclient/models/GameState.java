package org.solhost.folko.slclient.models;

import java.util.logging.Logger;

import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.network.packets.LoginPacket;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;

public class GameState {
    public enum State {DISCONNECTED, CONNECTED, LOGGED_IN};

    private static final Logger log = Logger.getLogger("slclient.game");
    private final SLData data;
    private final Player player;
    private final Property<State> state;
    private Connection connection;

    public GameState() {
        state = new SimpleObjectProperty<GameState.State>(State.DISCONNECTED);
        player = new Player();
        data = SLData.get();
        log.fine("Game initialized");
    }

    public ReadOnlyProperty<State> stateProperty() {
        return state;
    }

    public State getState() {
        return state.getValue();
    }

    public Player getPlayer() {
        return player;
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
        login.setName(player.getName());
        login.setPassword(player.getPassword());
        login.setSeed(LoginPacket.LOGIN_BY_NAME);
        login.setSerial(LoginPacket.LOGIN_BY_NAME);
        login.prepareSend();
        connection.sendPacket(login);
    }

    public void onLoginSuccess() {
        state.setValue(State.LOGGED_IN);
    }
}
