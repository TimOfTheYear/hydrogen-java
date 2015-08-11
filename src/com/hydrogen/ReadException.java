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

public class ReadException extends Exception {
    public static final String IO = "IO";
    public static final String EOF = "EOF";
    public static final String BUF = "BUF";

    public ReadException(String message) {
        super(message);
    }
}
