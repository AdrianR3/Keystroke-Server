package me.awesome.server;

import java.util.concurrent.CountDownLatch;

public class SocketData {

    private boolean isPinging = false;
    private CountDownLatch pingLatch = new CountDownLatch(1);

    private String username = "Player";

    public SocketData(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public CountDownLatch getPingLatch() {
        return pingLatch;
    }

    public SocketData setPingLatch(int count) {
        this.pingLatch = new CountDownLatch(count);
        return this;
    }

    public boolean isPinging() {
        return isPinging;
    }

    public SocketData setPinging(boolean pinging) {
        isPinging = pinging;
        return this;
    }

}
