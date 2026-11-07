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
package org.apache.logging.log4j.jul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.ConsoleHandler;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MutatorAccessApiLoggerTest {
    private static final String PROPERTY = "log4j2.julMutatorAccess";
    private final List<String> warnings = new CopyOnWriteArrayList<>();
    private final StatusListener listener = new StatusListener() {
        @Override
        public void log(final StatusData data) {
            final String message = data.getMessage().getFormattedMessage();
            if (message.startsWith("Ignoring call to `j.u.l.Logger.")) {
                warnings.add(message);
            }
        }

        @Override
        public Level getStatusLevel() {
            return Level.WARN;
        }

        @Override
        public void close() {}
    };
    private String previous;
    private ApiLogger logger;

    @BeforeEach
    void setUp() {
        previous = System.getProperty(PROPERTY);
        logger = new ApiLogger((ExtendedLogger) LogManager.getLogger(getClass()));
        StatusLogger.getLogger().registerListener(listener);
    }

    @AfterEach
    void tearDown() {
        StatusLogger.getLogger().removeListener(listener);
        if (previous == null) {
            System.clearProperty(PROPERTY);
        } else {
            System.setProperty(PROPERTY, previous);
        }
    }

    @Test
    void warnReportsEveryMutatorAndCaller() {
        System.setProperty(PROPERTY, "WARN");
        final ConsoleHandler handler = new ConsoleHandler();
        try {
            logger.setLevel(java.util.logging.Level.FINE);
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
            logger.removeHandler(handler);
            assertEquals(4, warnings.size());
            for (String warning : warnings) {
                assertTrue(warning.contains(getClass().getName()), warning);
                assertTrue(warning.contains("Please report this to the maintainer"));
            }
            assertEquals(java.util.logging.Level.FINE, logger.getLevel());
            assertEquals(0, logger.getHandlers().length);
        } finally {
            handler.close();
        }
    }

    @Test
    void ignoreSuppressesWarningsAndPreservesJulState() {
        System.setProperty(PROPERTY, "IGNORE");
        logger.setLevel(java.util.logging.Level.OFF);
        logger.setUseParentHandlers(false);
        final ConsoleHandler handler = new ConsoleHandler();
        try {
            logger.addHandler(handler);
            assertEquals(1, logger.getHandlers().length);
            logger.removeHandler(handler);
            assertTrue(warnings.isEmpty());
            assertEquals(java.util.logging.Level.OFF, logger.getLevel());
        } finally {
            handler.close();
        }
    }

    @Test
    void defaultWarnsOnceAcrossConcurrentCallsAndLoggerInstances() {
        System.clearProperty(PROPERTY);
        final ApiLogger other = new ApiLogger((ExtendedLogger) LogManager.getLogger("other"));
        IntStream.range(0, 100).parallel().forEach(i -> {
            logger.setLevel(java.util.logging.Level.INFO);
            other.setUseParentHandlers(false);
        });
        assertEquals(1, warnings.size());
        System.setProperty(PROPERTY, "warn-once");
        other.setLevel(java.util.logging.Level.OFF);
        System.setProperty(PROPERTY, "unrecognized");
        other.setLevel(java.util.logging.Level.INFO);
        assertEquals(1, warnings.size());
    }
}
