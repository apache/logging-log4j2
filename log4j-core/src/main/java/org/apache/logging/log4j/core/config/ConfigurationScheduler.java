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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.async.DaemonThreadFactory;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ConfigurationScheduler extends AbstractLifeCycle {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private ScheduledExecutorService executorService;

    private int scheduledItems = 0;


    @Override
    public void start() {
        super.start();
        if (scheduledItems > 0) {
            LOGGER.debug("Starting {} Log4j2Scheduled threads", scheduledItems);
            if (scheduledItems > 5) {
                scheduledItems = 5;
            }
            executorService = new ScheduledThreadPoolExecutor(scheduledItems, new DaemonThreadFactory("Log4j2Scheduled-"));
        } else {
            LOGGER.debug("No scheduled items");
        }
    }

    @Override
    public void stop() {
        if (executorService != null) {
            LOGGER.debug("Stopping Log4j2Scheduled threads.");
            executorService.shutdown();
        }
        super.stop();
    }

    /**
     * Increment the number of threads in the pool.
     */
    public void incrementScheduledItems() {
        if (!isStarted()) {
            ++scheduledItems;
        } else {
            LOGGER.error("Attempted to increment scheduled items after start");
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
     * @param callable the function to execute.
     * @param delay the time from now to delay execution.
     * @param unit the time unit of the delay parameter.
     * @return a ScheduledFuture that can be used to extract result or cancel.
     *
     */
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return executorService.schedule(callable, delay, unit);
    }

    /**
     * Creates and executes a one-shot action that becomes enabled after the given delay.
     * @param command the task to execute.
     * @param delay the time from now to delay execution.
     * @param unit the time unit of the delay parameter.
     * @return a ScheduledFuture representing pending completion of the task and whose get() method will return null
     * upon completion.
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return executorService.schedule(command, delay, unit);
    }


    /**
     * Creates and executes an action that first based on a cron expression.
     * @param cronExpression the cron expression describing the schedule.
     * @param command The Runnable to run,
     * @return a ScheduledFuture representing the next time the command will run.
     */
    public CronScheduledFuture<?> scheduleWithCron(CronExpression cronExpression, Runnable command) {
        CronRunnable runnable = new CronRunnable(command, cronExpression);
        ScheduledFuture<?> future = schedule(runnable, nextFireInterval(cronExpression), TimeUnit.MILLISECONDS);
        CronScheduledFuture<?> cronScheduledFuture = new CronScheduledFuture<>(future);
        runnable.setScheduledFuture(cronScheduledFuture);
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
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return executorService.scheduleAtFixedRate(command, initialDelay, period, unit);
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
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return executorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    private class CronRunnable implements Runnable {

        private final CronExpression cronExpression;
        private final Runnable runnable;
        private CronScheduledFuture<?> scheduledFuture;

        public CronRunnable(Runnable runnable, CronExpression cronExpression) {
            this.cronExpression = cronExpression;
            this.runnable = runnable;
        }

        public void setScheduledFuture(CronScheduledFuture<?> future) {
            this.scheduledFuture = future;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } catch(Throwable ex) {
                LOGGER.error("Error running command", ex);
            } finally {
                ScheduledFuture<?> future = schedule(this, nextFireInterval(cronExpression), TimeUnit.MILLISECONDS);
                scheduledFuture.setScheduledFuture(future);
            }
        }
    }

    private long nextFireInterval(CronExpression cronExpression) {
        Date now = new Date();
        Date fireDate = cronExpression.getNextValidTimeAfter(now);
        return fireDate.getTime() - now.getTime();
    }

}
