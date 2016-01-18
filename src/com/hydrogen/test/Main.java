// Copyright 2015 Nathan Sizemore <nathanrsizemore@gmail.com>
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.

package com.hydrogen.test;

import com.hydrogen.Client;
import com.hydrogen.IHydrogen;

import java.nio.charset.Charset;
import java.security.KeyStore;

public class Main {

    public static void main(String [] args) {
        Client client = new Client(hydrogenImplementor);

        try {
            client.connectToHost("realtime.heystaxapp.com", 1338, true, false);

            Thread.sleep(1000);

            // Send a thing
            final String payload = "{\"action\":100,\"version\":\"1.0.0\",\"data\":\"{\\\"id\\\":\\\"123456789\\\"}\"}";
            client.write(payload.getBytes(Charset.forName("UTF-8")));

            // Disconnect from host
            client.close();
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
