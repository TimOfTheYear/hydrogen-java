// Copyright 2015 Nathan Sizemore <nathanrsizemore@gmail.com>
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL was not
// distributed with this file, You can obtain one at
// http://mozilla.org/MPL/2.0/.


package com.hydrogen;


public class Frame {

    public static final byte START = 0x01;
    public static final byte END = 0x17;

    public static byte[] fromBuffer(byte[] buffer) {
        byte[] frame = new byte[buffer.length + 4];
        frame[0] = START;
        frame[1] = (byte)(buffer.length >> 8);
        frame[2] = (byte)buffer.length;
        for (int x = 0; x < buffer.length; x++) {
            frame[x + 3] = buffer[x];
        }
        frame[frame.length - 1] = END;

        return frame;
    }
}
