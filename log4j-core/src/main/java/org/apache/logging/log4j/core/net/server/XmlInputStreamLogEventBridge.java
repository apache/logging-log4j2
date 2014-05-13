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
import org.apache.logging.log4j.core.jackson.Log4jXmlObjectMapper;

/**
 * Reads and logs {@link LogEvent}s from an {@link InputStream}.
 */
public class XmlInputStreamLogEventBridge extends InputStreamLogEventBridge {

    private static final String EVENT_END = "</Event>";
    private static final String EVENT_START_NS_N = "<Event>";
    private static final String EVENT_START_NS_Y = "<Event ";

    public XmlInputStreamLogEventBridge() {
        this(1024, Charset.defaultCharset());
    }

    public XmlInputStreamLogEventBridge(final int bufferSize, final Charset charset) {
        super(new Log4jXmlObjectMapper(), bufferSize, charset, EVENT_END);
    }

    @Override
    protected int[] getEventIndices(final String text, final int beginIndex) {
        int start = text.indexOf(EVENT_START_NS_Y, beginIndex);
        int startLen = EVENT_START_NS_Y.length();
        if (start < 0) {
            start = text.indexOf(EVENT_START_NS_N, beginIndex);
            startLen = EVENT_START_NS_N.length();
        }
        final int end = start < 0 ? -1 : text.indexOf(EVENT_END, start + startLen);
        return new int[] { start, end };
    }

}
