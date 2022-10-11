package me.awesome.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.awesome.robot.ActivationController;
import me.awesome.server.utils.Colors;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Chat extends WebSocketServer {

    private final HashSet<WebSocket> conns;
    private CountDownLatch pingLatch = new CountDownLatch(1);

    private Thread pingThread;

    private InetSocketAddress socketAddress;

    public Chat(int port) {
        super(new InetSocketAddress(port));
        socketAddress = new InetSocketAddress(port);
        conns = new HashSet<>();
    }

    public Chat(InetSocketAddress address) {
        super(address);
        socketAddress = address;
        conns = new HashSet<>();
    }

    public Chat(int port, Draft_6455 draft) {
        super(new InetSocketAddress(port), Collections.singletonList(draft));
        socketAddress = new InetSocketAddress(port);
        conns = new HashSet<>();
    }



    @Override
    public void onStart() {
        System.out.println(Colors.GREEN + "Instance Started! " + (SSLSocket.useWss ? "wss" : "ws") + "://"+socketAddress.toString().substring(1));
        Colors.flush();

//        new Thread(this::listen, "Command Listener").start();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = conn.getResourceDescriptor().substring(1);
        String[] pairs = query.split("(.*\\?)");
        if (query.equals("") || pairs.length < 2) {
            conn.send("No (No Query)");
            conn.close(4000, "No name parameter specified");
            return;
        }
        pairs = pairs[1].split("&");
        for (String pair : pairs) {
            if (pair.contains("=")) {
                int idx = pair.indexOf("=");
                if (idx < 0)
                    idx = 0;
                query_pairs.put(URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
            }
        }

        if (!Objects.equals(query_pairs.get("password"), SSLSocket.password) || query_pairs.get("name") == null) {
            conn.send("No (Incorrect Password / No name specified)");
            conn.close(4000, "No name parameter specified");
            return;
        }

        conn.setAttachment(new SocketData(query_pairs.get("name")));

        for (WebSocket webSocket : conns) {
            System.out.println("((SocketData) webSocket.getAttachment()).getUsername() = " + ((SocketData) webSocket.getAttachment()).getUsername());
            System.out.println("query_pairs.get(\"name\") = " + query_pairs.get("name"));
            if (Objects.equals(((SocketData) webSocket.getAttachment()).getUsername(), query_pairs.get("name"))) {
                conn.send("No (Name already in use)");
                conn.close(4000, "No name parameter specified");
                return;
            }
        }
//        else {
//            System.out.println("Name: " + query_pairs.get("name"));
//            System.out.println("Password: " + query_pairs.get("password"));
//        }

        conns.add(conn);
        broadcast("Welcome ["+((SocketData) conn.getAttachment()).getUsername()+"] to the server!"); //This method sends a message to the new client
//        broadcast(Arrays.toString(new byte[]{
//                0x01, 0x1D
//        }));
//        broadcast(new byte[]{
//                0x01, 0x1D
//        });

//        broadcast("new connection: " + handshake
//                .getResourceDescriptor()); //This method sends a message to all clients connected
        System.out.println(Colors.BLUE_BRIGHT +"["+ socketAddress.toString() + "]" + Colors.RESET + " " + conn.getRemoteSocketAddress().getAddress().getHostAddress() + " ("+query_pairs.get("name")+") connected!");

//        System.out.println(query_pairs.get("name"));
//        System.out.println(query_pairs.get("id"));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        if (conns.contains(conn)) {
            conns.remove(conn);
            if (conn.getAttachment() != null && conn.getAttachment() instanceof SocketData) {
                broadcast(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " has left the room! (" + ((SocketData) conn.getAttachment()).getUsername() + ")");
                System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " has left the room! (" + ((SocketData) conn.getAttachment()).getUsername() + ")");
            } else if (conn.getAttachment() instanceof String) {
                System.out.println((String) conn.getAttachment());
            } else {
                System.err.println(Colors.BLUE_BRIGHT +"["+ socketAddress.toString() + "] " + Colors.RESET + "Attachment was null when the me.awesome.server.socket closed. Must have been an error");
            }
        } else {
            System.out.println(Colors.BLUE_BRIGHT +"["+ socketAddress.toString() + "] " + Colors.RESET + "Anonymous/Unknown client disconnected");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        if (message.length() > 500) {
            conn.close();
            return;
        }

//        conns.stream().filter(connection -> {
//
//            String name = ((SocketData) conn.getAttachment()).getUsername();
//            String name2 = ((SocketData) connection.getAttachment()).getUsername();
//
//            return !Objects.equals(name, name2);
//        }).forEach(connection -> connection.send(((SocketData) conn.getAttachment()).getUsername() +": "+message));

        if (checkJSON(message)) {
            JsonObject convertedObject = new Gson().fromJson(message, JsonObject.class);


            if (convertedObject.get("command").getAsString().equals("go")) {
                System.out.println(Colors.BLUE_BRIGHT +"["+ socketAddress.toString() + "] " + Colors.GREEN_BOLD_BRIGHT + "GO");
//                new Thread(ActivationController::go).start();
                ActivationController.go();
                return;
            }

        }

        System.out.println((checkJSON(message) ? Colors.GREEN : Colors.RED) + conn.getRemoteSocketAddress() + ": ["+((SocketData) conn.getAttachment()).getUsername()+"] " + message + Colors.RESET);

        switch (message) {
            case "pingme":
                //            conn.sendPing();
                conn.send(new byte[] {
                        0x01,
                });

                new Thread(() -> {
                    try {
                        getPing(conn);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
//            pingThread.start();

                System.out.println("Ping requested!");

                break;
            case "list":
                conns.forEach(connection -> {
                    String name = ((SocketData) connection.getAttachment()).getUsername();
                    conn.send(/*connection.getRemoteSocketAddress() + */
                            " [" + name + "]");
                });
                break;
            case "go":
                System.out.println(Colors.BLUE_BRIGHT +"["+ socketAddress.toString() + "] " + Colors.GREEN_BOLD_BRIGHT + "GO");
                break;
        }

    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        broadcast(message.array());
        System.out.println(conn + ": [0b] " + message);
    }

    @Override
    public void onWebsocketPong(WebSocket conn, Framedata f) {
        if (((SocketData) conn.getAttachment()).isPinging()) {
            conn.setAttachment(((SocketData) conn.getAttachment()).setPinging(false));
            pingLatch.countDown();
            pingLatch = new CountDownLatch(1);
        } else {
//            System.out.println("Server has received a pong message!");
            super.onWebsocketPong(conn, f);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println(Colors.BLUE_BRIGHT +"["+ socketAddress.toString() + "] " + Colors.RESET + "onError() has been invoked");
        ex.printStackTrace();
        if (conn != null) {
            conns.remove(conn);
            System.out.println(Colors.BLUE_BRIGHT +"["+ socketAddress.toString() + "] " + Colors.RESET + "Address associated with error: " + conn.getRemoteSocketAddress());
        }
    }


//    private void listen() {
//        BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
//        while (true) {
//            String in = null;
//            try {
//                in = sysin.readLine();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            assert in != null;
//            switch (in) {
//                case "pingall" -> {
//                    System.out.printf("Pinging all connections... %d\n", conns.size());
//                    for (WebSocket s : conns) {
//                        try {
//                            System.out.println("Ping: " + getPing(s));
//                            System.out.println(s.getRemoteSocketAddress());
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                case "test" -> {
//                    System.out.println("Test command");
//                    conns.stream().filter(conn -> ((
//                                    (me.awesome.server.SocketData) conn.getAttachment())
//                                    .getUsername() != "Awesome"))
//                            .forEach(conn -> conn.send("Hello, there!"));
//                }
//                case "list" -> conns.forEach(conn -> {
//                    String name = ((me.awesome.server.SocketData) conn.getAttachment()).getUsername();
//                    System.out.println(conn.getRemoteSocketAddress() + " [" + name + "]");
//                });
//            }
//        }
//    }

    private short getPing(WebSocket socket) throws InterruptedException {

        socket.setAttachment(((SocketData) socket.getAttachment()).setPinging(true));
        socket.sendPing();
        long time = System.currentTimeMillis();

        if (!pingLatch.await(3, TimeUnit.SECONDS)) {
            return -1;
        }

        return (short) (System.currentTimeMillis() - time);
    }

    private boolean checkJSON(String input) {
        try {
            new JSONObject(input);
        } catch (JSONException ex) {
            try {
                new JSONArray(input);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }


//    public static void main(String[] args) throws InterruptedException, IOException {
//        int port = 8887; // 843 flash policy port
//        try {
//            port = Integer.parseInt(args[0]);
//        } catch (Exception ex) {
//        }
//        GameServer s = new GameServer(port);
//        s.start();
//        System.out.println("GameServer started on port: " + s.getPort());
//
//        BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
//        while (true) {
//            String in = sysin.readLine();
//            s.broadcast(in);
//            if (in.equals("exit")) {
//                s.stop(1000);
//                break;
//            }
//        }
//    }

}
