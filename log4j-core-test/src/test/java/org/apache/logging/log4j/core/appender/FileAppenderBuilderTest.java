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
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.Test;

public class FileAppenderBuilderTest {

    /**
     * Tests https://issues.apache.org/jira/browse/LOG4J2-1620
     */
    @Test
    public void testDefaultImmediateFlush() {
        assertTrue(FileAppender.newBuilder().isImmediateFlush());
    }

    /**
     * Tests whether a missing name or file name causes the builder to return
     * {@code null}.
     */
    @Test
    public void testConstraints() {
        final AtomicInteger counter = new AtomicInteger();
        final StatusListener listener = new StatusListener() {

            @Override
            public void close() throws IOException {}

            @Override
            public void log(final StatusData data) {
                counter.incrementAndGet();
            }

            @Override
            public Level getStatusLevel() {
                return Level.ERROR;
            }
        };
        try {
            StatusLogger.getLogger().registerListener(listener);
            FileAppender appender = FileAppender.newBuilder().build();
            assertNull(appender);
            assertTrue(counter.getAndSet(0) > 0);
            appender = FileAppender.newBuilder()
                    .withFileName("target/FileAppenderBuilderTest.log")
                    .build();
            assertNull(appender);
            assertTrue(counter.getAndSet(0) > 0);
            appender = FileAppender.newBuilder().setName("FILE").build();
            assertNull(appender);
            assertTrue(counter.getAndSet(0) > 0);
            appender = FileAppender.newBuilder()
                    .setName("FILE")
                    .withFileName("target/FileAppenderBuilderTest.log")
                    .build();
            assertNotNull(appender);
            assertTrue(counter.get() == 0);
        } catch (NullPointerException e) {
            // thrown if no filename is provided
            fail(e);
        } finally {
            StatusLogger.getLogger().removeListener(listener);
        }
    }
}
