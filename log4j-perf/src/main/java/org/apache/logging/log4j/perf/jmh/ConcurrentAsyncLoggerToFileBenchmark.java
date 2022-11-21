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
package org.apache.logging.log4j.perf.jmh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicy;
import org.apache.logging.log4j.core.async.EventRoute;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.perf.util.BenchmarkMessageParams;
import org.apache.logging.log4j.spi.LoggingSystemProperties;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Tests Log4j2 Async Loggers performance with many threads producing events quickly while the background
 * thread persists events to disk.
 * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-2606">LOG4J2-2606</a>
 */
@Fork(1)
@State(Scope.Benchmark)
@Warmup(iterations = 1, time = 3)
@Measurement(iterations = 3, time = 15)
public class ConcurrentAsyncLoggerToFileBenchmark {

    @Benchmark
    @Threads(32)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void concurrentLoggingThreads(BenchmarkState state) {
        state.logger.info(BenchmarkMessageParams.TEST);
    }

    @Benchmark
    @Threads(1)
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void singleLoggingThread(BenchmarkState state) {
        state.logger.info(BenchmarkMessageParams.TEST);
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {

        @Param({"ENQUEUE", "ENQUEUE_UNSYNCHRONIZED", "SYNCHRONOUS"})
        private QueueFullPolicy queueFullPolicy;

        @Param({"ASYNC_CONTEXT", "ASYNC_CONFIG"})
        private AsyncLoggerType asyncLoggerType;

        private Logger logger;

        @Setup
        public final void before() throws IOException {
            Files.deleteIfExists(Path.of("target", "ConcurrentAsyncLoggerToFileBenchmark.log"));
            System.setProperty(LoggingSystemProperties.SYSTEM_IS_WEBAPP, "false");
            asyncLoggerType.setProperties();
            queueFullPolicy.setProperties();
            logger = LogManager.getLogger(ConcurrentAsyncLoggerToFileBenchmark.class);
        }

        @TearDown
        public final void after() throws IOException {
            ((LifeCycle) LogManager.getContext(false)).stop();
            Files.deleteIfExists(Path.of("target", "ConcurrentAsyncLoggerToFileBenchmark.log"));
            logger = null;
        }

        @SuppressWarnings("unused") // Used by JMH
        public enum QueueFullPolicy {
            ENQUEUE(Map.of(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY, "Default")),
            ENQUEUE_UNSYNCHRONIZED(Map.of(
                Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY, "Default",
                Log4jProperties.ASYNC_LOGGER_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL, "false",
                Log4jProperties.ASYNC_CONFIG_SYNCHRONIZE_ENQUEUE_WHEN_QUEUE_FULL, "false"
            )),
            SYNCHRONOUS(Map.of(Log4jProperties.ASYNC_LOGGER_QUEUE_FULL_POLICY,
                    SynchronousAsyncQueueFullPolicy.class.getName()));

            private final Map<String, String> properties;

            QueueFullPolicy(Map<String, String> properties) {
                this.properties = properties;
            }

            void setProperties() {
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            }
        }

        @SuppressWarnings("unused") // Used by JMH
        public enum AsyncLoggerType {
            ASYNC_CONTEXT,
            ASYNC_CONFIG;
            // TODO(ckozak): Consider adding ASYNC_APPENDER

            void setProperties() {
                switch (this) {
                    case ASYNC_CONTEXT:
                        System.setProperty(Log4jProperties.CONFIG_LOCATION, "ConcurrentAsyncLoggerToFileBenchmark.xml");
                        System.setProperty(Log4jProperties.CONTEXT_SELECTOR_CLASS_NAME,
                                "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
                        break;
                    case ASYNC_CONFIG:
                        System.setProperty(Log4jProperties.CONFIG_LOCATION,
                                "ConcurrentAsyncLoggerToFileBenchmark-asyncConfig.xml");
                        break;
                    default:
                        throw new IllegalStateException("Unknown type: " + this);
                }
            }
        }
    }

    public static final class SynchronousAsyncQueueFullPolicy implements AsyncQueueFullPolicy {
        @Override
        public EventRoute getRoute(final long backgroundThreadId, final Level level) {
            return EventRoute.SYNCHRONOUS;
        }
    }
}
