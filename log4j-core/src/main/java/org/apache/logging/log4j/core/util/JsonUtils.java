/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.util;

/**
 * This class is borrowed from <a href="https://github.com/FasterXML/jackson-core">Jackson</a>.
 */
public final class JsonUtils {

    private static final char[] HC = "0123456789ABCDEF".toCharArray();

    /**
     * Read-only encoding table for first 128 Unicode code points (single-byte UTF-8 characters).
     * Value of 0 means "no escaping"; other positive values that value is character
     * to use after backslash; and negative values that generic (backslash - u)
     * escaping is to be used.
     */
    private static final int[] ESC_CODES;

    static {
        final int[] table = new int[128];
        // Control chars need generic escape sequence
        for (int i = 0; i < 32; ++i) {
            // 04-Mar-2011, tatu: Used to use "-(i + 1)", replaced with constant
            table[i] = -1;
        }
        /* Others (and some within that range too) have explicit shorter
         * sequences
         */
        table['"'] = '"';
        table['\\'] = '\\';
        // Escaping of slash is optional, so let's not add it
        table[0x08] = 'b';
        table[0x09] = 't';
        table[0x0C] = 'f';
        table[0x0A] = 'n';
        table[0x0D] = 'r';
        ESC_CODES = table;
    }

    /**
     * Temporary buffer used for composing quote/escape sequences
     */
    private static final ThreadLocal<char[]> _qbufLocal = new ThreadLocal<>();

    private static char[] getQBuf() {
        char[] _qbuf = _qbufLocal.get();
        if (_qbuf == null) {
            _qbuf = new char[6];
            _qbuf[0] = '\\';
            _qbuf[2] = '0';
            _qbuf[3] = '0';

            _qbufLocal.set(_qbuf);
        }
        return _qbuf;
    }

    /**
     * Quote text contents using JSON standard quoting, and append results to a supplied {@link StringBuilder}.
     */
    public static void quoteAsString(final CharSequence input, final StringBuilder output) {
        final char[] qbuf = getQBuf();
        final int escCodeCount = ESC_CODES.length;
        int inPtr = 0;
        final int inputLen = input.length();

        outer:
        while (inPtr < inputLen) {
            tight_loop:
            while (true) {
                final char c = input.charAt(inPtr);
                if (c < escCodeCount && ESC_CODES[c] != 0) {
                    break tight_loop;
                }
                output.append(c);
                if (++inPtr >= inputLen) {
                    break outer;
                }
            }
            // something to escape; 2 or 6-char variant?
            final char d = input.charAt(inPtr++);
            final int escCode = ESC_CODES[d];
            final int length = (escCode < 0) ? _appendNumeric(d, qbuf) : _appendNamed(escCode, qbuf);

            output.append(qbuf, 0, length);
        }
    }

    private static int _appendNumeric(final int value, final char[] qbuf) {
        qbuf[1] = 'u';
        // We know it's a control char, so only the last 2 chars are non-0
        qbuf[4] = HC[value >> 4];
        qbuf[5] = HC[value & 0xF];
        return 6;
    }

    private static int _appendNamed(final int esc, final char[] qbuf) {
        qbuf[1] = (char) esc;
        return 2;
    }
}
