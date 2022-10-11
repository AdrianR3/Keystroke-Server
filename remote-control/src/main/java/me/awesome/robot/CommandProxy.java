package me.awesome.robot;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.function.Consumer;

public class CommandProxy {

    Data data = new Data();
    int counter = 0;

    public CommandProxy() {

    }

    Consumer<WebSocket> activate() {

        return (socket) -> {

            Gson gson = new Gson();
            data.increment();

            String json = gson.toJson(data);

            System.out.println(json);

//            socket.send(json);
        };
    }

    public static void main(String[] args) {
        new CommandProxy().activate().accept(null);
    }
}

class Data {
    int counter = 0;

    synchronized int increment() {
        counter++;
        return counter;
    }
}
