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
package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.layout.template.json.util.RecyclerFactories;
import org.apache.logging.log4j.layout.template.json.util.RecyclerFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class JsonTemplateLayoutConcurrentEncodeTest {

    private static class ConcurrentAccessError extends RuntimeException {

        public static final long serialVersionUID = 0;

        private ConcurrentAccessError(final int concurrentAccessCount) {
            super("concurrentAccessCount=" + concurrentAccessCount);
        }

    }

    private static class ConcurrentAccessDetectingByteBufferDestination
            extends BlackHoleByteBufferDestination {

        private final AtomicInteger concurrentAccessCounter = new AtomicInteger(0);

        ConcurrentAccessDetectingByteBufferDestination() {
            super(2_000);
        }

        @Override
        public ByteBuffer getByteBuffer() {
            final int concurrentAccessCount = concurrentAccessCounter.incrementAndGet();
            if (concurrentAccessCount > 1) {
                throw new ConcurrentAccessError(concurrentAccessCount);
            }
            try {
                return super.getByteBuffer();
            } finally {
                concurrentAccessCounter.decrementAndGet();
            }
        }

        @Override
        public ByteBuffer drain(final ByteBuffer byteBuffer) {
            final int concurrentAccessCount = concurrentAccessCounter.incrementAndGet();
            if (concurrentAccessCount > 1) {
                throw new ConcurrentAccessError(concurrentAccessCount);
            }
            try {
                return super.drain(byteBuffer);
            } finally {
                concurrentAccessCounter.decrementAndGet();
            }
        }

        @Override
        public void writeBytes(final ByteBuffer byteBuffer) {
            final int concurrentAccessCount = concurrentAccessCounter.incrementAndGet();
            if (concurrentAccessCount > 1) {
                throw new ConcurrentAccessError(concurrentAccessCount);
            }
            try {
                super.writeBytes(byteBuffer);
            } finally {
                concurrentAccessCounter.decrementAndGet();
            }
        }

        @Override
        public void writeBytes(final byte[] buffer, final int offset, final int length) {
            int concurrentAccessCount = concurrentAccessCounter.incrementAndGet();
            if (concurrentAccessCount > 1) {
                throw new ConcurrentAccessError(concurrentAccessCount);
            }
            try {
                super.writeBytes(buffer, offset, length);
            } finally {
                concurrentAccessCounter.decrementAndGet();
            }
        }

    }

    private static final LogEvent[] LOG_EVENTS = createMessages();

    private static LogEvent[] createMessages() {
        final int messageCount = 1_000;
        final LogEvent[] logEvents = new LogEvent[messageCount];
        LogEventFixture
                .createLiteLogEvents(messageCount)
                .toArray(logEvents);
        return logEvents;
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "dummy",
            "threadLocal",
            "queue:supplier=java.util.concurrent.ArrayBlockingQueue.new",
            "queue:supplier=org.jctools.queues.MpmcArrayQueue.new"
    })
    void test_concurrent_encode(final String recyclerFactorySpec) {
        final RecyclerFactory recyclerFactory = RecyclerFactories.ofSpec(recyclerFactorySpec);
        final AtomicReference<Exception> encodeFailureRef = new AtomicReference<>(null);
        produce(recyclerFactory, encodeFailureRef);
        Assertions.assertThat(encodeFailureRef.get()).isNull();
    }

    private void produce(
            final RecyclerFactory recyclerFactory,
            final AtomicReference<Exception> encodeFailureRef) {
        final int threadCount = 10;
        final JsonTemplateLayout layout = createLayout(recyclerFactory);
        final ByteBufferDestination destination =
                new ConcurrentAccessDetectingByteBufferDestination();
        final AtomicLong encodeCounter = new AtomicLong(0);
        final List<Thread> workers = IntStream
                .range(0, threadCount)
                .mapToObj((final int threadIndex) ->
                        createWorker(
                                layout,
                                destination,
                                encodeFailureRef,
                                encodeCounter,
                                threadIndex))
                .collect(Collectors.toList());
        workers.forEach(Thread::start);
        workers.forEach((final Thread worker) -> {
            try {
                worker.join();
            } catch (final InterruptedException ignored) {
                System.err.format("join to %s interrupted%n", worker.getName());
            }
        });
    }

    private static JsonTemplateLayout createLayout(final RecyclerFactory recyclerFactory) {
        final Configuration config = new DefaultConfiguration();
        return JsonTemplateLayout
                .newBuilder()
                .setConfiguration(config)
                .setEventTemplate("{\"message\": \"${json:message}\"}")
                .setStackTraceEnabled(false)
                .setLocationInfoEnabled(false)
                .setRecyclerFactory(recyclerFactory)
                .build();
    }

    private Thread createWorker(
            final JsonTemplateLayout layout,
            final ByteBufferDestination destination,
            final AtomicReference<Exception> encodeFailureRef,
            final AtomicLong encodeCounter,
            final int threadIndex) {
        final int maxEncodeCount = 1_000;
        final String threadName = String.format("Worker-%d", threadIndex);
        return new Thread(
                () -> {
                    try {
                        for (int logEventIndex = threadIndex % LOG_EVENTS.length;
                             encodeFailureRef.get() == null && encodeCounter.incrementAndGet() < maxEncodeCount;
                             logEventIndex = (logEventIndex + 1) % LOG_EVENTS.length) {
                            final LogEvent logEvent = LOG_EVENTS[logEventIndex];
                            layout.encode(logEvent, destination);
                        }
                    } catch (final Exception error) {
                        final boolean succeeded = encodeFailureRef.compareAndSet(null, error);
                        if (succeeded) {
                            System.err.format("%s failed%n", threadName);
                            error.printStackTrace(System.err);
                        }
                    }
                },
                threadName);
    }

}
