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
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Supplier;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

class AsyncAppenderEventForwarder {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final AppenderControl errorAppender;

    private final List<AppenderControl> appenders;

    private final BlockingQueue<LogEvent> queue;

    private final ThreadFactory threadFactory;

    private volatile boolean stopped;

    private volatile Thread activeThread;

    AsyncAppenderEventForwarder(
            final AppenderControl errorAppender,
            final List<AppenderControl> appenders,
            final BlockingQueue<LogEvent> queue,
            final ThreadFactory threadFactory) {
        this.errorAppender = errorAppender;
        this.appenders = appenders;
        this.queue = queue;
        this.threadFactory = threadFactory;
        this.stopped = true;
        this.activeThread = null;
    }

    synchronized void start() {
        if (stopped) {
            final Thread thread = createForwarder();
            stopped = false;
            activeThread = thread;
            thread.start();
        }
    }

    private Thread createForwarder() {

        // Create the holder for the thread supplier. (We need this holder to
        // avoid recursive calls to createForwarder() in the uncaught exception
        // handler, since this might result in a stack overflow.)
        @SuppressWarnings("unchecked")
        final Supplier<Thread>[] threadSupplierRef = new Supplier[1];

        // Create the uncaught exception handler, which respawns the forwarder
        // upon unexpected termination.
        final Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
                (final Thread ignored, final Throwable error) -> {

                    // In a synchronized block, determine if respawning should commence.
                    final Thread nextActiveThread;
                    synchronized (AsyncAppenderEventForwarder.this) {
                        if (stopped) {
                            nextActiveThread = null;
                        } else {
                            nextActiveThread = threadSupplierRef[0].get();
                            activeThread = nextActiveThread;
                        }
                    }

                    // Execute the result determined above — no synchronization
                    // is needed at this stage.
                    if (nextActiveThread == null) {
                        LOGGER.warn("forwarder has failed", error);
                    } else {
                        LOGGER.warn("respawning failed forwarder", error);
                        nextActiveThread.start();
                    }

                };

        // Create the thread supplier injecting the above created uncaught
        // exception handler.
        final Supplier<Thread> threadSupplier = () -> {
            final Thread thread =
                    threadFactory.newThread(this::forwardAll);
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            return thread;
        };

        // Fill in the holder.
        threadSupplierRef[0] = threadSupplier;

        // Return the initial thread.
        return threadSupplierRef[0].get();

    }

    private void forwardAll() {
        // Here we don't need to check against the "stopped" flag, since it is
        // only used for determining if respawning should take place.
        final Thread thread = Thread.currentThread();
        while (!thread.isInterrupted()) {
            LogEvent event;
            try {
                event = queue.take();
            } catch (final InterruptedException ignored) {
                // Restore the interrupted flag cleared when the exception is caught.
                thread.interrupt();
                break;
            }
            event.setEndOfBatch(queue.isEmpty());
            forwardOne(event);
        }
    }

    synchronized long getActiveThreadId() {
        return stopped ? -1 : activeThread.getId();
    }

    void stop(final long timeoutMillis) throws InterruptedException {

        // Disable respawning, if necessary.
        final Thread lastActiveThread;
        synchronized (this) {
            if (stopped) {
                return;
            } else {
                lastActiveThread = activeThread;
                activeThread = null;
                stopped = true;
            }
        }

        // Put the termination sequence into a thread.
        final Thread stopper = threadFactory.newThread(() -> {

            // Update the thread name to reflect the purpose.
            final Thread thread = Thread.currentThread();
            final String threadName = String.format("%s-Stopper", thread.getName());
            thread.setName(threadName);

            // There is a slight chance that the last active forwarder thread is
            // not started yet, wait for it to get picked up by a thread.
            // Otherwise, interrupt+join will block.
            // noinspection LoopConditionNotUpdatedInsideLoop, StatementWithEmptyBody
            while (Thread.State.NEW.equals(lastActiveThread.getState()));

            // Interrupt the last active forwarder.
            lastActiveThread.interrupt();

            // Forward any last remaining events.
            forwardRemaining();

            // Wait for the last active forwarder to stop.
            try {
                lastActiveThread.join();
            } catch (final InterruptedException ignored) {
                // Restore the interrupted flag cleared when the exception is caught.
                thread.interrupt();
            }

        });

        // Commence the termination sequence and wait at most for the given amount of time.
        stopper.start();
        stopper.join(timeoutMillis);

    }

    private void forwardRemaining() {
        int eventCount = 0;
        while (true) {
            final LogEvent event = queue.poll();
            if (event == null) {
                break;
            }
            event.setEndOfBatch(queue.isEmpty());
            forwardOne(event);
            eventCount++;
        }
        LOGGER.trace("AsyncAppenderThreadTask processed the last {} remaining event(s).", eventCount);
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
            } catch (final Exception ignored) {
                // If no appender is successful, the error appender will get it.
            }
        }

        // Fallback to the error appender if none has succeeded so far.
        if (!succeeded && errorAppender != null) {
            try {
                errorAppender.callAppender(event);
            } catch (final Exception ignored) {
                // If the error appender also fails, there is nothing further
                // we can do about it.
            }
        }

    }

}
