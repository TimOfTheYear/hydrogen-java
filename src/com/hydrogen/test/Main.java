package com.hydrogen.test;

import com.hydrogen.Client;
import com.hydrogen.IHydrogen;

import java.nio.charset.Charset;

public class Main {

    public static void main(String [] args) {
        Client client = new Client(hydrogenImplementor);

        // Connect to host
        try {
            client.connectToHost("127.0.0.1", 1337);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Reconnect timeout for server close testing
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Send a thing
        final String payload = "{\"action\":100,\"version\":\"1.0.0\",\"data\":\"{\\\"id\\\":\\\"123456789\\\"}\"}";
        client.write(payload.getBytes(Charset.forName("UTF-8")));

        // Response timeout to connect message...
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Disconnect from host
        client.close();
    }

    public static IHydrogen hydrogenImplementor = new IHydrogen() {
        public void onConnected() {
            System.out.println("onConnected");
        }

        public void onDisconnected() {
            System.out.println("onDisconnected");
        }

        public void onError(Exception e) {
            System.out.println("onError: " + e.getMessage());
            e.printStackTrace();
        }

        public void onDataReceived(byte[] buffer) {
            System.out.println("onDataReceived");
            System.out.println("Received: " + new String(buffer));
        }
    };
}
