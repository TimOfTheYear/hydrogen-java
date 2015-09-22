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

import java.util.ArrayList;

public class ReadBuffer {
    // Current message
    private Message cMessage;
    // Current bytes remaining for next read
    private int cRemaining;
    // Current buffer
    private ArrayList<Byte> cBuffer;
    // Queue of messages created during last read
    private ArrayList<Message> queue;


    public ReadBuffer() {
        this.cMessage = new Message();
        this.cRemaining = 2;
        this.cBuffer = new ArrayList<>(this.cRemaining);
        this.queue = new ArrayList<>();
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
        this.cBuffer = new ArrayList<>(this.cRemaining);
    }

    public void calcPayloadLen() {
        // Old method:
        //        int len = 0;
        //        len = len | this.cBuffer.get(0);
        //        len = (len << 8) | this.cBuffer.get(1);

        int len = this.cBuffer.get(0) << 8 | (this.cBuffer.get(1) & 0xFF);
        this.cMessage.len = len;
    }

    public int payloadLen() {
        return this.cMessage.len;
    }

    public void reset() {
        this.cMessage.payload = (ArrayList) this.cBuffer.clone();
        this.queue.add(this.cMessage.clone());
        this.cMessage = new Message();
        this.cRemaining = 2;
        this.cBuffer = new ArrayList<>(this.cRemaining);
    }

    public int queueLen() {
        return this.queue.size();
    }

    public ArrayList<Message> queueAsMut() {
        return this.queue;
    }

    public ArrayList<ArrayList<Byte>> drainQueue() {
        ArrayList<ArrayList<Byte>> buffer = new ArrayList<>(this.queue.size());
        for (int x = 0; x < this.queue.size(); x++) {
            ArrayList payload = (ArrayList)this.queue.get(x).payload.clone();
            buffer.add(payload);
        }
        this.queue = new ArrayList<>();
        return buffer;
    }
}
