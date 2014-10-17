package org.solhost.folko.slclient.controllers;

import java.io.IOException;
import java.nio.channels.UnresolvedAddressException;
import java.util.logging.Logger;

import org.solhost.folko.slclient.models.Connection;
import org.solhost.folko.slclient.models.Connection.ConnectionHandler;
import org.solhost.folko.slclient.models.GameState;
import org.solhost.folko.slclient.models.Player;
import org.solhost.folko.uosl.network.SendableMobile;
import org.solhost.folko.uosl.network.packets.InitPlayerPacket;
import org.solhost.folko.uosl.network.packets.LocationPacket;
import org.solhost.folko.uosl.network.packets.LoginErrorPacket;
import org.solhost.folko.uosl.network.packets.SLPacket;

public class NetworkController implements ConnectionHandler {
    private static final Logger log = Logger.getLogger("slclient.network");
    private final MainController mainController;
    private final GameState game;
    private Connection connection;

    public NetworkController(MainController mainController) {
        this.mainController = mainController;
        this.game = mainController.getGameState();
    }

    public void tryConnect(String host) {
        try {
            connection = new Connection(this, host);
        } catch (NumberFormatException e) {
            mainController.onNetworkError("Invalid port");
        } catch (UnresolvedAddressException e) {
            mainController.onNetworkError("Unknown host");
        } catch (IllegalArgumentException e) {
            mainController.onNetworkError("Invalid host");
        } catch (IOException e) {
            mainController.onNetworkError(e.getMessage());
        } catch (Exception e) {
            mainController.onNetworkError(e.getMessage());
        }
    }

    public void stopNetwork() {
        log.info("Stopping network");
        connection.disconnect();
        game.onDisconnect();
    }

    @Override
    public void onConnected() {
        game.onConnect(connection);
    }

    @Override
    public void onNetworkError(String reason) {
        log.warning("Network error: " + reason);
        mainController.onNetworkError(reason);
        game.onDisconnect();
    }

    @Override
    public void onRemoteDisconnect() {
        log.warning("Remote disconnected");
        game.onDisconnect();
    }

    private void onLoginFail(LoginErrorPacket packet) {
        String message;
        switch(packet.getReason()) {
        case LoginErrorPacket.REASON_PASSWORD:
            message = "Invalid Password";
            break;
        case LoginErrorPacket.REASON_CHAR_NOT_FOUND:
            message = "Character not found";
            break;
        default:
            message = "Unknown reason";
            break;
        }
        mainController.onLoginFail(message);
    }

    private void onInitPlayer(InitPlayerPacket packet) {
        game.onLoginSuccess();
        game.getPlayer().setSerial(packet.getSerial());
    }

    private void onLocationChange(LocationPacket packet) {
        SendableMobile src = packet.getMobile();
        if(src.getSerial() != game.getPlayer().getSerial()) {
            log.warning("LocationPacket received for non-player object");
            return;
        }
        Player player = game.getPlayer();
        player.setGraphic(src.getGraphic());
        player.setFacing(src.getFacing());
        player.setLocation(src.getLocation());
    }

    @Override
    public void onIncomingPacket(SLPacket packet) {
        log.finest("Incoming packet: " + packet);
        switch(packet.getID()) {
        case LoginErrorPacket.ID:   onLoginFail((LoginErrorPacket) packet); break;
        case InitPlayerPacket.ID:   onInitPlayer((InitPlayerPacket) packet); break;
        case LocationPacket.ID:     onLocationChange((LocationPacket) packet); break;
        }
    }
}
