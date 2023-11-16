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
package org.apache.logging.log4j.core.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class StyleConverterTest {

    private static final String EXPECTED =
            "\u001B[1;31mERROR\u001B[m \u001B[1;36mLoggerTest\u001B[m o.a.l.l.c.p.StyleConverterTest org.apache.logging.log4j.core.pattern.StyleConverterTest"
                    + Strings.LINE_SEPARATOR;

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("log4j.skipJansi", "false"); // LOG4J2-2087: explicitly enable
    }

    @Test
    @LoggerContextSource("log4j-style.xml")
    public void testReplacement(final LoggerContext context, @Named("List") final ListAppender app) {
        final Logger logger = context.getLogger("LoggerTest");
        logger.error(this.getClass().getName());

        final List<String> msgs = app.getMessages();
        assertNotNull(msgs);
        assertEquals(1, msgs.size(), "Incorrect number of messages. Should be 1 is " + msgs.size());
        assertTrue(
                msgs.get(0).endsWith(EXPECTED),
                "Replacement failed - expected ending " + EXPECTED + ", actual " + msgs.get(0));
    }

    @Test
    public void testNull() {
        assertNull(StyleConverter.newInstance(null, null));
    }

    @ParameterizedTest
    @MethodSource("org.apache.logging.log4j.core.pattern.HighlightConverterTest#colors")
    public void testHighlightConverterCompatibility(final String color, final String escape) {
        final StyleConverter converter = StyleConverter.newInstance(null, new String[] {"Hello!", color});
        final StringBuilder sb = new StringBuilder();
        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.INFO).build();
        converter.format(event, sb);
        assertEquals(escape + "Hello!" + AnsiEscape.getDefaultStyle(), sb.toString());
    }

    @ParameterizedTest
    @MethodSource("org.apache.logging.log4j.core.pattern.HighlightConverterTest#colors")
    public void testLegacyCommaSeparator(final String color, final String escape) {
        final StyleConverter converter =
                StyleConverter.newInstance(null, new String[] {"Hello!", color.replaceAll("\\s+", ",")});
        final StringBuilder sb = new StringBuilder();
        final LogEvent event = Log4jLogEvent.newBuilder().setLevel(Level.INFO).build();
        converter.format(event, sb);
        assertEquals(escape + "Hello!" + AnsiEscape.getDefaultStyle(), sb.toString());
    }

    @Test
    @UsingStatusListener
    public void testNoAnsiNoWarnings(final ListStatusListener listener) {
        StyleConverter converter = StyleConverter.newInstance(null, new String[] {"", "disableAnsi=true"});
        assertThat(converter).isNotNull();
        converter = StyleConverter.newInstance(null, new String[] {"", "noConsoleNoAnsi=true"});
        assertThat(converter).isNotNull();
        converter = StyleConverter.newInstance(null, new String[] {"", "INVALID_STYLE"});
        assertThat(converter).isNotNull();
        assertThat(listener.findStatusData(Level.WARN))
                .hasSize(1)
                .extracting(data -> data.getMessage().getFormattedMessage())
                .containsExactly("The style attribute INVALID_STYLE is incorrect.");
    }
}
