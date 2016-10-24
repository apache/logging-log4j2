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

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

// ============================== HOW TO RUN THIS TEST: ====================================
//
// In sampling mode (latency test):
// java -jar log4j-perf/target/benchmarks.jar ".*LoggerConfigBenchmark.*" -i 10 -f 1 -wi 5 -bm sample -tu ns
//
// Multi-threading test:
// java -jar benchmarks.jar ".*LoggerConfigBenchmark.*"  -i 10 -f 1 -wi 5 -bm sample -tu ns -t 4
//
// Usage help:
// java -jar log4j-perf/target/benchmarks.jar -help
//
@State(Scope.Benchmark)
public class LoggerConfigBenchmark {

    private final CopyOnWriteArraySet<AppenderControl> appenderSet = new CopyOnWriteArraySet<>();
    private volatile Filter filter = null;
    private final boolean additive = true;
    private final boolean includeLocation = true;
    private LoggerConfig parent;
    private final AtomicInteger counter = new AtomicInteger();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Lock shutdownLock = new ReentrantLock();
    private final Condition noLogEvents = shutdownLock.newCondition(); // should only be used when shutdown == true
    private final LogEvent LOGEVENT = createLogEventWithoutException();
    private final SimpleListAppender listAppender = new SimpleListAppender();

    private static class SimpleListAppender extends AbstractAppender {
        private static final long serialVersionUID = 1L;
        private final AtomicInteger count = new AtomicInteger();

        protected SimpleListAppender() {
            super("list", null, null);
        }

        @Override
        public void append(final LogEvent event) {
            count.incrementAndGet();
        }

        public int size() {
            return count.get();
        }
    }

    @Setup
    public void setup() {

        listAppender.start();
        final AppenderControl control = new AppenderControl(listAppender, Level.ALL, null);
        appenderSet.add(control);
    }

    @Benchmark
    public void testBaseline(final Blackhole bh) {
    }

    private static LogEvent createLogEventWithoutException() {
        return new Log4jLogEvent("a.b.c", null, "a.b.c", Level.INFO, new SimpleMessage("abc"), null, null);
    }

    @Benchmark
    public int logWithCountersAndLock() {
        log(LOGEVENT);
        return listAppender.size();
    }

    @Benchmark
    public int logWithCountersNoLocks() {
        log2(LOGEVENT);
        return listAppender.size();
    }

    @Benchmark
    public int logWithoutCountersOrLocks() {
        log3(LOGEVENT);
        return listAppender.size();
    }

    @Benchmark
    public int logWithCountersRetryAfterReconfig() {
        log4WithCounterAndFlag(LOGEVENT);
        return listAppender.size();
    }

    /**
     * Logs an event.
     *
     * @param event The log event.
     */
    public void log(final LogEvent event) {
        beforeLogEvent();
        try {
            if (!isFiltered(event)) {
                processLogEvent(event);
            }
        } finally {
            afterLogEvent();
        }
    }

    /**
     * Logs an event.
     *
     * @param event The log event.
     */
    public void log2(final LogEvent event) {
        beforeLogEvent();
        try {
            if (!isFiltered(event)) {
                processLogEvent(event);
            }
        } finally {
            afterLogEvent2();
        }
    }

    /**
     * Logs an event.
     *
     * @param event The log event.
     */
    public void log3(final LogEvent event) {
        if (!isFiltered(event)) {
            processLogEvent(event);
        }
    }
    
    volatile LoggerConfigBenchmark loggerConfig = this;

    /**
     * Logs an event.
     *
     * @param event The log event.
     */
    public void log4WithCounterAndFlag(final LogEvent event) {
        LoggerConfigBenchmark local = loggerConfig;
        while (!local.beforeLogEventCheckCounterPositive()) {
            local = loggerConfig;
        }
        try {
            local.log3(event);
        } finally {
            local.afterLogEvent2();
        }
    }

    /**
     * Determine if the LogEvent should be processed or ignored.
     * 
     * @param event The LogEvent.
     * @return true if the LogEvent should be processed.
     */
    public boolean isFiltered(final LogEvent event) {
        return filter != null && filter.filter(event) == Filter.Result.DENY;
    }

    private void beforeLogEvent() {
        counter.incrementAndGet();
    }

    private boolean beforeLogEventCheckCounterPositive() {
        return counter.incrementAndGet() > 0;
    }

    private void afterLogEvent() {
        if (counter.decrementAndGet() == 0) {
            signalCompletionIfShutdown();
        }
    }

    private void signalCompletionIfShutdown() {
        final Lock lock = shutdownLock;
        lock.lock();
        try {
            if (shutdown.get()) {
                noLogEvents.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    private void afterLogEvent2() {
        if (counter.decrementAndGet() == 0 && shutdown.get()) {
            signalCompletionIfShutdown();
        }
    }

    private void processLogEvent(final LogEvent event) {
        event.setIncludeLocation(isIncludeLocation());
        callAppenders(event);
        logParent(event);
    }

    public boolean isIncludeLocation() {
        return includeLocation;
    }

    private void logParent(final LogEvent event) {
        if (additive && parent != null) {
            parent.log(event);
        }
    }

    protected void callAppenders(final LogEvent event) {
        for (final AppenderControl control : appenderSet) {
            control.callAppender(event);
        }
    }

}
