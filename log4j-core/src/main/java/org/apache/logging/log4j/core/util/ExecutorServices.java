package org.apache.logging.log4j.core.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public class ExecutorServices {

    private static final Logger LOGGER = StatusLogger.getLogger();
    
    /**
     * Shuts down the given pool.
     * 
     * @param pool
     *            the pool to shutdown.
     * @param timeout
     *            the maximum time to wait
     * @param source 
     *            use this string in any log messages.
     * @param timeUnit
     *            the time unit of the timeout argument
     * @return {@code true} if the given executor terminated and {@code false} if the timeout elapsed before termination.
     */
    public static boolean shutdown(ExecutorService pool, long timeout, TimeUnit timeUnit, String source) {
        pool.shutdown(); // Disable new tasks from being submitted
        if (timeout > 0 && timeUnit == null) {
            throw new IllegalArgumentException(
                    String.format("%s can't shutdown %s when timeout = %,d and timeUnit = %s.",
                            source, pool, timeout, timeUnit));
        }
        if (timeout > 0) {
            try {
                // Wait a while for existing tasks to terminate
                if (!pool.awaitTermination(timeout, timeUnit)) {
                    pool.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!pool.awaitTermination(timeout, timeUnit)) {
                        LOGGER.error("{} pool {} did not terminate after {} {}", source, pool, timeout, timeUnit);
                    }
                    return false;
                }
            } catch (InterruptedException ie) {
                // (Re-)Cancel if current thread also interrupted
                pool.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
        return true;
    }

}
