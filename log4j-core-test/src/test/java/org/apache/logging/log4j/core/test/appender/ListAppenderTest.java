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
package org.apache.logging.log4j.core.test.appender;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ListAppender}.
 */
class ListAppenderTest {

    private static LogEvent createEvent(final int workerId, final int index) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName("worker-" + workerId)
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("event-" + workerId + "-" + index))
                .build();
    }

    private static List<String> expectedMessages(final int workerCount, final int eventsPerWorker) {
        final List<String> expected = new ArrayList<>(workerCount * eventsPerWorker);
        for (int workerId = 0; workerId < workerCount; workerId++) {
            for (int i = 0; i < eventsPerWorker; i++) {
                expected.add("event-" + workerId + "-" + i);
            }
        }
        return expected;
    }

    private static List<String> expectedEventKeys(final int workerCount, final int eventsPerWorker) {
        final List<String> expected = new ArrayList<>(workerCount * eventsPerWorker);
        for (int workerId = 0; workerId < workerCount; workerId++) {
            for (int i = 0; i < eventsPerWorker; i++) {
                expected.add("worker-" + workerId + ":event-" + workerId + "-" + i);
            }
        }
        return expected;
    }

    @Test
    void appendWithoutLayoutStoresEvents() {
        final ListAppender appender = new ListAppender("test");
        appender.start();

        appender.append(createEvent(0, 0));
        appender.append(createEvent(0, 1));

        assertThat(appender.getEvents()).hasSize(2);
        assertThat(appender.getMessages()).isEmpty();
    }

    @Test
    void appendWithLayoutStoresMessages() {
        final PatternLayout layout = PatternLayout.newBuilder().setPattern("%m").build();
        final ListAppender appender = new ListAppender("test", null, layout, false, false);
        appender.start();

        appender.append(createEvent(0, 0));
        appender.append(createEvent(0, 1));

        assertThat(appender.getMessages()).hasSize(2);
        assertThat(appender.getEvents()).isEmpty();
    }

    @Test
    void clearResetsAllCollections() {
        final ListAppender appender = new ListAppender("test");
        appender.start();

        appender.append(createEvent(0, 0));
        assertThat(appender.getEvents()).hasSize(1);

        appender.clear();

        assertThat(appender.getEvents()).isEmpty();
        assertThat(appender.getMessages()).isEmpty();
        assertThat(appender.getData()).isEmpty();
    }

    @Test
    void getMessagesWithTimeoutReturnsOnceMinSizeReached() throws InterruptedException {
        final PatternLayout layout = PatternLayout.newBuilder().setPattern("%m").build();
        final ListAppender appender = new ListAppender("test", null, layout, false, false);
        appender.start();

        // Append in a background thread, after a short delay
        final Thread producer = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            appender.append(createEvent(0, 0));
        });
        producer.start();

        final List<String> messages = appender.getMessages(1, 5, TimeUnit.SECONDS);
        producer.join();

        assertThat(messages).hasSize(1);
    }

    /**
     * Hammers {@link ListAppender#append(LogEvent)} concurrently using 10 workers each appending 1,000 deterministic
     * events, then verifies that {@link ListAppender#getEvents()} is consistent (no events were lost or duplicated).
     */
    @RepeatedTest(10)
    void appendIsThreadSafeWithoutLayout() throws InterruptedException {
        final int workerCount = 10;
        final int eventsPerWorker = 1_000;
        final List<String> expectedEventKeys = expectedEventKeys(workerCount, eventsPerWorker);

        final ListAppender appender = new ListAppender("thread-safe-test");
        appender.start();

        final ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        final CountDownLatch startGate = new CountDownLatch(1);

        for (int w = 0; w < workerCount; w++) {
            final int workerId = w;
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < eventsPerWorker; i++) {
                        appender.append(createEvent(workerId, i));
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startGate.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS))
                .as("all workers completed within timeout")
                .isTrue();

        assertThat(appender.getEvents())
                .as("all events were captured without loss or duplication")
                .hasSize(workerCount * eventsPerWorker);

        assertThat(appender.getEvents())
                .extracting(event ->
                        event.getLoggerName() + ":" + event.getMessage().getFormattedMessage())
                .as("all expected worker/message combinations are present exactly once")
                .containsExactlyInAnyOrderElementsOf(expectedEventKeys);
    }

    /**
     * Hammers {@link ListAppender#append(LogEvent)} concurrently using 10 workers each appending 1,000 deterministic
     * events with a layout, then verifies that {@link ListAppender#getMessages()} is consistent
     * (no messages were lost or duplicated).
     */
    @RepeatedTest(10)
    void appendIsThreadSafeWithLayout() throws InterruptedException {
        final int workerCount = 10;
        final int eventsPerWorker = 1_000;
        final List<String> expectedMessages = expectedMessages(workerCount, eventsPerWorker);

        final PatternLayout layout = PatternLayout.newBuilder().setPattern("%m").build();
        final ListAppender appender = new ListAppender("thread-safe-layout-test", null, layout, false, false);
        appender.start();

        final ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        final CountDownLatch startGate = new CountDownLatch(1);

        for (int w = 0; w < workerCount; w++) {
            final int workerId = w;
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < eventsPerWorker; i++) {
                        appender.append(createEvent(workerId, i));
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startGate.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS))
                .as("all workers completed within timeout")
                .isTrue();

        assertThat(appender.getMessages())
                .as("all messages were captured without loss or duplication")
                .hasSize(workerCount * eventsPerWorker);

        assertThat(appender.getMessages())
                .as("all expected messages are present exactly once")
                .containsExactlyInAnyOrderElementsOf(expectedMessages);
    }
}
