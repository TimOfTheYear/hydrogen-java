/*
 * Copyright 2015 Nathan Sizemore <nathanrsizemore@gmail.com>
 *
 * This Source Code Form is subject to the
 * terms of the Mozilla Public License, v.
 * 2.0. If a copy of the MPL was not
 * distributed with this file, You can
 * obtain one at
 * http://mozilla.org/MPL/2.0/.
 */

package com.hydrogen;


import java.io.IOException;
import java.net.Socket;

public class Client {

    private com.hydrogen.Bstream stream;
    private ReaderThread readerThread;
    private com.hydrogen.IHydrogen implementor;

    public Client(com.hydrogen.IHydrogen implementor) {
        this.implementor = implementor;
    }

    public void finalize() {
        cleanUpReaderThread();
        cleanUpStream();
    }

    public void connectToHost(String host, int port) throws IOException, NullPointerException {
        Socket socket;
        try {
            socket = new Socket(host, port);
        } catch (IOException e) {
            throw e;
        }

        try {
            this.stream = new com.hydrogen.Bstream(socket);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }

        this.readerThread = new ReaderThread();
        this.readerThread.start();

        if (this.implementor == null) {
            this.readerThread.cancelThread();
            throw new NullPointerException("IHydrogen implementor was null");
        }
        this.implementor.onConnected();
    }

    public void write(final byte[] buffer) {
        new Thread() {
            @Override
            public void run() {
                try {
                    stream.write(buffer);
                } catch (com.hydrogen.WriteException e) {
                    implementor.onError(e);
                    disconnect();
                }
            }
        }.start();
    }

    public void disconnect() {
        this.readerThread.cancelThread();
        try {
            this.stream.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.implementor.onDisconnected();
    }

    private void cleanUpReaderThread() {
        if (this.readerThread != null) {
            this.readerThread.cancelThread();
        }
    }

    private void cleanUpStream() {
        try {
            this.stream.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ReaderThread extends Thread {
        private volatile boolean keepAlive = true;

        @Override
        public void run() {
            while (keepAlive) {
                try {
                    byte[] buffer = stream.read();
                    implementor.onDataReceived(buffer);
                } catch (com.hydrogen.ReadException e) {
                    implementor.onError(e);
                    this.keepAlive = false;
                    return;
                }
            }
            disconnect();
        }

        public void cancelThread() {
            this.keepAlive = false;
        }
    }
}
