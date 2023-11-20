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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

/**
 * LOG4J2-1725.
 */
@UsingStatusListener
public class RollingAppenderReconfigureTest {

    private static final URL CONFIG =
            RollingAppenderReconfigureTest.class.getResource("RollingAppenderReconfigureTest.xml");
    private static final File CONFIG_FILE = FileUtils.toFile(CONFIG);

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    public void testReconfigure(final LoggerContext context) throws Exception {
        final Logger logger = context.getLogger(getClass());
        for (int i = 0; i < 500; ++i) {
            logger.debug("This is test message number {}", i);
        }

        assertThat(loggingPath).isDirectoryContaining("glob:**/*.current").isDirectoryContaining("glob:**/*.rolled");

        final String originalXmlConfig = FileUtils.readFileToString(CONFIG_FILE, UTF_8);
        try {
            final String updatedXmlConfig =
                    originalXmlConfig.replace("rollingtest.%i.rolled", "rollingtest.%i.reconfigured");
            FileUtils.write(CONFIG_FILE, updatedXmlConfig, UTF_8);

            // Reconfigure
            context.reconfigure();

            for (int i = 0; i < 500; ++i) {
                logger.debug("This is test message number {}", i);
            }

            assertThat(loggingPath)
                    .isDirectoryContaining("glob:**/*.reconfigured")
                    .isDirectoryContaining("glob:**/*.current")
                    .isDirectoryContaining("glob:**/*.rolled");
        } finally {
            FileUtils.write(CONFIG_FILE, originalXmlConfig, UTF_8);
        }
    }
}
