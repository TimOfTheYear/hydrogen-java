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

public class Message {
    public int len;
    public Vector<Byte> payload;

    public Message() {
        this.len = 2;
        this.payload = new Vector<>();
    }

    public Message clone() {
        Message msg = new Message();
        msg.len = this.len;
        msg.payload = (Vector) this.payload.clone();
        return msg;
    }
}
