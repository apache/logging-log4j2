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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class RollingFileManagerTest {

    /**
     * Test the RollingFileManager with a custom DirectFileRolloverStrategy
     *
     * @throws IOException
     */
    @Test
    public void testCustomDirectFileRolloverStrategy() throws IOException {
        class CustomDirectFileRolloverStrategy extends AbstractRolloverStrategy implements DirectFileRolloverStrategy {
            final File file;

            CustomDirectFileRolloverStrategy(File file, StrSubstitutor strSubstitutor) {
                super(strSubstitutor);
                this.file = file;
            }

            @Override
            public String getCurrentFileName(RollingFileManager manager) {
                return file.getAbsolutePath();
            }

            @Override
            public void clearCurrentFileName() {
                // do nothing
            }

            @Override
            public RolloverDescription rollover(RollingFileManager manager) throws SecurityException {
                return null; // do nothing
            }
        }

        try (final LoggerContext ctx = LoggerContext.getContext(false)) {
            final Configuration config = ctx.getConfiguration();
            final File file = File.createTempFile("RollingFileAppenderAccessTest", ".tmp");
            file.deleteOnExit();

            final RollingFileAppender appender = RollingFileAppender.newBuilder()
                    .withFilePattern("FilePattern")
                    .setName("RollingFileAppender")
                    .setConfiguration(config)
                    .withStrategy(new CustomDirectFileRolloverStrategy(file, config.getStrSubstitutor()))
                    .withPolicy(new SizeBasedTriggeringPolicy(100))
                    .build();

            Assert.assertNotNull(appender);
            final String testContent = "Test";
            try(final RollingFileManager manager = appender.getManager()) {
                Assert.assertEquals(file.getAbsolutePath(), manager.getFileName());
                manager.writeToDestination(testContent.getBytes(StandardCharsets.US_ASCII), 0, testContent.length());
            }
            try (final Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.US_ASCII)) {
                Assert.assertEquals(testContent, IOUtils.toString(reader));
            }
        }
    }
}
