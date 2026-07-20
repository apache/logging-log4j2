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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.core.util.CronExpression;
import org.junit.jupiter.api.Test;

class ConfigurationSchedulerTest {

    @Test
    void testScheduleWithCronRaceCondition() throws Exception {
        final CountDownLatch firstRunLatch = new CountDownLatch(1);
        final CountDownLatch secondRunLatch = new CountDownLatch(1);

        final Runnable command = () -> {
            if (firstRunLatch.getCount() > 0) {
                firstRunLatch.countDown();
            } else {
                secondRunLatch.countDown();
            }
        };

        final ConfigurationScheduler scheduler = new ConfigurationScheduler() {
            private final AtomicBoolean first = new AtomicBoolean(true);

            @Override
            public ScheduledFuture<?> schedule(final Runnable cmd, final long delay, final TimeUnit unit) {
                if (first.compareAndSet(true, false)) {
                    final ScheduledFuture<?> future = super.schedule(cmd, 0, TimeUnit.MILLISECONDS);
                    try {
                        assertTrue(firstRunLatch.await(5, TimeUnit.SECONDS), "First run failed, likely NPE");

                        Thread.sleep(100);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return future;
                }
                return super.schedule(cmd, delay, unit);
            }
        };

        try {
            scheduler.incrementScheduledItems();
            scheduler.start();

            final CronExpression cron = new CronExpression("0/1 * * * * ?");

            final ScheduledFuture<?> future = scheduler.scheduleWithCron(cron, new Date(), command);

            future.cancel(false);

            assertFalse(
                    secondRunLatch.await(2, TimeUnit.SECONDS),
                    "Task ran after cancellation. Monotonic reset is broken.");

        } finally {
            scheduler.stop(1, TimeUnit.SECONDS);
        }
    }
}
