package com.hydrogen;

/**
 * Created by nathan on 1/13/16.
 */
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
