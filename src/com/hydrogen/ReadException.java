package com.hydrogen;

/**
 * Created by nathan on 1/13/16.
 */
public class ReadException extends Exception {
    public static final String IO = "IO";
    public static final String EOF = "EOF";
    public static final String BUF = "BUF";
    public static final String NULLVAL = "NULLVAL";

    public ReadException(String message) {
        super(message);
    }
}
