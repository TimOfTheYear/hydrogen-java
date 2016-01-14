// Copyright 2015 Nathan Sizemore <nathanrsizemore@gmail.com>
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.


package com.hydrogen;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;


public class Bstream implements Stream {

    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private FrameState state;
    private ArrayList<Byte> buffer;
    private ArrayList<Byte> scratch;

    public Bstream(Socket socket) throws IOException {
        this.socket = socket;
        this.state = FrameState.Start;
        this.buffer = new ArrayList<>();
        this.scratch = new ArrayList<>();

        try {
            this.inputStream = this.socket.getInputStream();
            this.outputStream = this.socket.getOutputStream();
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public byte[] read() throws ReadException {
        while (true) {
            // Don't kill the cpu
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            final int numRead;
            byte[] buffer = new byte[512];
            try {
                numRead = this.inputStream.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ReadException(ReadException.IO);
            }

            if (numRead == -1) {
                throw new ReadException(ReadException.EOF);
            }

            buffer = this.bufWithScratch(buffer);
            int seek_pos = 0;

            if (this.state == FrameState.Start) {
                seek_pos = this.readForFrameStart(buffer, seek_pos, numRead);
            }

            if (this.state == FrameState.PayloadLen) {
                seek_pos = this.readPayloadLen(buffer, seek_pos, numRead);
            }

            if (this.state == FrameState.Payload) {
                seek_pos = this.readPayload(buffer, seek_pos, numRead);
            }

            if (this.state == FrameState.End) {
                byte[] retBuf = this.readForFrameEnd(buffer, seek_pos, numRead);
                if (retBuf.length > 0) {
                    return retBuf;
                }
            }
        }
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        byte[] frame = Frame.fromBuffer(buffer);
        try {
            this.outputStream.write(frame);
            this.outputStream.flush();
        } catch (IOException e) {
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (!this.socket.isInputShutdown()) {
            this.socket.shutdownInput();
        }
        if (!this.socket.isOutputShutdown()) {
            this.socket.shutdownOutput();
        }
        if (!this.socket.isClosed()) {
            this.socket.close();
        }
    }

    private byte[] bufWithScratch(byte[] buf) {
        byte[] newBuf = new byte[buf.length + this.scratch.size()];
        int x = 0;
        for (x = 0; x < this.scratch.size(); x++) {
            newBuf[x] = (byte)this.scratch.get(x);
        }
        for (int y = 0; y < buf.length; y++) {
            newBuf[y + x] = buf[y];
        }
        return newBuf;
    }

    private int readForFrameStart(byte[] buf, int offset, int len) {
        for (int x = offset; x < len; x++) {
            if (buf[x] == Frame.START) {
                this.buffer.add(buf[x]);
                this.state = FrameState.PayloadLen;
                offset++;
                break;
            }
            offset++;
        }
        return offset;
    }

    private int readPayloadLen(byte[] buf, int offset, int len) {
        for (int x = offset; x < len; x++) {
            this.buffer.add(buf[x]);
            if (this.buffer.size() == 3) {
                this.state = FrameState.Payload;
                offset++;
                break;
            }
            offset++;
        }
        return offset;
    }

    private int readPayload(byte[] buf, int offset, int len) {
        for (int x = offset; x < len; x++) {
            this.buffer.add(buf[x]);
            if (this.buffer.size() == this.payloadLen() + 3) {
                this.state = FrameState.End;
                offset++;
                break;
            }
            offset++;
        }
        return offset;
    }

    private byte[] readForFrameEnd(byte[] buf, int offset, int len) {
        if (offset < len) {
            byte expectedEndByte = buf[offset];
            if (expectedEndByte == Frame.END) {
                byte[] payload = new byte[this.buffer.size() - 3];
                for (int x = 3; x < this.buffer.size(); x++) {
                    payload[x - 3] = this.buffer.get(x);
                }
                this.state = FrameState.Start;
                this.buffer = new ArrayList<>();

                offset++;
                this.scratch = new ArrayList<>();
                for (int x = offset; x < len; x++) {
                    this.scratch.add(buf[x]);
                }
                return payload;
            }

            this.state = FrameState.Start;
            this.buffer = new ArrayList<>();
            this.scratch = new ArrayList<>();
            for (int x = offset; x < len; x++) {
                this.scratch.add(buf[x]);
            }
        }
        return new byte[0];
    }

    private int payloadLen() {
        return 0x00 << 24
                | 0x00 << 16
                | (this.buffer.get(1) & 0xFF) << 8
                | (this.buffer.get(2) & 0xFF);
    }
}
