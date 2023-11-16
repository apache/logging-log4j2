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
package org.apache.logging.log4j.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

@UsingStatusListener
public class CustomConfigurationTest {

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @SetTestProperty(key = "log4j.level", value = "INFO")
    @SetTestProperty(key = "log.level", value = "INFO")
    @LoggerContextSource
    public void testConfig(final LoggerContext ctx) throws IOException {
        final Path logFile = loggingPath.resolve("test.log");
        // don't bother using "error" since that's the default; try another level
        final Configuration config = ctx.getConfiguration();
        assertThat(config).isInstanceOf(XmlConfiguration.class);
        for (final StatusListener listener : StatusLogger.getLogger().getListeners()) {
            if (listener instanceof StatusConsoleListener) {
                assertSame(listener.getStatusLevel(), Level.INFO);
                break;
            }
        }
        final Layout<? extends Serializable> layout = PatternLayout.newBuilder()
                .withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
                .withConfiguration(config)
                .build();
        final FileAppender appender = FileAppender.newBuilder()
                .withFileName(logFile.toString())
                .withAppend(false)
                .setName("File")
                .setIgnoreExceptions(false)
                .withBufferedIo(false)
                .setLayout(layout)
                .build();
        appender.start();
        config.addAppender(appender);
        final AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
        final AppenderRef[] refs = new AppenderRef[] {ref};

        final LoggerConfig loggerConfig = LoggerConfig.createLogger(
                false, Level.INFO, "org.apache.logging.log4j", "true", refs, null, config, null);
        loggerConfig.addAppender(appender, null, null);
        config.addLogger("org.apache.logging.log4j", loggerConfig);
        ctx.updateLoggers();
        final Logger logger = ctx.getLogger(CustomConfigurationTest.class);
        logger.info("This is a test");
        assertThat(logFile).exists().isNotEmptyFile();
    }
}
