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
package org.apache.logging.log4j.core.net.server;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.apache.logging.log4j.util.Chars;

/**
 * Reads and logs JSON {@link LogEvent}s from an {@link InputStream}..
 */
public class JsonInputStreamLogEventBridge extends InputStreamLogEventBridge {

    private static final int[] END_PAIR = new int[] { END, END };
    private static final char EVENT_END_MARKER = '}';
    private static final char EVENT_START_MARKER = '{';
    private static final char JSON_ESC = '\\';
    private static final char JSON_STR_DELIM = Chars.DQUOTE;
    private static final boolean THREAD_CONTEXT_MAP_AS_LIST = false;

    public JsonInputStreamLogEventBridge() {
        this(1024, Charset.defaultCharset());
    }

    public JsonInputStreamLogEventBridge(final int bufferSize, final Charset charset) {
        super(new Log4jJsonObjectMapper(THREAD_CONTEXT_MAP_AS_LIST, true), bufferSize, charset,
                String.valueOf(EVENT_END_MARKER));
    }

    @Override
    protected int[] getEventIndices(final String text, final int beginIndex) {
        // Scan the text for the end of the next JSON object.
        final int start = text.indexOf(EVENT_START_MARKER, beginIndex);
        if (start == END) {
            return END_PAIR;
        }
        final char[] charArray = text.toCharArray();
        int stack = 0;
        boolean inStr = false;
        boolean inEsc = false;
        for (int i = start; i < charArray.length; i++) {
            final char c = charArray[i];
            if (inEsc) {
            	// Skip this char and continue
            	inEsc = false;
            } else {
                switch (c) {
                case EVENT_START_MARKER:
                    if (!inStr) {
                        stack++;
                    }
                    break;
                case EVENT_END_MARKER:
                    if (!inStr) {
                        stack--;
                    }
                    break;
                case JSON_STR_DELIM:
                    inStr = !inStr;
                    break;
                case JSON_ESC:
                    inEsc = true;
                    break;
                }
                if (stack == 0) {
                    return new int[] { start, i };
                }
            }
        }
        return END_PAIR;
    }

}
