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
package org.apache.logging.other.pkg;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;

/**
 * Test LoggerContext lookups by verifying the anchor class representing calling code.
 */
public class LoggerContextAnchorTest {
    private static final String PREFIX = "Log4jLoggerFactory.getContext() found anchor class ";

    @Test
    public void testLoggerFactoryLookupClass() {
        String fqcn = getAnchorFqcn(() -> LoggerFactory.getLogger(LoggerContextAnchorTest.class));
        assertEquals(getClass().getName(), fqcn);
    }

    @Test
    public void testLoggerFactoryLookupString() {
        String fqcn = getAnchorFqcn(() -> LoggerFactory.getLogger("custom.logger"));
        assertEquals(getClass().getName(), fqcn);
    }

    @Test
    public void testLoggerFactoryGetILoggerFactoryLookup() {
        String fqcn = getAnchorFqcn(() -> LoggerFactory.getILoggerFactory().getLogger("custom.logger"));
        assertEquals(getClass().getName(), fqcn);
    }

    private static String getAnchorFqcn(Runnable runnable) {
        List<String> results = new CopyOnWriteArrayList<>();
        StatusListener listener = new StatusListener() {
            @Override
            public void log(StatusData data) {
                String formattedMessage = data.getMessage().getFormattedMessage();
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
        StatusLogger statusLogger = StatusLogger.getLogger();
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
