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
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Objects.requireNonNull;

/**
 * Wrapper action that schedules compression for defferred execution.
 * This action wraps another action and schedules it to be executed
 * after a random defer within a specified time window.
 */
public class DeferredCompressionAction implements Action {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Action originalAction;
    private final int maxDeferSeconds;
    private boolean complete = false;
    private boolean interrupted = false;

    /**
     * Creates a new DelayedCompressionAction.
     *
     * @param originalAction the action to be executed with defer
     * @param maxDeferSeconds the maximum defer in seconds (0 means immediate execution)
     */
    public DeferredCompressionAction(Action originalAction, int maxDeferSeconds) {
        this.originalAction = requireNonNull(originalAction, "originalAction");
        this.maxDeferSeconds = maxDeferSeconds;
    }

    /**
     * Executes the action with a defer using sleep.
     *
     * @return true if the action was successfully executed
     * @throws IOException if an error occurs during execution
     */
    @Override
    public boolean execute() throws IOException {
        if (interrupted) {
            return false;
        }

        try {
            // Extract source file name for logging (if possible)
            String sourceFile = extractSourceFileName(originalAction);

            // Calculate defer
            int deferredSeconds = 0;
            if (maxDeferSeconds > 0) {
                deferredSeconds = ThreadLocalRandom.current().nextInt(maxDeferSeconds + 1);
                LOGGER.debug("Scheduling compression of {} with defer of {} seconds", sourceFile, deferredSeconds);
            }

            // Sleep for the defer period
            if (deferredSeconds > 0) {
                try {
                    Thread.sleep(deferredSeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    interrupted = true;
                    LOGGER.warn("Delayed compression interrupted for {}", sourceFile);
                    return false;
                }
            }

            // Execute the original action after defer
            if (!interrupted) {
                executeAction(originalAction, sourceFile);
            }

            return !interrupted;
        } catch (Exception e) {
            complete = true;
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
        if (!interrupted) {
            try {
                execute();
            } catch (final RuntimeException ex) {
                LOGGER.warn("Exception during deferred compression execution", ex);
            } catch (final IOException ex) {
                LOGGER.warn("IOException during deferred compression execution", ex);
            } catch (final Error e) {
                LOGGER.warn("Error during deferred compression execution", e);
            }
            complete = true;
        }
    }

    /**
     * Cancels the compression task by interrupting the current thread.
     */
    @Override
    public void close() {
        interrupted = true;
        // Interrupt the current thread if it's sleeping
        Thread.currentThread().interrupt();
    }

    /**
     * Executes the compression action.
     *
     * @param action the action to execute
     * @param sourceFile the source file name (for logging)
     */
    private void executeAction(Action action, String sourceFile) {
        try {
            LOGGER.debug("Starting deferred compression of {}", sourceFile);
            boolean success = action.execute();
            if (success) {
                LOGGER.debug("Successfully completed deferred compression of {}", sourceFile);
            } else {
                LOGGER.warn("Failed to execute deferred compression of {}", sourceFile);
            }
        } catch (Exception e) {
            LOGGER.warn("Exception during deferred compression of {}", sourceFile, e);
        } finally {
            // 在finally块中设置complete状态
            complete = true;
        }
    }

    /**
     * Checks if the action has completed.
     *
     * @return true if the action is complete
     */
    @Override
    public boolean isComplete() {
        return complete;
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
     * Gets the maximum defer in seconds for compression.
     *
     * @return the maximum defer in seconds
     */
    public int getMaxDeferSeconds() {
        return maxDeferSeconds;
    }

    @Override
    public String toString() {
        return "DeferredCompressionAction[originalAction=" + originalAction + ", maxDeferSeconds=" + maxDeferSeconds + "]";
    }
}

