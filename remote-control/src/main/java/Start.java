import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import me.awesome.server.Messages;
import me.awesome.server.SSLSocket;
import me.awesome.server.utils.Colors;

import java.io.IOException;
import java.io.Reader;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Start {

    static boolean debug = false;

    static ArrayList<SSLSocket> socketHashMap = new ArrayList<>();
    static HashMap<String, Object> configMap = new HashMap<>();

    static String password = "";
    static String storePassword = "";
    public static boolean useWss = true;
    public static boolean useHeartbeat = true;
    static int amount = 3;
    static InetSocketAddress defaultAddr;

    static ArrayList<InetSocketAddress> addressArrayList = new ArrayList<>(amount);

    public static void main(String[] args) {

        if (Arrays.asList(args).contains("debug")) {
            System.out.println(Colors.GREEN_BRIGHT + "Debug Mode Enabled" + Colors.RESET);
            debug = true;
        }

        try {
            // create Gson instance
            Gson gson = new Gson();

            // create a reader
            Reader reader = Files.newBufferedReader(Paths.get(Objects.requireNonNull(Start.class.getResource("config.json")).toURI()));

            // convert JSON file to map
            Map<?, ?> map = gson.fromJson(reader, Map.class);

            // print map entries
            for (Map.Entry<?, ?> entry : map.entrySet()) {
//                System.out.println(entry.getKey() + "=" + entry.getValue());
                configMap.put(entry.getKey().toString(), entry.getValue());
            }

            configMap.forEach((index, element) -> {

//                Print out json file's contents
//                System.out.println("Index: "+index+" | Element: "+element);

                switch (index) {
                    case "password" -> password = element.toString();
                    case "storePassword" -> storePassword = element.toString();
                    case "use_wss" -> useWss = Boolean.parseBoolean(element.toString());
                    case "use_heartbeat" -> useHeartbeat = Boolean.parseBoolean(element.toString());
                    case "config" -> ((LinkedTreeMap<?, ?>) element).forEach((configIndex, configElement) -> {

//                        Print out config object's contents
                        System.out.println(Colors.WHITE_BOLD + configIndex+": "+configElement);
                        Colors.flush(System.out);

                        switch (configIndex.toString()) {
                            case "amount" -> amount = Integer.parseInt(configElement.toString());
                            case "defaultAddress" -> {

//                                Disable default address parsing
//                                if (true) break;

                                String s = configElement.toString();

                                if (s.contains("LOCAL")) {
                                    s = s.replace("LOCAL", retrieveLocalIP());
                                } else if (s.contains("localhost")) {
                                    try {
                                        s = s.replace("localhost", InetAddress.getLocalHost().getHostAddress());
                                    } catch (UnknownHostException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                System.out.println("Default IP Address: " + s);
//
                                try {
                                    // WORKAROUND: add any scheme to make the resulting URI valid.
                                    URI uri = new URI("wss://" + s); // may throw URISyntaxException
                                    String host = uri.getHost();
                                    int port = uri.getPort();

                                    if (uri.getHost() == null || uri.getPort() == -1) {
                                        throw new URISyntaxException(uri.toString(),
                                                "URI must have host and port parts");
                                    }

                                    // here, additional checks can be performed, such as
                                    // presence of path, query, fragment, ...

                                    // parsing succeeded
                                    defaultAddr = new InetSocketAddress(host, port);

                                } catch (URISyntaxException ex) {
                                    // validation failed
                                    System.err.println("Failed to parse default address: " + (!debug ? "(" + Messages.ENABLE_DEBUG + ")" : ""));
                                    if (debug) {ex.printStackTrace();}
                                }

                            }
                            case "bindAddresses" -> {
                                String s = configElement.toString();
                                s = s.substring(1, s.length() - 1)
                                        .replace(",", "");

                                String[] addresses = s.split(" ");

//                                Make sure that a valid amount of sockets is requested
                                if (addresses.length < amount || amount < 1) {
                                    throw new IllegalArgumentException(!(amount < 1) ? Messages.TOO_MANY_SOCKETS.toString() : Messages.NOT_ENOUGH_SOCKETS.toString());
                                }

//                                Loop through list of supplied addresses (up to requested amount) and process them to be added to the ArrayList
                                for (int i = 0; i < amount; i++) {

                                    String string = addresses[i];

//                                    Replace "LOCAL" with local ip address, which is calculated by connecting to 8.8.8.8

                                    if (string.contains("LOCAL")) {
                                        string = string.replace("LOCAL", retrieveLocalIP());
                                    } else if (string.contains("localhost")) {
                                        try {
                                            string = string.replace("localhost", InetAddress.getLocalHost().getHostAddress());
                                        } catch (UnknownHostException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }

//                                    Parse address into InetSocketAddress & add it to ArrayList

                                    try {
                                        URI uri = new URI("wss://" + string); // may throw URISyntaxException
                                        String host = uri.getHost();
                                        int port = uri.getPort();

                                        if (uri.getHost() == null || uri.getPort() == -1) {
                                            throw new URISyntaxException(uri.toString(),
                                                    "URI must have host and port parts");
                                        }
                                        // validation succeeded
                                        addressArrayList.add(new InetSocketAddress(host, port));
//                                        System.out.println("Added to ArrayList");

                                    } catch (URISyntaxException ex) {
                                        // validation failed
                                        System.err.println("Failed to parse default address: " + (!debug ? "(" + Messages.ENABLE_DEBUG + ")" : ""));
                                        if (debug) {ex.printStackTrace();}
                                    }

//                                    System.out.println(string);

                                }

                            }
                        }
                    });
                }
            });

            // close reader
            reader.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

//        Json has finished reading

        for (InetSocketAddress socketAddress : addressArrayList) {
//            System.out.println("Bind Server: " + socketAddress.getAddress().getHostAddress()+":"+socketAddress.getPort());

            new Thread(() -> {
                try {
                    socketHashMap.add(new SSLSocket(socketAddress, useWss, useHeartbeat, password, storePassword));
                    Thread.currentThread().setName(String.valueOf(socketHashMap.size()));
                } catch (Exception e) {
                    if (e instanceof IOException) {
                        e.printStackTrace();
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }).start();

        }

    }

    private static String localIP = null;
    private static String retrieveLocalIP() {

        if (localIP == null) {

            System.out.println(Colors.GREEN + "Retrieving Local IP Address..." + Colors.RESET);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.connect(new InetSocketAddress("8.8.8.8", 443));
                localIP = socket.getLocalAddress().getHostAddress();

                System.out.println(Colors.YELLOW_UNDERLINED + localIP + Colors.RESET);
                return localIP;
            } catch (ConnectException e) {
                System.err.println("Timed out?" + (!debug ? "(" + Messages.ENABLE_DEBUG + ")" : ""));
                if (debug) {
                    e.printStackTrace();
                }
            } catch (SocketException e) {
                System.err.println("Socket Exception" + (!debug ? "(" + Messages.ENABLE_DEBUG + ")" : ""));
                throw new RuntimeException(e);
            }

        } else {
            return localIP;
        }

        return localIP;
    }

}

