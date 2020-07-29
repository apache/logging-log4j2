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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.jctools.queues.MpmcArrayQueue;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.List;

/**
 * Checks {@link PatternFormatter} performance with various StringBuilder
 * caching strategies: no-op, ThreadLocal, and JCTools MPMC queue.
 */
// ============================== HOW TO RUN THIS TEST: ====================================
// (Quick build: mvn -DskipTests=true clean package -pl log4j-perf -am )
//
// single thread:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadLocalVsPool.*" -f 1 -wi 10 -i 20 -tu ns -bm sample
//
// four threads:
// java -jar log4j-perf/target/benchmarks.jar ".*ThreadLocalVsPool.*" -f 1 -wi 10 -i 20 -tu ns -bm sample -t 4
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class ThreadLocalVsPoolBenchmark {

    private static final LogEvent LOG_EVENT = createLogEvent();

    private static final List<PatternFormatter> FORMATTERS = createFormatters();

    private static LogEvent createLogEvent() {
        final String loggerName = "name(ignored)";
        final String loggerFqcn = "com.mycom.myproject.mypackage.MyClass";
        final Level level = Level.DEBUG;
        final String messageString = "AB!(%087936DZYXQWEIOP$#^~-=/><nb"; // length=32
        final Message message = new SimpleMessage(messageString);
        final long timestamp = 12345678;
        return Log4jLogEvent
                .newBuilder()
                .setLoggerName(loggerName)
                .setLoggerFqcn(loggerFqcn)
                .setLevel(level)
                .setMessage(message)
                .setTimeMillis(timestamp)
                .build();
    }

    private static List<PatternFormatter> createFormatters() {
        final Configuration config = new DefaultConfiguration();
        final PatternParser parser = new PatternParser(config, "Converter", LogEventPatternConverter.class);
        return parser.parse("%d %5p [%t] %c{1} %X{transactionId} - %m%n", false, true);
    }

    private static abstract class StringBuilderPool {

        abstract StringBuilder acquire();

        abstract void release(StringBuilder stringBuilder);

        StringBuilder createStringBuilder() {
            return new StringBuilder(1024 * 32);
        }

    }

    private static final class AllocatePool extends StringBuilderPool {

        private static final AllocatePool INSTANCE = new AllocatePool();

        @Override
        public StringBuilder acquire() {
            return createStringBuilder();
        }

        @Override
        public void release(final StringBuilder stringBuilder) {}

    }

    private static final class ThreadLocalPool extends StringBuilderPool {

        private static final ThreadLocalPool INSTANCE = new ThreadLocalPool();

        private final ThreadLocal<StringBuilder> stringBuilderRef =
                ThreadLocal.withInitial(this::createStringBuilder);

        @Override
        public StringBuilder acquire() {
            return stringBuilderRef.get();
        }

        @Override
        public void release(final StringBuilder stringBuilder) {
            stringBuilder.setLength(0);
        }

    }

    private static final class JcPool extends StringBuilderPool {

        private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

        private static final int MPMC_REQUIRED_MIN_CAPACITY = 2;

        // Putting the under-provisioned instance to a wrapper class to prevent
        // the initialization of JcPool itself when there are insufficient CPU
        // cores.
        private enum UnderProvisionedInstanceHolder {;

            private static final JcPool INSTANCE = createInstance();

            private static JcPool createInstance() {
                if (CPU_COUNT <= MPMC_REQUIRED_MIN_CAPACITY) {
                    throw new IllegalArgumentException("insufficient CPU cores");
                }
                return new JcPool(MPMC_REQUIRED_MIN_CAPACITY);
            }

        }

        private static final JcPool RIGHT_PROVISIONED_INSTANCE =
                new JcPool(Math.max(MPMC_REQUIRED_MIN_CAPACITY, CPU_COUNT));

        private final MpmcArrayQueue<StringBuilder> stringBuilders;

        private JcPool(final int capacity) {
            this.stringBuilders = new MpmcArrayQueue<>(capacity);
        }

        @Override
        public StringBuilder acquire() {
            final StringBuilder stringBuilder = stringBuilders.poll();
            return stringBuilder != null
                    ? stringBuilder
                    : createStringBuilder();
        }

        @Override
        public void release(final StringBuilder stringBuilder) {
            stringBuilder.setLength(0);
            stringBuilders.offer(stringBuilder);
        }

    }

    @Benchmark
    public int allocate() {
        return findSerializedLength(AllocatePool.INSTANCE);
    }

    @Benchmark
    public int threadLocal() {
        return findSerializedLength(ThreadLocalPool.INSTANCE);
    }

    @Benchmark
    public int rightProvedJc() {
        return findSerializedLength(JcPool.RIGHT_PROVISIONED_INSTANCE);
    }

    @Benchmark
    public int underProvedJc() {
        return findSerializedLength(JcPool.UnderProvisionedInstanceHolder.INSTANCE);
    }

    private int findSerializedLength(final StringBuilderPool pool) {
        final StringBuilder stringBuilder = pool.acquire();
        serialize(stringBuilder);
        final int length = stringBuilder.length();
        pool.release(stringBuilder);
        return length;
    }

    private void serialize(final StringBuilder stringBuilder) {
        // noinspection ForLoopReplaceableByForEach (avoid iterator instantiation)
        for (int formatterIndex = 0; formatterIndex < FORMATTERS.size(); formatterIndex++) {
            PatternFormatter formatter = FORMATTERS.get(formatterIndex);
            formatter.format(LOG_EVENT, stringBuilder);
        }
    }

}
