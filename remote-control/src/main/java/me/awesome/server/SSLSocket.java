package me.awesome.server;

import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;

public class SSLSocket {

    /*
     * Keystore with certificate created like so (in JKS format):
     *
     *keytool -genkey -keyalg RSA -validity 3650 -keystore "keystore.jks" -storepass "storepassword" -keypass "keypassword" -alias "default" -dname "CN=127.0.0.1, OU=MyOrgUnit, O=MyOrg, L=MyCity, S=MyRegion, C=MyCountry"
     */

    public static String password;
    public static boolean useWss;
    public static boolean useHeartbeat;

    public SSLSocket(InetSocketAddress address, boolean useWss, boolean useHeartbeat, String password) throws Exception {
        SSLSocket.useHeartbeat = useHeartbeat;
        SSLSocket.password = password;
        SSLSocket.useWss = useWss;
        Chat gameServer = new Chat(address);
        // Firefox does allow multiple ssl connection only via port 443 //tested on FF16
//        old port: 8887

//                SSLUtility.getSSLContext(String.valueOf(System.console().readPassword("Enter certificate password: ")), getClass().getResourceAsStream("/keystore.jks"))
        if (useWss) {
            gameServer.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(
                    SSLUtility.getSSLContext("storepassword", getClass().getResourceAsStream("/keystore.jks"))
            ));
        }
        gameServer.setReuseAddr(true);
        gameServer.setTcpNoDelay(true);
//        Heartbeat every 2 seconds, therefore ensuring, every 5 seconds, all clients are alive
        gameServer.setConnectionLostTimeout(useHeartbeat ? (int) (5 / 2.5) : 0);
        gameServer.start();

    }

    static class SSLUtility {
        private SSLUtility() { throw new UnsupportedOperationException("Class Instantiation not supported"); }

        private static SSLContext getSSLContext(String storePassword, String keyPassword, InputStream keyStream) throws Exception {
            // load up the key store
            String STORETYPE = "JKS";

            KeyStore ks = KeyStore.getInstance(STORETYPE);
            ks.load(keyStream, storePassword.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, storePassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext sslContext;
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext;
        }

        private static SSLContext getSSLContext(String storePassword, InputStream keyStream) throws Exception {
            return getSSLContext(storePassword, storePassword, keyStream);
        }
    }
}