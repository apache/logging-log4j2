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
package org.apache.log4j.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test configuration from XML.
 */
@UsingStatusListener
public class AsyncAppenderTest {

    private static long DEFAULT_TIMEOUT_MS = 500;

    static Stream<String> testAsyncAppender() {
        return Stream.of("/log4j1-async.xml", "/log4j1-async.properties")
                .map(config -> assertDoesNotThrow(() -> {
                    final URI uri = AsyncAppenderTest.class.getResource(config).toURI();
                    return Paths.get(uri).toString();
                }));
    }

    @ParameterizedTest
    @MethodSource
    public void testAsyncAppender(final String configLocation) throws Exception {
        try (final LoggerContext loggerContext = TestConfigurator.configure(configLocation)) {
            final Logger logger = LogManager.getLogger("test");
            logger.debug("This is a test of the root logger");
            final AppenderAdapter.Adapter adapter =
                    loggerContext.getConfiguration().getAppender("list");
            assertThat(adapter).isNotNull();
            final ListAppender appender = (ListAppender) adapter.getAppender();
            final List<String> messages = appender.getMessages(1, DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            assertThat(messages).hasSize(1);
        }
    }
}
