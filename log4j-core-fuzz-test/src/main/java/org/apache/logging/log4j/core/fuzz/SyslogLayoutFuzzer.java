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
package org.apache.logging.log4j.core.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueLow;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.layout.Rfc5424Layout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StructuredDataId;
import org.apache.logging.log4j.message.StructuredDataMessage;

/**
 * Fuzzes the syslog and structured-data rendering round trips, which are not covered by
 * the existing message-layout fuzzers.
 *
 * <p>Oracles:
 * <ol>
 *   <li>Rfc5424Layout and SyslogLayout render exactly one syslog frame per event
 *       (no PRI header injection through MDC values or message text).</li>
 *   <li>StructuredDataMessage.asString(FULL) round-trips every PARAM-VALUE
 *       (PARAM-VALUE count equals the number of entries created).</li>
 *   <li>GelfLayout output is JSON-conformant (no raw control characters inside strings).</li>
 * </ol>
 *
 * @since 2.27.0
 */
public final class SyslogLayoutFuzzer {

    private static final Layout<?> RFC5424 = Rfc5424Layout.newBuilder()
            .setAppName("fuzz")
            .setIncludeMDC(true)
            .build();

    private static final Layout<?> BSD = SyslogLayout.newBuilder()
            .setFacility(Facility.LOCAL0)
            .setIncludeNewLine(false)
            .build();

    private static final Layout<?> GELF = GelfLayout.newBuilder()
            .setHost("fuzz")
            .setCompressionType(GelfLayout.CompressionType.OFF)
            .build();

    public static void fuzzerTestOneInput(final FuzzedDataProvider dataProvider) {
        final String key = dataProvider.consumeString(64);
        final String value = dataProvider.consumeString(256);
        final String message = dataProvider.consumeRemainingAsString();
        if (key == null || value == null || message == null) {
            return;
        }
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName(SyslogLayoutFuzzer.class.getName())
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage(message))
                .build();

        // 1. One event must render as exactly one syslog frame.
        ThreadContext.put(key, value);
        try {
            checkSingleFrame("Rfc5424Layout", RFC5424, event);
            checkSingleFrame("SyslogLayout", BSD, event);
        } finally {
            ThreadContext.clearMap();
        }

        // 2. Structured data round trip.
        final StructuredDataMessage structured = new StructuredDataMessage(
                new StructuredDataId("fuzz@41094", null, null), message, "FUZZ");
        structured.put(key, value);
        structured.put("fixed", "ok");
        final String rendered =
                structured.asString(StructuredDataMessage.Format.FULL, null);
        final int quoteCount = countUnescapedQuotes(rendered);
        if (quoteCount % 2 != 0 || quoteCount / 2 != 2) {
            throw new FuzzerSecurityIssueLow(String.format(
                    "StructuredDataMessage.asString(FULL) emits %d PARAM-VALUEs for 2 entries; output: %s",
                    quoteCount / 2, sanitize(rendered)));
        }

        // 3. GELF must be valid JSON.
        final String gelfOutput =
                new String(GELF.toByteArray(event), StandardCharsets.UTF_8);
        if (hasRawControlCharacterInString(gelfOutput)) {
            throw new FuzzerSecurityIssueLow(String.format(
                    "GelfLayout emits a raw control character inside a JSON string; output: %s",
                    sanitize(gelfOutput)));
        }
    }

    private static void checkSingleFrame(final String name, final Layout<?> layout, final LogEvent event) {
        final String output = new String(layout.toByteArray(event), StandardCharsets.UTF_8);
        // With newLine=false a conforming frame contains no line breaks at all; any CR, LF,
        // NEL (U+0085) or U+2028 lets a line-oriented receiver split the frame (PRI injection).
        if (output.indexOf('\r') >= 0
                || output.indexOf('\n') >= 0
                || output.indexOf('\u0085') >= 0
                || output.indexOf('\u2028') >= 0
                || output.indexOf('\u2029') >= 0) {
            throw new FuzzerSecurityIssueLow(String.format(
                    "%s rendered 1 event with an embedded line break (frame forging); output: %s",
                    name, sanitize(output)));
        }
    }

    private static int countPriHeaders(final String output) {
        int count = 0;
        for (int i = 0; i < output.length() - 1; i++) {
            if (output.charAt(i) == '<' && Character.isDigit(output.charAt(i + 1))) {
                int j = i + 1;
                while (j < output.length() && Character.isDigit(output.charAt(j))) {
                    j++;
                }
                if (j < output.length() && output.charAt(j) == '>') {
                    count++;
                    i = j;
                }
            }
        }
        return count;
    }

    private static int countUnescapedQuotes(final String output) {
        int count = 0;
        boolean inString = false;
        int backslashRun = 0;
        for (int i = 0; i < output.length(); i++) {
            final char c = output.charAt(i);
            if (c == '\\') {
                backslashRun++;
            } else {
                if (c == '"' && backslashRun % 2 == 0) {
                    // Not preceded by an escaped backslash: structural quote.
                    count++;
                    inString = !inString;
                }
                backslashRun = 0;
            }
        }
        return inString ? count + 1 : count;
    }

    private static boolean hasRawControlCharacterInString(final String output) {
        boolean inString = false;
        for (int i = 0; i < output.length(); i++) {
            final char c = output.charAt(i);
            if (inString && c < 0x20) {
                return true;
            }
            if (c == '"' && (i == 0 || output.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
        }
        return inString;
    }

    private static String sanitize(final String s) {
        return s.replace("\r", "\\r").replace("\n", "\\n");
    }
}
