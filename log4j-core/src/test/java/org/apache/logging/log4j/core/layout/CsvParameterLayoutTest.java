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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.ObjectArrayMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 * Tests {@link AbstractCsvLayout}.
 *
 * @since 2.4
 */
@RunWith(value = Parameterized.class)
public class CsvParameterLayoutTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        { new LoggerContextRule("csvParamsSync.xml"), },
                        { new LoggerContextRule("csvParamsMixedAsync.xml"), },
                }
        );
    }

    @Rule
    public final LoggerContextRule init;

    public CsvParameterLayoutTest(final LoggerContextRule contextRule) {
        this.init = contextRule;
    }

    @AfterClass
    public static void cleanupClass() {
//        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, null);
        ThreadContext.clearAll();
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

    static void testLayoutNormalApi(final Logger root, final AbstractCsvLayout layout, boolean messageApi) throws Exception {
        final Map<String, Appender> appenders = root.getAppenders();
        for (final Appender appender : appenders.values()) {
            root.removeAppender(appender);
        }
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

        // wait until background thread finished processing
        appender.countDownLatch.await(10, TimeUnit.SECONDS);
        assertEquals("Background thread did not finish processing: msg count", 4, appender.getMessages().size());

        // don't stop appender until background thread is done
        appender.stop();

        final List<String> list = appender.getMessages();
        final char d = layout.getFormat().getDelimiter();
        Assert.assertEquals("1" + d + "2" + d + "3", list.get(0));
        Assert.assertEquals("2" + d + "3", list.get(1));
        Assert.assertEquals("5" + d + "6", list.get(2));
        Assert.assertEquals("7" + d + "8" + d + "9" + d + "10", list.get(3));
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
        Logger root = this.init.getLogger("");
        testLayoutNormalApi(root, CsvParameterLayout.createDefaultLayout(), false);
    }

    @Test
    public void testLayoutDefaultObjectArrayMessage() throws Exception {
        Logger root = this.init.getLogger("");
        testLayoutNormalApi(root, CsvParameterLayout.createDefaultLayout(), true);
    }

    @Test
    public void testLayoutTab() throws Exception {
        Logger root = this.init.getLogger("");
        testLayoutNormalApi(root, CsvParameterLayout.createLayout(CSVFormat.TDF), true);
    }
}
