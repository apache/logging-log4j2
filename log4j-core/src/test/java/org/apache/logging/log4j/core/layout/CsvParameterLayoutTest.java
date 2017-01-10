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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.categories.Layouts;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.junit.ThreadContextRule;
import org.apache.logging.log4j.message.ObjectArrayMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests {@link AbstractCsvLayout}.
 *
 * @since 2.4
 */
@RunWith(Parameterized.class)
@Category(Layouts.Csv.class)
public class CsvParameterLayoutTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { { new LoggerContextRule("csvParamsSync.xml"), },
                { new LoggerContextRule("csvParamsMixedAsync.xml"), }, });
    }

    @Rule
    public final LoggerContextRule init;

    @Rule
    public final ThreadContextRule threadContextRule = new ThreadContextRule();

    public CsvParameterLayoutTest(final LoggerContextRule contextRule) {
        this.init = contextRule;
    }

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

    static void testLayoutNormalApi(final Logger root, final AbstractCsvLayout layout, final boolean messageApi)
            throws Exception {
        removeAppenders(root);
        // set up appender
        final ListAppender appender = new ListAppender("List", null, layout, true, false);
        appender.start();

        appender.countDownLatch = new CountDownLatch(4);

        // set appender on root and set level to debug
        root.addAppender(appender);
        root.setLevel(Level.DEBUG);

        // output messages
        if (messageApi) {
            logDebugObjectArrayMessage(root);
        } else {
            logDebugNormalApi(root);
        }
        final int msgCount = 4;
        if (appender.getMessages().size() < msgCount) {
            // wait until background thread finished processing
            appender.countDownLatch.await(10, TimeUnit.SECONDS);
        }
        assertEquals("Background thread did not finish processing: msg count", msgCount, appender.getMessages().size());

        // don't stop appender until background thread is done
        appender.stop();

        final List<String> list = appender.getMessages();
        final char d = layout.getFormat().getDelimiter();
        Assert.assertEquals("1" + d + "2" + d + "3", list.get(0));
        Assert.assertEquals("2" + d + "3", list.get(1));
        Assert.assertEquals("5" + d + "6", list.get(2));
        Assert.assertEquals("7" + d + "8" + d + "9" + d + "10", list.get(3));
    }

    private static void removeAppenders(final Logger root) {
        final Map<String, Appender> appenders = root.getAppenders();
        for (final Appender appender : appenders.values()) {
            root.removeAppender(appender);
        }
    }

    private static void logDebugNormalApi(final Logger root) {
        root.debug("with placeholders: {}{}{}", 1, 2, 3);
        root.debug("without placeholders", 2, 3);
        root.debug(null, 5, 6);
        root.debug("invalid placeholder count {}", 7, 8, 9, 10);
    }

    private static void logDebugObjectArrayMessage(final Logger root) {
        root.debug(new ObjectArrayMessage(1, 2, 3));
        root.debug(new ObjectArrayMessage(2, 3));
        root.debug(new ObjectArrayMessage(5, 6));
        root.debug(new ObjectArrayMessage(7, 8, 9, 10));
    }

    @Test
    public void testLayoutDefaultNormal() throws Exception {
        final Logger root = this.init.getRootLogger();
        testLayoutNormalApi(root, CsvParameterLayout.createDefaultLayout(), false);
    }

    @Test
    public void testLayoutDefaultObjectArrayMessage() throws Exception {
        final Logger root = this.init.getRootLogger();
        testLayoutNormalApi(root, CsvParameterLayout.createDefaultLayout(), true);
    }

    @Test
    public void testLayoutTab() throws Exception {
        final Logger root = this.init.getRootLogger();
        testLayoutNormalApi(root, CsvParameterLayout.createLayout(CSVFormat.TDF), true);
    }

    @Test
    public void testLogJsonArgument() throws InterruptedException {
        final ListAppender appender = (ListAppender) init.getAppender("List");
        appender.countDownLatch = new CountDownLatch(4);
        appender.clear();
        final Logger logger = (Logger) LogManager.getRootLogger();
        final String json = "{\"id\":10,\"name\":\"Alice\"}";
        logger.error("log:{}", json);
        // wait until background thread finished processing
        final int msgCount = 1;
        if (appender.getMessages().size() < msgCount) {
            appender.countDownLatch.await(5, TimeUnit.SECONDS);
        }
        assertEquals("Background thread did not finish processing: msg count", msgCount, appender.getMessages().size());

        // don't stop appender until background thread is done
        appender.stop();
        final List<String> list = appender.getMessages();
        final String eventStr = list.get(0).toString();
        Assert.assertTrue(eventStr, eventStr.contains(json));
    }

}
