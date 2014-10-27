package org.solhost.folko.slclient.models;

import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.solhost.folko.uosl.network.SendableObject;
import org.solhost.folko.uosl.network.packets.LoginPacket;
import org.solhost.folko.uosl.types.Direction;
import org.solhost.folko.uosl.types.Items;
import org.solhost.folko.uosl.types.Point2D;
import org.solhost.folko.uosl.types.Point3D;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class GameState {
    public enum State {DISCONNECTED, CONNECTED, LOGGED_IN};

    private static final Logger log = Logger.getLogger("slclient.game");
    private final Player player;
    private final Property<State> state;
    private final ObservableMap<Long, SLObject> objectsInRange;
    private int updateRange = 15;
    private Connection connection;

    public GameState() {
        state = new SimpleObjectProperty<GameState.State>(State.DISCONNECTED);
        player = new Player(-1, -1);
        objectsInRange = FXCollections.observableMap(new HashMap<Long, SLObject>());

        player.locationProperty().addListener((Player, oldLoc, newLoc) -> onPlayerLocationChange(oldLoc, newLoc));

        log.fine("Game state initialized");
    }

    public ObservableMap<Long, SLObject> objectsInRangeProperty() {
        return objectsInRange;
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

    public int getUpdateRange() {
        return updateRange;
    }

    public void setUpdateRange(int updateRange) {
        this.updateRange = updateRange;
        onPlayerLocationChange(player.getLocation(), player.getLocation());
    }

    public void forEachObjectAt(Point2D point, Consumer<SLObject> c) {
        for(SLObject obj : objectsInRange.values()) {
            if(point.equals(obj.getLocation())) {
                c.accept(obj);
            }
        }
    }

    private synchronized void onPlayerLocationChange(Point3D oldLoc, Point3D newLoc) {
        checkInvisible();
    }

    private void checkInvisible() {
        // check for objects no longer visible
        Point3D location = player.getLocation();
        for(Iterator<SLObject> it = objectsInRange.values().iterator(); it.hasNext(); ) {
            SLObject obj = it.next();
            if(obj.getLocation().distanceTo(location) > updateRange) {
                it.remove();
            }
        }
    }

    public synchronized void updateOrInitObject(SendableObject object, Direction facing, int amount) {
        // valid in object: serial, graphic, location, hue
        SLObject updatedObj = objectsInRange.get(object.getSerial());
        if(updatedObj == null) {
            // init new object
            if(object.getSerial() >= Items.SERIAL_FIRST) {
                updatedObj = new SLItem(object.getSerial(), object.getGraphic());
            } else {
                updatedObj = new SLMobile(object.getSerial(), object.getGraphic());
            }
            objectsInRange.put(updatedObj.getSerial(), updatedObj);
        }
        updatedObj.setGraphic(object.getGraphic());
        updatedObj.setLocation(object.getLocation());
        updatedObj.setHue(object.getHue());
        if(updatedObj instanceof SLItem) {
            ((SLItem) updatedObj).setAmount(amount);
        } else if(updatedObj instanceof SLMobile) {
            ((SLMobile) updatedObj).setFacing(facing);
        }
        checkInvisible();
    }
}
