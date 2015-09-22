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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Bstream {

    private ReadState state;
    private Socket stream;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ReadBuffer buffer;

    public Bstream(Socket socket) throws ReadException, WriteException {
        this.state = ReadState.PayloadLen;
        this.stream = socket;
        this.buffer = new ReadBuffer();

        try {
            this.inputStream = this.stream.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ReadException("Error getting input stream");
        }

        try {
            outputStream = this.stream.getOutputStream();
        } catch (IOException e) {
            throw new WriteException("Error getting output stream");
        }
    }

    public byte[] read() throws ReadException {
        while (true) {
            try {
                final int bytes_avail = this.inputStream.available();
                if (bytes_avail < 1) {
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            int numRead = 0;
            int count = this.buffer.remaining();
            byte[] buffer = new byte[count];
            try {
                numRead = this.inputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ReadException(ReadException.IO);
            }

            // EOF
            if (numRead == -1) {
                throw new ReadException(ReadException.EOF);
            }

            for (int x = 0; x < numRead; x++) {
                this.buffer.push(buffer[x]);
            }

            if (this.buffer.remaining() == 0) {
                if (this.state == ReadState.PayloadLen) {
                    this.buffer.calcPayloadLen();
                    int pLen = this.buffer.payloadLen();
                    this.buffer.setCapacity(pLen);
                    this.state = ReadState.Payload;
                } else {
                    this.buffer.reset();
                    this.state = ReadState.PayloadLen;
                    break;
                }
            }
        }

        ArrayList<ArrayList<Byte>> buffer = this.buffer.drainQueue();
        if (buffer.size() != 1) {
            throw new ReadException(ReadException.BUF);
        }

        byte[] rBuffer = new byte[buffer.get(0).size()];
        ArrayList<Byte> tBuffer = buffer.get(0);
        for (int x = 0; x < rBuffer.length; x++) {
            Byte tByte = tBuffer.get(x);
            if (tByte == null)
            {
                throw new ReadException(ReadException.NULLVAL);
            }
            rBuffer[x] = (byte)tByte;
        }


        return rBuffer;
    }

    public void write(byte[] buffer) throws WriteException {
        byte[] nBuffer = new byte[buffer.length + 2];
        nBuffer[0] = (byte)(buffer.length >> 8);
        nBuffer[1] = (byte)buffer.length;


        for (int x = 0; x < buffer.length; x++) {
            System.out.println((char)buffer[x]);
            nBuffer[x + 2] = buffer[x];
        }

        try {
            this.outputStream.write(nBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            throw new WriteException("Error writing to output stream");
        }

        try {
            this.outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new WriteException("Error flushing output stream");
        }
    }

    public void cleanup() throws IOException {
        if (!this.stream.isInputShutdown()) {
            if (!this.stream.isClosed()) {
                this.stream.shutdownInput();
                this.inputStream.close();
            }
        }

        if (!this.stream.isOutputShutdown()) {
            if (!this.stream.isClosed()) {
                this.stream.shutdownOutput();
                this.outputStream.close();
            }
        }

        if (!this.stream.isClosed()) {
            this.stream.close();
        }
    }
}
