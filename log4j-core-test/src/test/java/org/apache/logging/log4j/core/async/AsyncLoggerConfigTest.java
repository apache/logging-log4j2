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
package org.apache.logging.log4j.core.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig.RootLogger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("AsyncLoggers")
@UsingStatusListener
public class AsyncLoggerConfigTest {

    private static final String FQCN = AsyncLoggerConfigTest.class.getName();

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    public void testAdditivity(final LoggerContext context) throws Exception {
        final Path file = loggingPath.resolve("AsyncLoggerConfigTest.log");
        assertThat(file).isEmptyFile();

        final Logger log = context.getLogger("com.foo.Bar");
        final String msg = "Additive logging: 2 for the price of 1!";
        log.info(msg);
        CoreLoggerContexts.stopLoggerContext(file.toFile()); // stop async thread

        final String location = "testAdditivity";
        try (final BufferedReader reader = Files.newBufferedReader(file)) {
            for (int i = 0; i < 2; i++) {
                assertThat(reader.readLine())
                        .as("Message")
                        .contains(msg)
                        .as("Location")
                        .contains(location);
            }
        }
    }

    @Test
    public void testIncludeLocationDefaultsToFalse() {
        final Configuration configuration = new NullConfiguration();
        final LoggerConfig rootLoggerConfig =
                RootLogger.newAsyncRootBuilder().withConfig(configuration).build();
        assertFalse(rootLoggerConfig.isIncludeLocation(), "Include location should default to false for async loggers");

        final LoggerConfig loggerConfig = AsyncLoggerConfig.newAsyncBuilder()
                .withConfig(configuration)
                .withLoggerName("com.foo.Bar")
                .build();
        assertFalse(loggerConfig.isIncludeLocation(), "Include location should default to false for async loggers");
    }

    @Test
    public void testSingleFilterInvocation() {
        final Configuration configuration = new NullConfiguration();
        final Filter filter = mock(Filter.class);
        final LoggerConfig config = AsyncLoggerConfig.newAsyncBuilder()
                .withLoggerName(FQCN)
                .withConfig(configuration)
                .withLevel(Level.INFO)
                .withtFilter(filter)
                .build();
        final Appender appender = mock(Appender.class);
        when(appender.isStarted()).thenReturn(true);
        when(appender.getName()).thenReturn("test");
        config.addAppender(appender, null, null);
        final AsyncLoggerConfigDisruptor disruptor =
                (AsyncLoggerConfigDisruptor) configuration.getAsyncLoggerConfigDelegate();
        disruptor.start();
        try {
            config.log(FQCN, FQCN, null, Level.INFO, new SimpleMessage(), null);
            verify(appender, timeout(500).times(1)).append(any());
            verify(filter, times(1)).filter(any());
        } finally {
            disruptor.stop();
        }
    }
}
