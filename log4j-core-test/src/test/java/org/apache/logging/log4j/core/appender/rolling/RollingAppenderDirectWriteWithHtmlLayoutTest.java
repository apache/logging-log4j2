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
package org.apache.logging.log4j.core.appender.rolling;

import static org.apache.logging.log4j.core.test.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.core.test.hamcrest.FileMatchers.hasName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.core.util.IOUtils;
import org.apache.logging.log4j.message.SimpleMessage;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests for LOG4J2-2760
 */
public class RollingAppenderDirectWriteWithHtmlLayoutTest {

    private static final String DIR = "target/rolling-direct-htmlLayout";

    public static LoggerContextRule loggerContextRule = new LoggerContextRule();

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    @Test
    public void testRollingFileAppenderWithHtmlLayout() throws Exception {
        checkAppenderWithHtmlLayout(true);
    }

    @Test
    public void testRollingFileAppenderWithHtmlLayoutNoAppend() throws Exception {
        checkAppenderWithHtmlLayout(false);
    }

    private void checkAppenderWithHtmlLayout(final boolean append) throws InterruptedException, IOException {
        final String prefix = "testHtml_" + (append ? "append_" : "noAppend_");
        final Configuration config = loggerContextRule.getConfiguration();
        final RollingFileAppender appender = RollingFileAppender.newBuilder()
                .setName("RollingHtml")
                .setFilePattern(DIR + "/" + prefix + "_-%d{MM-dd-yy-HH-mm}-%i.html")
                .setPolicy(new SizeBasedTriggeringPolicy(500))
                .setStrategy(DirectWriteRolloverStrategy.newBuilder()
                        .setConfig(config)
                        .build())
                .setLayout(HtmlLayout.createDefaultLayout())
                .setAppend(append)
                .build();
        boolean stopped = false;
        try {
            final int count = 100;
            for (int i = 0; i < count; ++i) {
                appender.append(Log4jLogEvent.newBuilder()
                        .setMessage(new SimpleMessage("This is test message number " + i))
                        .build());
            }
            appender.getManager().flush();
            appender.stop();
            stopped = true;
            Thread.sleep(50);
            final File dir = new File(DIR);
            assertTrue("Directory not created", dir.exists());
            final File[] files = dir.listFiles();
            assertNotNull(files);
            assertThat(files, hasItemInArray(that(hasName(that(endsWith(".html"))))));

            int foundEvents = 0;
            final Pattern eventMatcher = Pattern.compile("title=\"Message\"");
            for (File file : files) {
                if (!file.getName().startsWith(prefix)) {
                    continue;
                }
                try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    final String data = IOUtils.toString(reader).trim();
                    // check that every file starts with the header
                    assertThat("header in file " + file, data, Matchers.startsWith("<!DOCTYPE"));
                    assertThat("footer in file " + file, data, Matchers.endsWith("</html>"));
                    final Matcher matcher = eventMatcher.matcher(data);
                    while (matcher.find()) {
                        foundEvents++;
                    }
                }
            }
            assertEquals("Incorrect number of events read.", count, foundEvents);
        } finally {
            if (!stopped) {
                appender.stop();
            }
        }
    }
}
