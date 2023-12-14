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
package org.apache.logging.log4j.core.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public class ExecutorServices {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Shuts down the given {@link ExecutorService} in an orderly fashion. Disables new tasks from submission and then
     * waits for existing tasks to terminate. Eventually cancels running tasks if too much time elapses.
     * <p>
     * If the timeout is 0, then a plain shutdown takes place.
     * </p>
     *
     * @param executorService
     *            the pool to shutdown.
     * @param timeout
     *            the maximum time to wait, or 0 to not wait for existing tasks to terminate.
     * @param timeUnit
     *            the time unit of the timeout argument
     * @param source
     *            use this string in any log messages.
     * @return {@code true} if the given executor terminated and {@code false} if the timeout elapsed before
     *         termination.
     */
    public static boolean shutdown(
            final ExecutorService executorService, final long timeout, final TimeUnit timeUnit, final String source) {
        if (executorService == null || executorService.isTerminated()) {
            return true;
        }
        executorService.shutdown(); // Disable new tasks from being submitted
        if (timeout > 0 && timeUnit == null) {
            throw new IllegalArgumentException(String.format(
                    "%s can't shutdown %s when timeout = %,d and timeUnit = %s.",
                    source, executorService, timeout, timeUnit));
        }
        if (timeout > 0) {
            try {
                // Wait a while for existing tasks to terminate
                if (!executorService.awaitTermination(timeout, timeUnit)) {
                    executorService.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!executorService.awaitTermination(timeout, timeUnit)) {
                        LOGGER.error(
                                "{} pool {} did not terminate after {} {}", source, executorService, timeout, timeUnit);
                    }
                    return false;
                }
            } catch (final InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                executorService.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        } else {
            executorService.shutdown();
        }
        return true;
    }

    /** No-op method which can be invoked to ensure this class has been initialized per jls-12.4.2. */
    public static void ensureInitialized() {}
}
