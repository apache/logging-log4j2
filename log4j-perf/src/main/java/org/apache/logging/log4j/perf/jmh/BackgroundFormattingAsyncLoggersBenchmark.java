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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
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
 * Tests performance of deferring formatting to a background thread.
 *
 * This test aims to show that ReusableMessage objects get a performance boost
 * on the _logging thread_ by deferring formatting operations asynchronously
 * to a background thread. Importantly, the overall amount of work is still the
 * same (the message gets formatted eventually), but the goal is to show that
 * the logging thread will not block on frequent formatting operations.
 */
// HOW TO RUN THIS TEST
// java -jar target/benchmarks.jar ".*BackgroundFormattingAsyncLoggersBenchmark.*" -f 1 -i 10 -wi 20 -bm sample -tu ns
@State(Scope.Thread)
@Threads(1)
@Fork(1)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 3, time = 3)
public class BackgroundFormattingAsyncLoggersBenchmark {

    Logger logger;

    @Param({"false", "true"})
    public String asyncFormatting;

    @Param({"ParameterizedMessageFactory", "ReusableMessageFactory"})
    public String messageFactory;

    @Setup(Level.Trial)
    public void setUp() {
        System.setProperty("log4j.configurationFile", "perf-NullAppender.xml");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("AsyncLogger.RingBufferSize", "262144");
        System.setProperty("AsyncLogger.WaitStrategy", "Yield");
        System.setProperty("log4j.format.msg.async", asyncFormatting);
        System.setProperty("log4j2.messageFactory", "org.apache.logging.log4j.message." + messageFactory);

        logger = LogManager.getLogger(getClass());
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        ((LifeCycle) LogManager.getContext(false)).stop();
    }

    private static final String COMPLEX_FORMAT_STRING =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "{}" +
                    "Proin sem purus, laoreet eget enim quis, eleifend tristique metus. " +
                    "{}" +
                    "Nam at massa et erat accumsan tempor eget sit amet dui. " +
                    "{}" +
                    "In hac habitasse platea dictumst. " +
                    "{}" +
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "{}" +
                    "Sed consectetur id massa eget dignissim. " +
                    "{}" +
                    "Mauris molestie lectus ac sem faucibus accumsan. " +
                    "{}" +
                    "Suspendisse potenti. " +
                    "{}" +
                    "Aliquam eu mollis ipsum. " +
                    "Sed pharetra sit amet dolor et facilisis. " +
                    "Morbi aliquet blandit rhoncus. " +
                    "Cras ut malesuada massa. " +
                    "Phasellus vel dui eget libero blandit sodales.";

    private enum ComplexToString {
        INSTANCE;

        private static final String[] CONTENT = {
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ",
                "Proin sem purus, laoreet eget enim quis, eleifend tristique metus. ",
                "Nam at massa et erat accumsan tempor eget sit amet dui. ",
                "In hac habitasse platea dictumst. ",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ",
                "Sed consectetur id massa eget dignissim. ",
                "Mauris molestie lectus ac sem faucibus accumsan. ",
                "Suspendisse potenti. ",
                "Aliquam eu mollis ipsum. ",
                "Sed pharetra sit amet dolor et facilisis. ",
                "Morbi aliquet blandit rhoncus. ",
                "Cras ut malesuada massa. ",
                "Phasellus vel dui eget libero blandit sodales."
        };

        private static AtomicInteger idx = new AtomicInteger(0);

        private String chooseContent() {
            return CONTENT[idx.incrementAndGet() % CONTENT.length];
        }

        @Override
        public String toString() {
            return "[ComplextToString content=" + chooseContent() + "]";
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void throughput8ComplexParams() {
        logger.info(COMPLEX_FORMAT_STRING,
            ComplexToString.INSTANCE,
            ComplexToString.INSTANCE,
            ComplexToString.INSTANCE,
            ComplexToString.INSTANCE,
            ComplexToString.INSTANCE,
            ComplexToString.INSTANCE,
            ComplexToString.INSTANCE,
            ComplexToString.INSTANCE);
    }
}
