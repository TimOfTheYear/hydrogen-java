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

    private final byte START = (byte)0x01;
    private final byte END = (byte)0x17;

    private Socket stream;
    private InputStream inputStream;
    private OutputStream outputStream;

    private com.hydrogen.ReadState state;
    private ArrayList<Byte> buffer;
    private ArrayList<Byte> scratch;

    public Bstream(Socket socket) throws com.hydrogen.ReadException, com.hydrogen.WriteException {
        this.buffer = new ArrayList<>();
        this.scratch = new ArrayList<>();
        this.state = com.hydrogen.ReadState.Start;
        this.stream = socket;

        try {
            this.inputStream = this.stream.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            throw new com.hydrogen.ReadException("Error getting input stream");
        }

        try {
            outputStream = this.stream.getOutputStream();
        } catch (IOException e) {
            throw new com.hydrogen.WriteException("Error getting output stream");
        }
    }

    public byte[] read() throws com.hydrogen.ReadException {
        while (true) {
            // Don't want to crush our cpu
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int bytesAvail = 0;
            try {
                bytesAvail = this.inputStream.available();
                if (bytesAvail < 1) {
                    continue;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            final int numRead;
            byte[] buffer = new byte[bytesAvail];
            try {
                numRead = this.inputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                throw new com.hydrogen.ReadException(com.hydrogen.ReadException.IO);
            }

            // EOF
            if (numRead == -1) {
                throw new com.hydrogen.ReadException(com.hydrogen.ReadException.EOF);
            }

            // Add scratch to buffer
            buffer = this.buf_with_scratch(buffer);
            int seek_pos = 0;

            if (this.state == com.hydrogen.ReadState.Start) {
                seek_pos = this.read_for_frame_start(buffer, seek_pos, numRead);
            }

            if (this.state == com.hydrogen.ReadState.PayloadLen) {
                seek_pos = this.read_payload_len(buffer, seek_pos, numRead);
            }

            if (this.state == com.hydrogen.ReadState.Payload) {
                seek_pos = this.read_payload(buffer, seek_pos, numRead);
            }

            if (this.state == com.hydrogen.ReadState.End) {
                byte[] retBuf = this.read_for_frame_end(buffer, seek_pos, numRead);
                if (retBuf.length > 0) {
                    return retBuf;
                }
            }
        }
    }

    public void write(byte[] buffer) throws com.hydrogen.WriteException {
        byte[] nBuffer = new byte[buffer.length + 4];
        nBuffer[0] = START;
        nBuffer[1] = (byte)(buffer.length >> 8);
        nBuffer[2] = (byte)buffer.length;
        for (int x = 0; x < buffer.length; x++) {
            System.out.println((char)buffer[x]);
            nBuffer[x + 2] = buffer[x];
        }
        nBuffer[nBuffer.length - 1] = END;

        try {
            this.outputStream.write(nBuffer);
        } catch (IOException e) {
            e.printStackTrace();
            throw new com.hydrogen.WriteException("Error writing to output stream");
        }

        try {
            this.outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new com.hydrogen.WriteException("Error flushing output stream");
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

    private byte[] buf_with_scratch(byte[] buf) {
        byte[] newBuf = new byte[buf.length + this.scratch.size()];
        int x = 0;
        for (x = 0; x < this.scratch.size(); x++) {
            newBuf[x] = this.scratch.get(x);
        }
        for (int y = 0; y < buf.length; y++) {
            newBuf[y + x] = buf[y];
        }
        return newBuf;
    }

    private int read_for_frame_start(byte[] buf, int offset, int len) {
        for (int x = offset; x < len; x++) {
            if (buf[x] == START) {
                this.buffer.add(buf[x]);
                this.state = com.hydrogen.ReadState.PayloadLen;
                offset++;
                break;
            }
            offset++;
        }
        return offset;
    }

    private int read_payload_len(byte[] buf, int offset, int len) {
        for (int x = offset; x < len; x++) {
            this.buffer.add(buf[x]);
            if (this.buffer.size() == 3) {
                this.state = com.hydrogen.ReadState.Payload;
                offset++;
                break;
            }
            offset++;
        }
        return offset;
    }

    private int read_payload(byte[] buf, int offset, int len) {
        for (int x = offset; x < len; x++) {
            this.buffer.add(buf[x]);
            if (this.buffer.size() == this.payload_length() + 3) {
                this.state = com.hydrogen.ReadState.End;
                offset++;
                break;
            }
            offset++;
        }
        return offset;
    }

    private byte[] read_for_frame_end(byte[] buf, int offset, int len) {
        if (offset < len) {
            byte expectedEndByte = buf[offset];
            if (expectedEndByte == END) {
                byte[] payload = new byte[this.buffer.size() - 3];
                for (int x = 3; x < this.buffer.size(); x++) {
                    payload[x - 3] = this.buffer.get(x);
                }
                this.state = com.hydrogen.ReadState.Start;
                this.buffer = new ArrayList<>();

                offset++;
                this.scratch = new ArrayList<>();
                for (int x = offset; x < len; x++) {
                    this.scratch.add(buf[x]);
                }
                return payload;
            }

            this.state = com.hydrogen.ReadState.Start;
            this.buffer = new ArrayList<>();
            this.scratch = new ArrayList<>();
            for (int x = offset; x < len; x++) {
                this.scratch.add(buf[x]);
            }
        }
        return new byte[0];
    }

    private int payload_length() {
        final int mask = 0xFFFF;
        int len = (this.buffer.get(1) << 8) & mask;
        len = len | this.buffer.get(2);
        return len;
    }
}
