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
package org.apache.logging.log4j.core.appender.rolling.action;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

/**
 * Wrapper action that schedules compression for delayed execution.
 * This action wraps another action and schedules it to be executed
 * after a random delay within a specified time window using ScheduledExecutorService.
 */
public class DelayedCompressionAction implements Action {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Action originalAction;
    private final int maxDelaySeconds;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final AtomicBoolean interrupted = new AtomicBoolean(false);
    private final AtomicReference<ScheduledFuture<?>> scheduledTask = new AtomicReference<>();

    /**
     * Creates a new DelayedCompressionAction.
     *
     * @param originalAction the action to be executed with delay
     * @param maxDelaySeconds the maximum delay in seconds (0 means immediate execution)
     * @param scheduler the ScheduledExecutorService to use for scheduling
     */
    public DelayedCompressionAction(Action originalAction, int maxDelaySeconds, ScheduledExecutorService scheduler) {
        this.originalAction = requireNonNull(originalAction, "originalAction");
        this.maxDelaySeconds = maxDelaySeconds;
        this.scheduler = requireNonNull(scheduler, "scheduler");
    }

    /**
     * Executes the action with a delay using ScheduledExecutorService.
     *
     * @return true if the action was successfully executed or scheduled
     * @throws IOException if an error occurs during execution
     */
    @Override
    public boolean execute() throws IOException {
        if (interrupted.get()) {
            return false;
        }

        // Extract source file name for logging (if possible)
        String sourceFile = extractSourceFileName(originalAction);
// Calculate delay
        int delayedSeconds = 0;
        try {
            if (maxDelaySeconds > 0) {
                delayedSeconds = ThreadLocalRandom.current().nextInt(maxDelaySeconds + 1);
                LOGGER.debug("Scheduling compression of {} with delay of {} seconds", sourceFile, delayedSeconds);
            }

            // Schedule the task if there's a delay period
            if (delayedSeconds > 0) {
                ScheduledFuture<?> future = scheduler.schedule(
                    new CompressionTask(originalAction, sourceFile),
                    delayedSeconds,
                    TimeUnit.SECONDS
                );
                scheduledTask.set(future);
                LOGGER.debug("Compression task scheduled for {} seconds from now", delayedSeconds);
                return true;
            } else {
                // Execute immediately if no delay
                return executeAction(originalAction, sourceFile);
            }
        } catch (Exception e) {
            if (delayedSeconds == 0) {
                complete.set(true);
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e);
        }
    }

    /**
     * Runs the action. This method is called when the action is executed
     * as a Runnable.
     */
    @Override
    public void run() {
        if (!interrupted.get()) {
            try {
                execute();
            } catch (final RuntimeException ex) {
                LOGGER.warn("Exception during delayed compression execution", ex);
                complete.set(true);
            } catch (final IOException ex) {
                LOGGER.warn("IOException during delayed compression execution", ex);
                complete.set(true);
            } catch (final Error e) {
                LOGGER.warn("Error during delayed compression execution", e);
                complete.set(true);
            }
        }
    }

    /**
     * Cancels the compression task.
     */
    @Override
    public void close() {
        interrupted.set(true);
        ScheduledFuture<?> future = scheduledTask.get();
        if (future != null) {
            future.cancel(true);
        }
        complete.set(true);
    }

    /**
     * Executes the compression action immediately.
     *
     * @param action the action to execute
     * @param sourceFile the source file name (for logging)
     * @return true if execution was successful
     */
    private boolean executeAction(Action action, String sourceFile) {
        try {
            LOGGER.debug("Starting delayed compression of {}", sourceFile);
            boolean success = action.execute();
            if (success) {
                LOGGER.debug("Successfully completed delayed compression of {}", sourceFile);
            } else {
                LOGGER.warn("Failed to execute delayed compression of {}", sourceFile);
            }
            return success;
        } catch (Exception e) {
            LOGGER.warn("Exception during delayed compression of {}", sourceFile, e);
            return false;
        } finally {
            complete.set(true);
        }
    }

    /**
     * Checks if the action has completed.
     *
     * @return true if the action is complete
     */
    @Override
    public boolean isComplete() {
        return complete.get();
    }

    /**
     * Attempts to extract the source file name from the action for logging purposes.
     *
     * @param action the action to extract file name from
     * @return the source file name or action toString if cannot be determined
     */
    private String extractSourceFileName(Action action) {
        // This is a best-effort attempt to get the source file name
        // The actual implementation may need to be enhanced based on the action type
        return action.toString();
    }

    /**
     * Gets the original action being wrapped.
     *
     * @return the original action
     */
    public Action getOriginalAction() {
        return originalAction;
    }

    /**
     * Gets the maximum delay in seconds for compression.
     *
     * @return the maximum delay in seconds
     */
    public int getMaxDelaySeconds() {
        return maxDelaySeconds;
    }

    /**
     * Task that executes the compression action.
     */
    private class CompressionTask implements Runnable {
        private final Action action;
        private final String sourceFile;

        CompressionTask(Action action, String sourceFile) {
            this.action = action;
            this.sourceFile = sourceFile;
        }

        @Override
        public void run() {
            if (!interrupted.get()) {
                executeAction(action, sourceFile);
            } else {
                complete.set(true);
                LOGGER.debug("Compression task was interrupted for {}", sourceFile);
            }
        }
    }

    @Override
    public String toString() {
        return "DelayedCompressionAction[originalAction=" + originalAction + ", maxDelaySeconds=" + maxDelaySeconds + "]";
    }
}

