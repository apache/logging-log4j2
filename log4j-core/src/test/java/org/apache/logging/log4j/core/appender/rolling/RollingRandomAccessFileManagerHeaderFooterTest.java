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
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 */
public class RollingRandomAccessFileManagerHeaderFooterTest {

    private static final String CONFIG = "RollingRandomAccessFileAppenderHeaderFooterTest.xml";
    private static final String DIR = "target/RollingRandomAccessFileAppenderHeaderFooterTest/";
    private static final String LOGFILE = "target/RollingRandomAccessFileAppenderHeaderFooterTest.log";

    public LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingRandomAccessFileManagerHeaderFooterTest.class.getName());
    }

    @Ignore // FIXME
    @Test
    public void testAppender() throws Exception {
        for (int i = 0; i < 8; ++i) {
            logger.debug("This is test message number " + i);
        }
        Thread.sleep(50);
        final File dir = new File(DIR);
        assertTrue("Directory not created: " + dir, dir.exists() );
        assertTrue("Directory empty: " + dir, dir.listFiles().length > 0);
        final File[] files = dir.listFiles();
        assertNotNull(files);
        for (final File file : files) {
            assertHeader(file);
            assertFooter(file);
        }
        final File logFile = new File(LOGFILE);
        assertTrue("Expected logfile to exist: " + LOGFILE, logFile.exists());
        assertHeader(logFile);
    }

    private void assertHeader(final File file) throws Exception {
        final HtmlLayout layout = HtmlLayout.createDefaultLayout();
        final String header = new String(layout.getHeader(), Charset.defaultCharset());
        final String withoutTimestamp = header.substring(0, 435);
        final String contents = new String(slurp(file), Charset.defaultCharset());
        final String contentsInitialChunk = contents.substring(0, 435);
        assertEquals(file.getName(), withoutTimestamp, contentsInitialChunk);
    }

    private void assertFooter(final File file) throws Exception {
        final HtmlLayout layout = HtmlLayout.createDefaultLayout();
        final String footer = new String(layout.getFooter(), Charset.defaultCharset());
        final String contents = new String(slurp(file), Charset.defaultCharset());
        assertTrue(file.getName(), contents.endsWith(footer));
    }

    private byte[] slurp(final File file) throws Exception {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            final byte[] result = new byte[(int) file.length()];
            int pos = 0;
            int length = in.read(result);
            while (length > 0) {
                pos += length;
                length = in.read(result, pos, result.length - pos);
            }
            return result;
        } finally {
            Closer.closeSilently(in);
        }
    }
}
