package me.awesome.server;

import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class SSLSocket {

    /*
     * Keystore with certificate created like so (in JKS format):
     *
     *keytool -genkey -keyalg RSA -validity 3650 -keystore "keystore.jks" -storepass "storepassword" -keypass "keypassword" -alias "default" -dname "CN=127.0.0.1, OU=MyOrgUnit, O=MyOrg, L=MyCity, S=MyRegion, C=MyCountry"
     */

    public static String password;
    public static boolean useWss;
    public static boolean useHeartbeat;

    public SSLSocket(InetSocketAddress address, boolean useWss, boolean useHeartbeat, String password, String storePassword) throws Exception {
        SSLSocket.useHeartbeat = useHeartbeat;
        SSLSocket.password = password;
        SSLSocket.useWss = useWss;

        Chat gameServer = new Chat(address);
        // Firefox does allow multiple ssl connection only via port 443 //tested on FF16
//        old port: 8887

//                SSLUtility.getSSLContext(String.valueOf(System.console().readPassword("Enter certificate password: ")), getClass().getResourceAsStream("/keystore.jks"))
        if (useWss) {
            try {
                gameServer.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(
                        SSLUtility.getSSLContext("".equals(storePassword) ? (System.console() != null ? String.valueOf(System.console().readPassword("Enter certificate password: ")) : null) : storePassword, getClass().getResourceAsStream("/keystore.jks"))
                ));
            } catch (java.lang.IllegalArgumentException e) {
                System.err.println("IllegalArgumentException: Shutting down this thread");
                Thread.currentThread().interrupt();
                return;
            }
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
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            try {
                ks.load(keyStream, storePassword.toCharArray());
                kmf.init(ks, storePassword.toCharArray());
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
                throw new RuntimeException(e);
            } catch (NullPointerException e) {
                System.err.println("NullPointerException: This is fatal. \nCrashing by using invalid password...");
                return getSSLContext(storePassword == null ? "null" : storePassword, keyPassword == null ? "null" : keyPassword, keyStream);
            } catch (IOException e) {
//                e.printStackTrace();
                return null;
            }
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