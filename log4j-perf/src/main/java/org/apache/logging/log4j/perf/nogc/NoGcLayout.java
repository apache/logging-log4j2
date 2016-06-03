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
package org.apache.logging.log4j.perf.nogc;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.Encoder;
import org.apache.logging.log4j.core.layout.StringBuilderEncoder;
import org.apache.logging.log4j.core.pattern.FormattingInfo;
import org.apache.logging.log4j.core.pattern.PatternFormatter;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Layout that does not allocate temp objects.
 * <p>
 * LOG4J2-1274 proposes to add the {@link #encode} method to the Layout interface.
 * In that scenario, the Appender would supply the ByteBuffer.
 * For this demo we cannot
 * </p>
 */
public class NoGcLayout implements Layout<Serializable>, Encoder<LogEvent> {
    private final StringBuilder cachedStringBuilder = new StringBuilder(2048);
    private final PatternSerializer2 serializer = new PatternSerializer2();
    private final StringBuilderEncoder cachedHelper;

    public NoGcLayout(final Charset charset) {
        cachedHelper = new StringBuilderEncoder(charset);
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        final StringBuilder text = toText(event, getCachedStringBuilder());

        final Encoder<StringBuilder> helper = getCachedHelper();
        helper.encode(text, destination);
    }

    /**
     * Creates a text representation of the specified log event
     * and writes it into the specified StringBuilder.
     * <p>
     * Implementations are free to return a new StringBuilder if they can
     * detect in advance that the specified StringBuilder is too small.
     */
    StringBuilder toText(final LogEvent e, final StringBuilder destination) {
        return serializer.toSerializable(e, destination);
    }

    public StringBuilder getCachedStringBuilder() {
        cachedStringBuilder.setLength(0);
        return cachedStringBuilder;
    }

    public Encoder<StringBuilder> getCachedHelper() {
        return cachedHelper;
    }

    private static class PatternSerializer2 {
        private final PatternFormatter[] formatters;

        PatternSerializer2() {
            this(new PatternFormatter[]{
                    new PatternFormatter(NoGcMessagePatternConverter.newInstance(null, null),
                            FormattingInfo.getDefault()),
            });
        }

        private PatternSerializer2(final PatternFormatter[] formatters) {
            super();
            this.formatters = formatters;
        }

        public StringBuilder toSerializable(final LogEvent event, final StringBuilder buf) {
            //final StringBuilder buf = getStringBuilder();
            final int len = formatters.length;
            for (int i = 0; i < len; i++) {
                formatters[i].format(event, buf);
            }
            //            String str = buf.toString();
            //            if (replace != null) {
            //                str = replace.format(str);
            //            }
            return buf;
        }
    }

    @Override
    public byte[] getFooter() {
        return new byte[0];
    }

    @Override
    public byte[] getHeader() {
        return new byte[0];
    }

    @Override
    public byte[] toByteArray(final LogEvent event) {
        return null;
    }

    @Override
    public Serializable toSerializable(final LogEvent event) {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public Map<String, String> getContentFormat() {
        return null;
    }
}