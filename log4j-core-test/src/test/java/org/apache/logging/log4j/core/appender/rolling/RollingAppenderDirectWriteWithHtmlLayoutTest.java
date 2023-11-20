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
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for LOG4J2-2760
 */
@Tag("sleepy")
@CleanUpDirectories("target/rolling-direct-htmlLayout")
@LoggerContextSource(value = "")
public class RollingAppenderDirectWriteWithHtmlLayoutTest {

    private static final String DIR = "target/rolling-direct-htmlLayout";

    @Test
    public void testRollingFileAppenderWithHtmlLayout(final Configuration config) throws Exception {
        checkAppenderWithHtmlLayout(true, config);
    }

    @Test
    public void testRollingFileAppenderWithHtmlLayoutNoAppend(final Configuration config) throws Exception {
        checkAppenderWithHtmlLayout(false, config);
    }

    private void checkAppenderWithHtmlLayout(final boolean append, final Configuration config)
            throws InterruptedException, IOException {
        final String prefix = "testHtml_" + (append ? "append_" : "noAppend_");
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
            assertTrue(dir.exists(), "Directory not created");
            final File[] files = dir.listFiles();
            assertNotNull(files);
            assertThat(files, hasItemInArray(that(hasName(that(endsWith(".html"))))));

            int foundEvents = 0;
            final Pattern eventMatcher = Pattern.compile("title=\"Message\"");
            for (File file : files) {
                if (!file.getName().startsWith(prefix)) continue;
                final String data = Files.readString(file.toPath()).trim();
                // check that every file starts with the header
                assertThat("header in file " + file, data, startsWith("<!DOCTYPE"));
                assertThat("footer in file " + file, data, endsWith("</html>"));
                final Matcher matcher = eventMatcher.matcher(data);
                while (matcher.find()) {
                    foundEvents++;
                }
            }
            assertEquals(count, foundEvents, "Incorrect number of events read.");
        } finally {
            if (!stopped) {
                appender.stop();
            }
        }
    }
}
