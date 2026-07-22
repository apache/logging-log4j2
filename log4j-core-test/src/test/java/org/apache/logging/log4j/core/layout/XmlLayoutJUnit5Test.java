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
package org.apache.logging.log4j.core.layout;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.DefaultThreadContextStack;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class XmlLayoutJUnit5Test {

    private static Log4jLogEvent createLogEventWithString(final String str) {
        final Marker marker = MarkerManager.getMarker("marker" + str);

        final RuntimeException thrown = new RuntimeException("thrown" + str);
        thrown.addSuppressed(new IllegalStateException("suppressed" + str));

        final StringMap contextData = ContextDataFactory.createContextData();
        contextData.putValue("mdcKey" + str, "mdcValue" + str);

        final DefaultThreadContextStack contextStack = new DefaultThreadContextStack();
        contextStack.clear();
        contextStack.push("contextStack" + str);

        final StackTraceElement source =
                new StackTraceElement("class" + str, "method" + str, "file" + str + ".java", 123);

        return Log4jLogEvent.newBuilder()
                .setLoggerName("logger" + str)
                .setMarker(marker)
                .setLoggerFqcn("fqcn" + str)
                .setLevel(Level.DEBUG)
                .setMessage(new SimpleMessage("message" + str))
                .setThrown(thrown)
                .setContextData(contextData)
                .setContextStack(contextStack)
                .setThreadName("thread" + str)
                .setSource(source)
                .setTimeMillis(1L)
                .build();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "\u0000",
                "\u001F",
                // hi surrogate
                "\uD800",
                // low surrogate
                "\uDC00",
                // invalid chars
                "\uFFFE",
                "\uFFFF"
            })
    void testInvalidXmlCharsAreSanitized(final String invalidXmlChars) {
        final Log4jLogEvent event = createLogEventWithString(invalidXmlChars);
        final AbstractJacksonLayout layout = XmlLayout.newBuilder()
                .setCompact(true)
                .setIncludeStacktrace(true)
                .setLocationInfo(true)
                .setProperties(true)
                .build();
        final String str = layout.toSerializable(event);
        assertThat(str).doesNotContain(invalidXmlChars).contains("\uFFFD");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                " ",
                "A",
                // First character from supplementary plane
                "\uD801\uDC00",
                // Last character from supplementary plane
                "\uDBFF\uDFFF"
            })
    void testValidXmlCharsAreKept(final String validXmlChars) {
        final Log4jLogEvent event = createLogEventWithString(validXmlChars);
        final AbstractJacksonLayout layout = XmlLayout.newBuilder()
                .setCompact(true)
                .setIncludeStacktrace(true)
                .setLocationInfo(true)
                .setProperties(true)
                .build();
        final String str = layout.toSerializable(event);
        assertThat(str).contains(validXmlChars).doesNotContain("\uFFFD");
    }
}
