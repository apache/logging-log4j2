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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.BasicConfigurationFactory;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.message.ObjectArrayMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests {@link AbstractCsvLayout}.
 *
 * @since 2.4
 */
public class CsvParameterLayoutTest {
    static ConfigurationFactory cf = new BasicConfigurationFactory();

    @AfterClass
    public static void cleanupClass() {
        ConfigurationFactory.removeConfigurationFactory(cf);
        ThreadContext.clearAll();
    }

    @BeforeClass
    public static void setupClass() {
        ThreadContext.clearAll();
        ConfigurationFactory.setConfigurationFactory(cf);
        final LoggerContext ctx = LoggerContext.getContext();
        ctx.reconfigure();
    }

    private final LoggerContext ctx = LoggerContext.getContext();

    private final Logger root = ctx.getLogger("");

    @Test
    public void testCustomCharset() {
        final AbstractCsvLayout layout = CsvParameterLayout.createLayout(null, "Excel", null, null, null, null, null,
                null, StandardCharsets.UTF_16, null, null);
        assertEquals("text/csv; charset=UTF-16", layout.getContentType());
    }

    @Test
    public void testDefaultCharset() {
        final AbstractCsvLayout layout = CsvParameterLayout.createDefaultLayout();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    @Test
    public void testDefaultContentType() {
        final AbstractCsvLayout layout = CsvParameterLayout.createDefaultLayout();
        assertEquals("text/csv; charset=UTF-8", layout.getContentType());
    }

    private void testLayoutNormalApi(final AbstractCsvLayout layout, boolean messageApi) throws Exception {
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

        // output messages
        if (messageApi) {
            logDebugObjectArrayMessage();
        } else {
            logDebugNormalApi();
        }

        appender.stop();

        final List<String> list = appender.getMessages();
        final char d = layout.getFormat().getDelimiter();
        Assert.assertEquals("1" + d + "2" + d + "3", list.get(0));
        Assert.assertEquals("2" + d + "3", list.get(1));
        Assert.assertEquals("5" + d + "6", list.get(2));
        Assert.assertEquals("7" + d + "8" + d + "9" + d + "10", list.get(3));
    }

    private void logDebugNormalApi() {
        root.debug("{}{}{}", 1, 2, 3);
        root.debug("{}{}{}", 2, 3);
        root.debug("{}{}{}", 5, 6);
        root.debug("{}{}{}", 7, 8, 9, 10);
    }

    private void logDebugObjectArrayMessage() {
        root.debug(new ObjectArrayMessage(1, 2, 3));
        root.debug(new ObjectArrayMessage(2, 3));
        root.debug(new ObjectArrayMessage(5, 6));
        root.debug(new ObjectArrayMessage(7, 8, 9, 10));
    }

    @Test
    public void testLayoutDefaultNormal() throws Exception {
        testLayoutNormalApi(CsvParameterLayout.createDefaultLayout(), false);
    }

    @Test
    public void testLayoutDefaultObjectArrayMessage() throws Exception {
        testLayoutNormalApi(CsvParameterLayout.createDefaultLayout(), true);
    }

    @Test
    public void testLayoutTab() throws Exception {
        testLayoutNormalApi(CsvParameterLayout.createLayout(CSVFormat.TDF), true);
    }
}
