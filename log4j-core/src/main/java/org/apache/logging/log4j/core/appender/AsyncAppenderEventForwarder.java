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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class AsyncAppenderEventForwarder extends Log4jThread {

    private static final AtomicLong THREAD_COUNTER = new AtomicLong(0);

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final AppenderControl errorAppender;

    private final List<AppenderControl> appenders;

    private final BlockingQueue<LogEvent> queue;

    private final AtomicBoolean stoppedRef;

    AsyncAppenderEventForwarder(
            final String name,
            final AppenderControl errorAppender,
            final List<AppenderControl> appenders,
            final BlockingQueue<LogEvent> queue) {
        super("AsyncAppenderEventForwarder-" + THREAD_COUNTER.incrementAndGet() + "-" + name);
        this.errorAppender = errorAppender;
        this.appenders = appenders;
        this.queue = queue;
        this.stoppedRef = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        LOGGER.trace("{} has started.", getName());
        forwardAll();
        forwardRemaining();
    }

    private void forwardAll() {
        while (!stoppedRef.get()) {
            LogEvent event;
            try {
                event = queue.take();
            } catch (final InterruptedException ignored) {
                // Restore the interrupted flag cleared when the exception is caught.
                interrupt();
                break;
            }
            event.setEndOfBatch(queue.isEmpty());
            forwardOne(event);
        }
        LOGGER.trace("{} has stopped.", getName());
    }

    private void forwardRemaining() {
        int eventCount = 0;
        while (true) {
            // Note the non-blocking Queue#poll() method!
            final LogEvent event = queue.poll();
            if (event == null) {
                break;
            }
            event.setEndOfBatch(queue.isEmpty());
            forwardOne(event);
            eventCount++;
        }
        LOGGER.trace(
                "{} has processed the last {} remaining event(s).",
                getName(), eventCount);
    }

    void forwardOne(final LogEvent event) {

        // Forward the event to all registered appenders.
        boolean succeeded = false;
        // noinspection ForLoopReplaceableByForEach (avoid iterator instantion)
        for (int appenderIndex = 0; appenderIndex < appenders.size(); appenderIndex++) {
            final AppenderControl control = appenders.get(appenderIndex);
            try {
                control.callAppender(event);
                succeeded = true;
            } catch (final Throwable ignored) {
                // If no appender is successful, the error appender will get it.
            }
        }

        // Fallback to the error appender if none has succeeded so far.
        if (!succeeded && errorAppender != null) {
            try {
                errorAppender.callAppender(event);
            } catch (final Throwable ignored) {
                // If the error appender also fails, there is nothing further
                // we can do about it.
            }
        }

    }

    void stop(final long timeoutMillis) throws InterruptedException {

        // Mark the completion, if necessary.
        final boolean stopped = stoppedRef.compareAndSet(false, true);
        if (stopped) {
            LOGGER.trace("{} is signaled to stop.", getName());
        }

        // There is a slight chance that the thread is not started yet, wait for
        // it to run. Otherwise, interrupt+join might block.
        // noinspection StatementWithEmptyBody
        while (Thread.State.NEW.equals(getState()));

        // Interrupt the thread. (Note that there is neither a check on
        // "stopped", nor a synchronization here. It is okay to interrupt
        // concurrently and/or multiple times.)
        interrupt();

        // Wait for the completion.
        join(timeoutMillis);

    }

}
