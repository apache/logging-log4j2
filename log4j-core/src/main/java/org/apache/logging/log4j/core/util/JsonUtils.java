/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.util;

import com.fasterxml.jackson.core.io.CharTypes;

/**
 * This class is borrowed from <a href="https://github.com/FasterXML/jackson-core">Jackson</a>.
 */
public final class JsonUtils {

    private final static char[] HC = CharTypes.copyHexChars();

    /**
     * Temporary buffer used for composing quote/escape sequences
     */
    private final static ThreadLocal<char[]> _qbufLocal = new ThreadLocal<>();

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
    public static void quoteAsString(CharSequence input, StringBuilder output) {
        final char[] qbuf = getQBuf();
        final int[] escCodes = CharTypes.get7BitOutputEscapes();
        final int escCodeCount = escCodes.length;
        int inPtr = 0;
        final int inputLen = input.length();

        outer:
        while (inPtr < inputLen) {
            tight_loop:
            while (true) {
                char c = input.charAt(inPtr);
                if (c < escCodeCount && escCodes[c] != 0) {
                    break tight_loop;
                }
                output.append(c);
                if (++inPtr >= inputLen) {
                    break outer;
                }
            }
            // something to escape; 2 or 6-char variant?
            char d = input.charAt(inPtr++);
            int escCode = escCodes[d];
            int length = (escCode < 0)
                    ? _appendNumeric(d, qbuf)
                    : _appendNamed(escCode, qbuf);

            output.append(qbuf, 0, length);
        }
    }

    private static int _appendNumeric(int value, char[] qbuf) {
        qbuf[1] = 'u';
        // We know it's a control char, so only the last 2 chars are non-0
        qbuf[4] = HC[value >> 4];
        qbuf[5] = HC[value & 0xF];
        return 6;
    }

    private static int _appendNamed(int esc, char[] qbuf) {
        qbuf[1] = (char) esc;
        return 2;
    }

}
