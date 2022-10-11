package me.awesome.server.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Settings {

    private InetSocketAddress address;

    private int port = 53333;
    private String host = InetAddress.getLoopbackAddress().getHostName();

    {
        try (Socket socket = new Socket();) {
            socket.connect(new InetSocketAddress("google.com", 80));
            host = socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {e.printStackTrace();}
    }

    public Settings() {
        build();
    }
    public Settings(InetSocketAddress address) {
        this.address = address;
    }


    public Settings build() {
        this.address = new InetSocketAddress(host, port);
        return this;
    }


    public InetSocketAddress getAddress() {
        return address;
    }

    public Settings address(InetSocketAddress address) {
        this.address = address;
        return this;
    }


    public int getPort() {
        return port;
    }

    public Settings port(int port) {
        this.port = port;
        return this;
    }


    public String getHost() {
        return host;
    }

    public Settings setHost(String host) {
        this.host = host;
        return this;
    }

}
