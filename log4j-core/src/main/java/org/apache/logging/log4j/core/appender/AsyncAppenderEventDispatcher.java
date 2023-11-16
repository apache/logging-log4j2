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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.status.StatusLogger;

class AsyncAppenderEventDispatcher extends Log4jThread {

    private static final LogEvent STOP_EVENT = new Log4jLogEvent();

    private static final AtomicLong THREAD_COUNTER = new AtomicLong(0);

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final AppenderControl errorAppender;

    private final List<AppenderControl> appenders;

    private final BlockingQueue<LogEvent> queue;

    private final AtomicBoolean stoppedRef;

    AsyncAppenderEventDispatcher(
            final String name,
            final AppenderControl errorAppender,
            final List<AppenderControl> appenders,
            final BlockingQueue<LogEvent> queue) {
        super("AsyncAppenderEventDispatcher-" + THREAD_COUNTER.incrementAndGet() + "-" + name);
        this.setDaemon(true);
        this.errorAppender = errorAppender;
        this.appenders = appenders;
        this.queue = queue;
        this.stoppedRef = new AtomicBoolean();
    }

    /**
     * Gets all Appenders.
     *
     * @return a list of Appenders.
     */
    List<Appender> getAppenders() {
        return appenders.stream().map(AppenderControl::getAppender).collect(Collectors.toList());
    }

    @Override
    public void run() {
        LOGGER.trace("{} has started.", getName());
        dispatchAll();
        dispatchRemaining();
    }

    private void dispatchAll() {
        while (!stoppedRef.get()) {
            LogEvent event;
            try {
                event = queue.take();
            } catch (final InterruptedException ignored) {
                // Restore the interrupted flag cleared when the exception is caught.
                interrupt();
                break;
            }
            if (event == STOP_EVENT) {
                break;
            }
            event.setEndOfBatch(queue.isEmpty());
            dispatch(event);
        }
        LOGGER.trace("{} has stopped.", getName());
    }

    private void dispatchRemaining() {
        int eventCount = 0;
        while (true) {
            // Note the non-blocking Queue#poll() method!
            final LogEvent event = queue.poll();
            if (event == null) {
                break;
            }
            // Allow events that managed to be submitted after the sentinel.
            if (event == STOP_EVENT) {
                continue;
            }
            event.setEndOfBatch(queue.isEmpty());
            dispatch(event);
            eventCount++;
        }
        LOGGER.trace("{} has processed the last {} remaining event(s).", getName(), eventCount);
    }

    /**
     * Dispatches the given {@code event} to the registered appenders <b>in the
     * current thread</b>.
     */
    void dispatch(final LogEvent event) {

        // Dispatch the event to all registered appenders.
        boolean succeeded = false;
        // noinspection ForLoopReplaceableByForEach (avoid iterator instantion)
        for (int appenderIndex = 0; appenderIndex < appenders.size(); appenderIndex++) {
            final AppenderControl control = appenders.get(appenderIndex);
            try {
                control.callAppender(event);
                succeeded = true;
            } catch (final Throwable error) {
                // If no appender is successful, the error appender will get it.
                // It is okay to simply log it here.
                LOGGER.trace("{} has failed to call appender {}", getName(), control.getAppenderName(), error);
            }
        }

        // Fallback to the error appender if none has succeeded so far.
        if (!succeeded && errorAppender != null) {
            try {
                errorAppender.callAppender(event);
            } catch (final Throwable error) {
                // If the error appender also fails, there is nothing further
                // we can do about it.
                LOGGER.trace(
                        "{} has failed to call the error appender {}",
                        getName(),
                        errorAppender.getAppenderName(),
                        error);
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
        while (Thread.State.NEW.equals(getState()))
            ;

        // Enqueue the stop event, if there is sufficient room; otherwise,
        // fallback to interruption. (We should avoid interrupting the thread if
        // at all possible due to the subtleties of Java interruption, which
        // will actually close sockets if any blocking operations are in
        // progress! This means a socket appender may surprisingly fail to
        // deliver final events. I recall some oddities with file I/O as well.
        // — ckozak)
        final boolean added = queue.offer(STOP_EVENT);
        if (!added) {
            interrupt();
        }

        // Wait for the completion.
        join(timeoutMillis);
    }
}
