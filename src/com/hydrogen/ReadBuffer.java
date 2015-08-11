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

import java.util.Vector;

public class ReadBuffer {
    // Current message
    private Message cMessage;
    // Current bytes remaining for next read
    private int cRemaining;
    // Current buffer
    private Vector<Byte> cBuffer;
    // Queue of messages created during last read
    private Vector<Message> queue;


    public ReadBuffer() {
        this.cMessage = new Message();
        this.cRemaining = 2;
        this.cBuffer = new Vector<>(this.cRemaining);
        this.queue = new Vector<>();
    }

    public int remaining() {
        return this.cRemaining;
    }

    public void push(byte elem) {
        this.cBuffer.add(elem);
        this.cRemaining--;
    }

    public void setCapacity(int size) {
        this.cRemaining = size;
        this.cBuffer = new Vector<>(this.cRemaining);
    }

    public void calcPayloadLen() {
        int len = 0;
        len = len | this.cBuffer.get(0);
        len = (len << 8) | this.cBuffer.get(1);
        this.cMessage.len = len;
    }

    public int payloadLen() {
        return this.cMessage.len;
    }

    public void reset() {
        this.cMessage.payload = (Vector) this.cBuffer.clone();
        this.queue.add(this.cMessage.clone());
        this.cMessage = new Message();
        this.cRemaining = 2;
        this.cBuffer = new Vector<>(this.cRemaining);
    }

    public int queueLen() {
        return this.queue.size();
    }

    public Vector<Message> queueAsMut() {
        return this.queue;
    }

    public Vector<Vector<Byte>> drainQueue() {
        Vector<Vector<Byte>> buffer = new Vector<>(this.queue.size());
        for (int x = 0; x < this.queue.size(); x++) {
            Vector<Byte> payload = (Vector) this.queue.get(x).payload.clone();
            buffer.add(payload);
        }
        this.queue = new Vector<>();
        return buffer;
    }
}
