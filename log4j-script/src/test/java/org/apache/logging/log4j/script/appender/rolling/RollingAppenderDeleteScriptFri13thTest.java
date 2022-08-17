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
package org.apache.logging.log4j.script.appender.rolling;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.script.factory.ScriptManagerFactoryImpl;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class RollingAppenderDeleteScriptFri13thTest {

    private static final String CONFIG = "log4j-rolling-with-custom-delete-script-fri13th.xml";
    private static final String DIR = "target/rolling-with-delete-script-fri13th/test";

    private final LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ScriptManagerFactoryImpl.SCRIPT_LANGUAGES, "Groovy, Javascript");
    }

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

    @Test
    public void testAppender() throws Exception {
        final var dir = Path.of(DIR);
        Files.createDirectories(dir);
        for (int i = 1; i <= 30; i++) {
            Files.createFile(dir.resolve(String.format("test-201511%02d-0.log", i)));
        }
        final LongSupplier calculateDirectorySize = () -> {
            final var size = new AtomicLong();
            doWithDirectoryListing(dir, stream -> size.set(stream.count()));
            return size.get();
        };

        doWithDirectoryListing(dir, stream -> assertThat(stream).hasSize(30));

        final Logger logger = loggerContextRule.getLogger();
        // Trigger the rollover
        while (calculateDirectorySize.getAsLong() < 32) {
            // 60+ chars per message: each message should trigger a rollover
            logger.debug("This is a very, very, very, very long test message............."); // 60+ chars:
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100)); // Allow time for rollover to complete
        }

        try (final var stream = Files.list(dir)) {
            assertThat(stream.map(path -> path.getFileName().toString())).allSatisfy(name ->
                    assertThat(name).startsWith("test-").endsWith(".log").doesNotContain("20151113"));
        }
    }

    private static void doWithDirectoryListing(final Path directory, final ThrowingConsumer<Stream<Path>> consumer) {
        try (final var directoryStream = Files.list(directory)) {
            consumer.accept(directoryStream);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
