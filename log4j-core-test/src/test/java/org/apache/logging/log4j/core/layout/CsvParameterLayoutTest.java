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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.ReconfigurationPolicy;
import org.apache.logging.log4j.message.ObjectArrayMessage;
import org.apache.logging.log4j.test.junit.UsingThreadContextStack;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link AbstractCsvLayout}.
 *
 * @since 2.4
 */
@Tag("Layouts.Csv")
@UsingThreadContextStack
@LoggerContextSource(reconfigure = ReconfigurationPolicy.AFTER_EACH)
public class CsvParameterLayoutTest {

    public void testCustomCharset() {
        final AbstractCsvLayout layout = CsvParameterLayout.createLayout(
                null, "Excel", null, null, null, null, null, null, StandardCharsets.UTF_16, null, null);
        assertEquals("text/csv; charset=UTF-16", layout.getContentType());
    }

    @Test
    @LoggerContextSource("csvParamsSync.xml")
    public void testCustomCharsetcsvParamsSync() {
        testCustomCharset();
    }

    @Test
    @LoggerContextSource("csvParamsMixedAsync.xml")
    public void testCustomCharsetcsvParamsMixedAsync() {
        testCustomCharset();
    }

    public void testDefaultCharset() {
        final AbstractCsvLayout layout = CsvParameterLayout.createDefaultLayout();
        assertEquals(StandardCharsets.UTF_8, layout.getCharset());
    }

    @Test
    @LoggerContextSource("csvParamsSync.xml")
    public void testDefaultCharsetcsvParamsSync() {
        testDefaultCharset();
    }

    @Test
    @LoggerContextSource("csvParamsMixedAsync.xml")
    public void testDefaultCharsetcsvParamsMixedAsync() {
        testDefaultCharset();
    }

    public void testDefaultContentType() {
        final AbstractCsvLayout layout = CsvParameterLayout.createDefaultLayout();
        assertEquals("text/csv; charset=UTF-8", layout.getContentType());
    }

    @Test
    @LoggerContextSource("csvParamsSync.xml")
    public void testDefaultContentTypecsvParamsSync() {
        testDefaultContentType();
    }

    @Test
    @LoggerContextSource("csvParamsMixedAsync.xml")
    public void testDefaultContentTypecsvParamsMixedAsync() {
        testDefaultContentType();
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
        assertEquals(msgCount, appender.getMessages().size(), "Background thread did not finish processing: msg count");

        // don't stop appender until background thread is done
        appender.stop();

        final List<String> list = appender.getMessages();
        final char d = layout.getFormat().getDelimiter();
        assertEquals("1" + d + "2" + d + "3", list.get(0));
        assertEquals("2" + d + "3", list.get(1));
        assertEquals("5" + d + "6", list.get(2));
        assertEquals("7" + d + "8" + d + "9" + d + "10", list.get(3));
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

    public void testLayoutDefaultNormal(final LoggerContext context) throws Exception {
        final Logger root = context.getRootLogger();
        testLayoutNormalApi(root, CsvParameterLayout.createDefaultLayout(), false);
    }

    @Test
    @LoggerContextSource("csvParamsSync.xml")
    public void testLayoutDefaultNormalCsvParamsSync(final LoggerContext context) throws Exception {
        testLayoutDefaultNormal(context);
    }

    @Test
    @LoggerContextSource("csvParamsMixedAsync.xml")
    public void testLayoutDefaultNormalCsvParamsMixedAsync(final LoggerContext context) throws Exception {
        testLayoutDefaultNormal(context);
    }

    public void testLayoutDefaultObjectArrayMessage(final LoggerContext context) throws Exception {
        final Logger root = context.getRootLogger();
        testLayoutNormalApi(root, CsvParameterLayout.createDefaultLayout(), true);
    }

    @Test
    @LoggerContextSource("csvParamsSync.xml")
    public void testLayoutDefaultObjectArrayMessageCsvParamsSync(final LoggerContext context) throws Exception {
        testLayoutDefaultObjectArrayMessage(context);
    }

    @Test
    @LoggerContextSource("csvParamsMixedAsync.xml")
    public void testLayoutDefaultObjectArrayMessageCsvParamsMixedAsync(final LoggerContext context) throws Exception {
        testLayoutDefaultObjectArrayMessage(context);
    }

    public void testLayoutTab(final LoggerContext context) throws Exception {
        final Logger root = context.getRootLogger();
        testLayoutNormalApi(root, CsvParameterLayout.createLayout(CSVFormat.TDF), true);
    }

    @Test
    @LoggerContextSource("csvParamsSync.xml")
    public void testLayoutTabCsvParamsSync(final LoggerContext context) throws Exception {
        testLayoutTab(context);
    }

    @Test
    @LoggerContextSource("csvParamsMixedAsync.xml")
    public void testLayoutTabCsvParamsMixedAsync(final LoggerContext context) throws Exception {
        testLayoutTab(context);
    }

    public void testLogJsonArgument(final LoggerContext context) throws InterruptedException {
        final ListAppender appender = context.getConfiguration().getAppender("List");
        appender.countDownLatch = new CountDownLatch(4);
        appender.clear();
        final Logger logger = (Logger) LogManager.getRootLogger();
        final String json = "{\"id\":10,\"name\":\"Alice\"}";
        logger.error("log:{}", json);
        // wait until background thread finished processing
        final int msgCount = 1;
        if (appender.getMessages().isEmpty()) {
            appender.countDownLatch.await(5, TimeUnit.SECONDS);
        }
        assertEquals(msgCount, appender.getMessages().size(), "Background thread did not finish processing: msg count");

        // don't stop appender until background thread is done
        appender.stop();
        final List<String> list = appender.getMessages();
        final String eventStr = list.get(0).toString();
        assertTrue(eventStr.contains(json), eventStr);
    }

    @Test
    @LoggerContextSource("csvParamsSync.xml")
    public void testLogJsonArgumentCsvParamsSync(final LoggerContext context) throws InterruptedException {
        testLogJsonArgument(context);
    }

    @Test
    @LoggerContextSource("csvParamsMixedAsync.xml")
    public void testLogJsonArgumentCsvParamsMixedAsync(final LoggerContext context) throws InterruptedException {
        testLogJsonArgument(context);
    }
}
