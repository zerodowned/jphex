package org.solhost.folko.slclient.models;

public class Player extends SLMobile {
    private String password;

    public Player(long serial, int graphic) {
        super(serial, graphic);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
