package org.solhost.folko.slclient.models;

import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.solhost.folko.uosl.data.SLData;
import org.solhost.folko.uosl.data.SLMap;
import org.solhost.folko.uosl.data.SLStatic;
import org.solhost.folko.uosl.network.packets.LoginPacket;
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
    private int updateRange;
    private Connection connection;

    public GameState() {
        state = new SimpleObjectProperty<GameState.State>(State.DISCONNECTED);
        player = new Player();
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

    public void visitUpdateRange(Consumer<Point2D> c) {
        Point2D center = player.getLocation();
        for(int x = center.getX() - updateRange; x < center.getX() + updateRange; x++) {
            if(x < 0 || x >= SLMap.MAP_WIDTH) {
                continue;
            }
            for(int y = center.getY() - updateRange; y < center.getY() + updateRange; y++) {
                if(y < 0 || y >= SLMap.MAP_HEIGHT) {
                    continue;
                }
                c.accept(new Point2D(x, y));
            }
        }
    }

    private synchronized void onPlayerLocationChange(Point3D oldLoc, Point3D newLoc) {
        // check for objects no longer visible
        for(Iterator<SLObject> it = objectsInRange.values().iterator(); it.hasNext(); ) {
            SLObject obj = it.next();
            if(obj.getLocation().distanceTo(newLoc) > updateRange) {
                it.remove();
            }
        }

        // check for new statics
        visitUpdateRange(point -> {
            for(SLStatic stat : SLData.get().getStatics().getStatics(point)) {
                if(objectsInRange.containsKey(stat.getSerial())) {
                    continue;
                }
                SLItem itm = SLItem.fromStatic(stat);
                objectsInRange.put(itm.getSerial(), itm);
            }
        });
    }
}
