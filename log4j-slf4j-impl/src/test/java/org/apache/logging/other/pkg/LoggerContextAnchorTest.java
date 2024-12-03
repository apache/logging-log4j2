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
package org.apache.logging.other.pkg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Test LoggerContext lookups by verifying the anchor class representing calling code.
 */
public class LoggerContextAnchorTest {
    private static final String PREFIX = "Log4jLoggerFactory.getContext() found anchor class ";

    @Test
    public void testLoggerFactoryLookupClass() {
        final String fqcn = getAnchorFqcn(() -> LoggerFactory.getLogger(LoggerContextAnchorTest.class));
        assertEquals(getClass().getName(), fqcn);
    }

    @Test
    public void testLoggerFactoryLookupString() {
        final String fqcn = getAnchorFqcn(() -> LoggerFactory.getLogger("custom.logger"));
        assertEquals(getClass().getName(), fqcn);
    }

    @Test
    public void testLoggerFactoryGetILoggerFactoryLookup() {
        final String fqcn =
                getAnchorFqcn(() -> LoggerFactory.getILoggerFactory().getLogger("custom.logger"));
        assertEquals(getClass().getName(), fqcn);
    }

    private static String getAnchorFqcn(final Runnable runnable) {
        final List<String> results = new CopyOnWriteArrayList<>();
        final StatusListener listener = new StatusListener() {
            @Override
            public void log(final StatusData data) {
                final String formattedMessage = data.getMessage().getFormattedMessage();
                if (formattedMessage.startsWith(PREFIX)) {
                    results.add(formattedMessage.substring(PREFIX.length()));
                }
            }

            @Override
            public Level getStatusLevel() {
                return Level.TRACE;
            }

            @Override
            public void close() {
                // nop
            }
        };
        final StatusLogger statusLogger = StatusLogger.getLogger();
        statusLogger.registerListener(listener);
        try {
            runnable.run();
            if (results.isEmpty()) {
                throw new AssertionError("Failed to locate an anchor lookup status message");
            }
            if (results.size() > 1) {
                throw new AssertionError("Found multiple anchor lines: " + results);
            }
            return results.get(0);
        } finally {
            statusLogger.removeListener(listener);
        }
    }
}
