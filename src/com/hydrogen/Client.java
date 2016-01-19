// Copyright 2015 Nathan Sizemore <nathanrsizemore@gmail.com>
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.


package com.hydrogen;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;


public class Client {

    private Stream stream;
    private IHydrogen implementor;
    private ReaderThread readerThread;
    private WriterThread writerThread;
    private ReconnectThread reconnectThread;

    private String host;
    private int port;
    private boolean usingSSL;
    private boolean usingStrictCATrust;

    private boolean unexpectedDisconnect = false;


    public Client(IHydrogen implementor) {
        this.implementor = implementor;
    }

    public void connectToHost(String host, int port, boolean useSSL, boolean useStrictCATrust) throws Exception {
        if (this.implementor == null) {
            throw new NullPointerException("IHydrogen implementor was null");
        }

        if (useSSL) {
            SSLSocket sslSocket;
            if (useStrictCATrust) {
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
                sslSocket = (SSLSocket)sslSocketFactory.createSocket(host, port);
            } else {
                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[] {
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                            public void checkClientTrusted(
                                    java.security.cert.X509Certificate[] certs, String authType) {
                            }
                            public void checkServerTrusted(
                                    java.security.cert.X509Certificate[] certs, String authType) {
                            }
                        }
                };

                // Install the all-trusting trust manager
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                sslSocket = (SSLSocket)sslSocketFactory.createSocket(host, port);
            }

            sslSocket.startHandshake();
            this.stream = new SecureStream(sslSocket);
        } else {
            Socket socket = new Socket(host, port);
            this.stream = new Bstream(socket);
        }

        this.host = host;
        this.port = port;
        this.usingSSL = useSSL;
        this.usingStrictCATrust = useStrictCATrust;

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
            Thread.sleep(500);
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

    public boolean reconnectToHost(String host, int port) throws IOException, NullPointerException, NoSuchAlgorithmException, KeyManagementException {
        if (this.implementor == null) {
            throw new NullPointerException("IHydrogen implementor was null");
        }

        this.implementor.onReconnectAttempt();

        if (usingSSL) {
            SSLSocket sslSocket;
            if (usingStrictCATrust) {
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
                sslSocket = (SSLSocket)sslSocketFactory.createSocket(host, port);
            } else {
                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[] {
                        new X509TrustManager() {
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                            public void checkClientTrusted(
                                    java.security.cert.X509Certificate[] certs, String authType) {
                            }
                            public void checkServerTrusted(
                                    java.security.cert.X509Certificate[] certs, String authType) {
                            }
                        }
                };

                // Install the all-trusting trust manager
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                sslSocket = (SSLSocket)sslSocketFactory.createSocket(host, port);
            }

            sslSocket.startHandshake();
            this.stream = new SecureStream(sslSocket);
        } else {
            Socket socket = new Socket(host, port);
            this.stream = new Bstream(socket);
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
        private ArrayList<byte[]> txQueue;
        private volatile boolean keepAlive = true;

        private final Object queueLock = new Object();
        private final Object keepAliveLock = new Object();

        @Override
        public void run() {
            txQueue = new ArrayList<>();

            while (true) {
                synchronized (this.keepAliveLock) {
                    if (!this.keepAlive) {
                        return;
                    }
                }

                try {
                    // CPU rest
                    Thread.sleep(10);

                    synchronized (this.queueLock) {
                        if (this.txQueue.size() < 1) {
                            continue;
                        }

                        for (int x = 0; x < this.txQueue.size(); x++) {
                            stream.write(this.txQueue.get(x));
                        }
                        this.txQueue = new ArrayList<>();
                    }
                } catch (InterruptedException e) {
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    errorClose();
                }
            }
        }

        public void write(final byte[] buffer) {
            synchronized (this.queueLock) {
                this.txQueue.add(buffer);
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
                    } catch (Exception e) {
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
