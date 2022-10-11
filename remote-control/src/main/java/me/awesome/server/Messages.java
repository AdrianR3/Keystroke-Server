package me.awesome.server;

import me.awesome.server.utils.Colors;

public enum Messages {
    ENABLE_DEBUG(Colors.PURPLE_BRIGHT + "Try running with the \""+Colors.YELLOW+"-debug"+Colors.PURPLE_BRIGHT+"\" flag for debug mode"),
    TOO_MANY_SOCKETS("There are more sockets than provided addresses"),
    NOT_ENOUGH_SOCKETS("There must be at least 1 me.awesome.server.socket");


    private final String message;

    Messages(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message + Colors.RESET;
    }
}
