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
package org.apache.logging.log4j.core.layout;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.categories.Layouts;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.junit.ThreadContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests {@link AbstractCsvLayout}.
 *
 * @since 2.4
 */
@Category(Layouts.Csv.class)
public class CsvLogEventLayoutTest {
    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @Rule
    public final ThreadContextRule threadContextRule = new ThreadContextRule(); 

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
    }

    @BeforeClass
    public static void setupClass() {
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
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
        Assert.assertTrue(event0, event0.contains(del + "DEBUG" + del));
        final String quote = del == ',' ? "\"" : "";
        Assert.assertTrue(event0, event0.contains(del + quote + "one=1, two=2, three=3" + quote + del));
        Assert.assertTrue(event1, event1.contains(del + "INFO" + del));
        
        if (hasHeaderSerializer && header == null) {
            Assert.fail();
        }
        if (!hasHeaderSerializer && header != null) {
            Assert.fail();
        }
        if (hasFooterSerializer && footer == null) {
            Assert.fail();
        }
        if (!hasFooterSerializer && footer != null) {
            Assert.fail();
        }
        if (hasHeaderSerializer) {
            Assert.assertEquals(list.toString(), header, list.get(0));
        }
        if (hasFooterSerializer) {
            Assert.assertEquals(list.toString(), footer, list.get(list.size() - 1));
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
