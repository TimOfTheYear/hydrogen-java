// Copyright 2015 Nathan Sizemore <nathanrsizemore@gmail.com>
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.


package com.hydrogen;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Client {

    private Stream stream;
    private IHydrogen implementor;
    private ReaderThread readerThread;
    private WriterThread writerThread;
    private ReconnectThread reconnectThread;

    private String host;
    private int port;

    private boolean unexpectedDisconnect = false;


    public Client(IHydrogen implementor) {
        this.implementor = implementor;
    }

    public void connectToHost(String host, int port, boolean useSSL) throws Exception {
        if (this.implementor == null) {
            throw new NullPointerException("IHydrogen implementor was null");
        }

        if (useSSL) {
            throw new Exception("SSL not yet available");
        } else {
            try {
                Socket socket = new Socket(host, port);
                this.stream = new Bstream(socket);
            } catch (Exception e) {
                throw e;
            }
        }

        this.host = host;
        this.port = port;

        this.readerThread = new ReaderThread();
        this.readerThread.start();

        this.writerThread = new WriterThread();
        this.writerThread.start();

        this.reconnectThread = new ReconnectThread();
        this.reconnectThread.start();

        this.unexpectedDisconnect = false;

        this.implementor.onConnected();
    }

    public void write(final byte[] buffer) {
        if (buffer == null || buffer.length < 1) {
            return;
        }

        this.writerThread.write(buffer);
    }

    public void close() {
        this.reconnectThread.close();

        // Allow time for reconnectThread to stop
        // Shutting down the stream during a read operation will
        // trigger an exception, which will in turn set a flag that
        // the stream has been disconnected unexpectedly, which will
        // trigger a reconnect loop
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            // Do nothing
        }

        this.readerThread.close();
        this.writerThread.close();

        try {
            this.stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.implementor.onDisconnected();
    }

    public boolean reconnectToHost(String host, int port) throws IOException, NullPointerException {
        System.out.println("Attempting reconnect...");
        if (this.implementor == null) {
            throw new NullPointerException("IHydrogen implementor was null");
        }

        Socket socket;
        try {
            socket = new Socket(host, port);
            this.stream = new Bstream(socket);
        } catch (Exception e) {
            return false;
        }

        this.host = host;
        this.port = port;

        this.readerThread = new ReaderThread();
        this.readerThread.start();

        this.writerThread = new WriterThread();
        this.writerThread.start();

        this.unexpectedDisconnect = false;

        this.implementor.onConnected();

        return true;
    }

    private void errorClose() {
        this.readerThread.close();
        this.writerThread.close();
        try {
            this.stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.unexpectedDisconnect = true;
    }

    private class ReaderThread extends Thread {
        private volatile boolean keepAlive = true;

        @Override
        public void run() {
            while (keepAlive) {
                try {
                    byte[] buffer = stream.read();
                    implementor.onDataReceived(buffer);
                } catch (ReadException e) {
                    e.printStackTrace();
                    errorClose();
                }
            }
        }

        public void close() {
            this.keepAlive = false;
        }
    }

    private class WriterThread extends Thread {
        private volatile boolean keepAlive = true;

        private ArrayList<byte[]> queue;
        private final Lock queueLock = new ReentrantLock();

        @Override
        public void run() {
            queue = new ArrayList<>();
            while (keepAlive) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    continue;
                }

                if (this.queue.size() < 1) {
                    continue;
                }

                this.queueLock.lock();
                for (int x = 0; x < this.queue.size(); x++) {
                    try {
                        stream.write(this.queue.get(x));
                    } catch (IOException e) {
                        e.printStackTrace();
                        errorClose();
                    }
                }
                this.queue = new ArrayList<>();
                this.queueLock.unlock();
            }
        }

        public void write(final byte[] buffer) {
            try {
                this.queueLock.lock();
                this.queue.add(buffer);
                this.queueLock.unlock();
            } catch (Exception e) {
                throw e;
            }
        }

        public void close() {
            this.keepAlive = false;
        }
    }

    private class ReconnectThread extends Thread {
        private volatile boolean keepAlive = true;
        int numReconnectTries = 0;

        @Override
        public void run() {
            while (this.keepAlive) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (unexpectedDisconnect) {
                    try {
                        if (!reconnectToHost(host, port)) {
                            numReconnectTries++;
                            Thread.sleep(1000 * numReconnectTries);
                        } else {
                            numReconnectTries = 0;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (numReconnectTries > 3) {
                        close();
                        implementor.onError(new Exception("Unable to reconnect..."));
                        return;
                    }
                }
            }
        }

        public void close() {
            this.keepAlive = false;
        }
    }
}
