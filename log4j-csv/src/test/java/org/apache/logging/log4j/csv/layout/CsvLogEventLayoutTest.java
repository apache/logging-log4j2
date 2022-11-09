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
package org.apache.logging.log4j.csv.layout;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.BasicConfigurationFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.apache.logging.log4j.util.Lazy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests {@link AbstractCsvLayout}.
 *
 * @since 2.4
 */
@UsingAnyThreadContext
public class CsvLogEventLayoutTest {

    @AfterAll
    public static void cleanupClass() {
        LoggerContext.getContext().getInjector().removeBinding(ConfigurationFactory.KEY);
    }

    @BeforeAll
    public static void setupClass() {
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.getInjector().registerBinding(ConfigurationFactory.KEY, Lazy.lazy(BasicConfigurationFactory::new)::value);
        ctx.reconfigure();
    }

    private final LoggerContext ctx = LoggerContext.getContext();

    private final Logger root = ctx.getRootLogger();

    @Test
    public void testCustomCharset() {
        final AbstractCsvLayout layout = CsvLogEventLayout.createLayout(null, "Excel", null, null, null, null, null,
                null, StandardCharsets.UTF_16, null, null);
        assertEquals("text/csv; charset=UTF-16", layout.getContentType());
    }

    @Test
    public void testHeaderFooter() {
        final String header = "# Header";
        final String footer = "# Footer ";
        final AbstractCsvLayout layout = CsvLogEventLayout.createLayout(ctx.getConfiguration(), "Excel", null, null,
                null, null, null, null, null, header, footer);
        testLayout(CSVFormat.DEFAULT, layout, header, footer);
    }

    @Test
    public void testDefaultCharset() {
        final AbstractCsvLayout layout = CsvLogEventLayout.createDefaultLayout();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    @Test
    public void testDefaultContentType() {
        final AbstractCsvLayout layout = CsvLogEventLayout.createDefaultLayout();
        assertEquals("text/csv; charset=UTF-8", layout.getContentType());
    }

    private void testLayout(final CSVFormat format) {
        testLayout(format, CsvLogEventLayout.createLayout(format), null, null);
    }

    private void testLayout(final CSVFormat format, final AbstractCsvLayout layout, final String header, final String footer) {
        final Map<String, Appender> appenders = root.getAppenders();
        for (final Appender appender : appenders.values()) {
            root.removeAppender(appender);
        }
        // set up appender
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        root.debug("one={}, two={}, three={}", 1, 2, 3);
        root.info("Hello");
        appender.stop();

        final List<String> list = appender.getMessages();
        final boolean hasHeaderSerializer = layout.getHeaderSerializer() != null;
        final boolean hasFooterSerializer = layout.getFooterSerializer() != null;
        final int headerOffset = hasHeaderSerializer ? 1 : 0;
        final String event0 = list.get(0 + headerOffset);
        final String event1 = list.get(1 + headerOffset);
        final char del = format.getDelimiter();
        assertTrue(event0.contains(del + "DEBUG" + del), event0);
        final String quote = del == ',' ? "\"" : "";
        assertTrue(event0.contains(del + quote + "one=1, two=2, three=3" + quote + del), event0);
        assertTrue(event1.contains(del + "INFO" + del), event1);

        if (hasHeaderSerializer && header == null) {
            fail();
        }
        if (!hasHeaderSerializer && header != null) {
            fail();
        }
        if (hasFooterSerializer && footer == null) {
            fail();
        }
        if (!hasFooterSerializer && footer != null) {
            fail();
        }
        if (hasHeaderSerializer) {
            assertEquals(header, list.get(0), list.toString());
        }
        if (hasFooterSerializer) {
            assertEquals(footer, list.get(list.size() - 1), list.toString());
        }
    }

    @Test
    public void testLayoutDefault() throws Exception {
        testLayout(CSVFormat.DEFAULT);
    }

    @Test
    public void testLayoutExcel() throws Exception {
        testLayout(CSVFormat.EXCEL);
    }

    @Test
    public void testLayoutMySQL() throws Exception {
        testLayout(CSVFormat.MYSQL);
    }

    @Test
    public void testLayoutRFC4180() throws Exception {
        testLayout(CSVFormat.RFC4180);
    }

    @Test
    public void testLayoutTab() throws Exception {
        testLayout(CSVFormat.TDF);
    }
}
