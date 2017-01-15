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
package org.apache.logging.log4j.core.config;

import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
public class ConfigurationScheduler extends AbstractLifeCycle {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final String SIMPLE_NAME = "Log4j2 " + ConfigurationScheduler.class.getSimpleName();
    private static final int MAX_SCHEDULED_ITEMS = 5;
    private ScheduledExecutorService executorService;

    private int scheduledItems = 0;

    @Override
    public void start() {
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        if (isExecutorServiceSet()) {
            LOGGER.debug("{} shutting down threads in {}", SIMPLE_NAME, getExecutorService());
            executorService.shutdown();
            try {
                executorService.awaitTermination(timeout, timeUnit);
            } catch (final InterruptedException ie) {
                executorService.shutdownNow();
                try {
                    executorService.awaitTermination(timeout, timeUnit);
                } catch (final InterruptedException inner) {
                    LOGGER.warn("ConfigurationScheduler stopped but some scheduled services may not have completed.");
                }
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
        setStopped();
        return true;
    }

    public boolean isExecutorServiceSet() {
        return executorService != null;
    }

    /**
     * Increment the number of threads in the pool.
     */
    public void incrementScheduledItems() {
        if (isExecutorServiceSet()) {
            LOGGER.error("{} attempted to increment scheduled items after start", SIMPLE_NAME);
        } else {
            ++scheduledItems;
        }
    }

    /**
     * Decrement the number of threads in the pool
     */
    public void decrementScheduledItems() {
        if (!isStarted() && scheduledItems > 0) {
            --scheduledItems;
        }
    }

    /**
     * Creates and executes a ScheduledFuture that becomes enabled after the given delay.
     * @param <V> The result type returned by this Future
     * @param callable the function to execute.
     * @param delay the time from now to delay execution.
     * @param unit the time unit of the delay parameter.
     * @return a ScheduledFuture that can be used to extract result or cancel.
     *
     */
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return getExecutorService().schedule(callable, delay, unit);
    }

    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     * @param command the task to execute.
     * @param delay the time from now to delay execution.
     * @param unit the time unit of the delay parameter.
     * @return a ScheduledFuture representing pending completion of the task and whose get() method will return null
     * upon completion.
     */
    public ScheduledFuture<?> schedule(final Runnable command, final long delay, final TimeUnit unit) {
        return getExecutorService().schedule(command, delay, unit);
    }


    /**
     * Creates and executes an action that first based on a cron expression.
     * @param cronExpression the cron expression describing the schedule.
     * @param command The Runnable to run,
     * @return a ScheduledFuture representing the next time the command will run.
     */
    public CronScheduledFuture<?> scheduleWithCron(final CronExpression cronExpression, final Runnable command) {
        return scheduleWithCron(cronExpression, new Date(), command);
    }

    /**
     * Creates and executes an action that first based on a cron expression.
     * @param cronExpression the cron expression describing the schedule.
     * @param startDate The time to use as the time to begin the cron expression. Defaults to the current date and time.
     * @param command The Runnable to run,
     * @return a ScheduledFuture representing the next time the command will run.
     */
    public CronScheduledFuture<?> scheduleWithCron(final CronExpression cronExpression, final Date startDate, final Runnable command) {
        final Date fireDate = cronExpression.getNextValidTimeAfter(startDate == null ? new Date() : startDate);
        final CronRunnable runnable = new CronRunnable(command, cronExpression);
        final ScheduledFuture<?> future = schedule(runnable, nextFireInterval(fireDate), TimeUnit.MILLISECONDS);
        final CronScheduledFuture<?> cronScheduledFuture = new CronScheduledFuture<>(future, fireDate);
        runnable.setScheduledFuture(cronScheduledFuture);
        LOGGER.debug("Scheduled cron expression {} to fire at {}", cronExpression.getCronExpression(), fireDate);
        return cronScheduledFuture;
    }


    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently
     * with the given period; that is executions will commence after initialDelay then initialDelay+period,
     * then initialDelay + 2 * period, and so on.
     * @param command the task to execute.
     * @param initialDelay the time to delay first execution.
     * @param period the period between successive executions.
     * @param unit the time unit of the initialDelay and period parameters.
     * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an
     * exception upon cancellation
     */
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
        return getExecutorService().scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    /**
     * Creates and executes a periodic action that becomes enabled first after the given initial delay, and
     * subsequently with the given delay between the termination of one execution and the commencement of the next.
     * @param command the task to execute.
     * @param initialDelay the time to delay first execution.
     * @param delay the delay between the termination of one execution and the commencement of the next.
     * @param unit the time unit of the initialDelay and delay parameters
     * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an
     * exception upon cancellation
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        return getExecutorService().scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public long nextFireInterval(final Date fireDate) {
        return fireDate.getTime() - new Date().getTime();
    }

    private ScheduledExecutorService getExecutorService() {
        if (executorService == null) {
            if (scheduledItems > 0) {
                LOGGER.debug("{} starting {} threads", SIMPLE_NAME, scheduledItems);
                scheduledItems = Math.min(scheduledItems, MAX_SCHEDULED_ITEMS);
                ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(scheduledItems,
                        Log4jThreadFactory.createDaemonThreadFactory("Scheduled"));
                executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
                executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                this.executorService = executor;

            } else {
                LOGGER.debug("{}: No scheduled items", SIMPLE_NAME);
            }
        }
        return executorService;
    }

    public class CronRunnable implements Runnable {

        private final CronExpression cronExpression;
        private final Runnable runnable;
        private CronScheduledFuture<?> scheduledFuture;

        public CronRunnable(final Runnable runnable, final CronExpression cronExpression) {
            this.cronExpression = cronExpression;
            this.runnable = runnable;
        }

        public void setScheduledFuture(final CronScheduledFuture<?> future) {
            this.scheduledFuture = future;
        }

        @Override
        public void run() {
            try {
                long millis = scheduledFuture.getFireTime().getTime() - System.currentTimeMillis();
                if (millis > 0) {
                    LOGGER.debug("Cron thread woke up {} millis early. Sleeping", millis);
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException ie) {
                        // Ignore the interruption.
                    }
                }
                runnable.run();
            } catch(final Throwable ex) {
                LOGGER.error("{} caught error running command", SIMPLE_NAME, ex);
            } finally {
                final Date fireDate = cronExpression.getNextValidTimeAfter(new Date());
                final ScheduledFuture<?> future = schedule(this, nextFireInterval(fireDate), TimeUnit.MILLISECONDS);
                LOGGER.debug("Cron expression {} scheduled to fire again at {}", cronExpression.getCronExpression(),
                        fireDate);
                scheduledFuture.reset(future, fireDate);
            }
        }

        public String toString() {
            return "CronRunnable{" + cronExpression.getCronExpression() + " - " + scheduledFuture.getFireTime();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ConfigurationScheduler {");
        Queue<Runnable> queue = ((ScheduledThreadPoolExecutor) executorService).getQueue();
        boolean first = true;
        for (Runnable runnable : queue) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(runnable.toString());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

}
