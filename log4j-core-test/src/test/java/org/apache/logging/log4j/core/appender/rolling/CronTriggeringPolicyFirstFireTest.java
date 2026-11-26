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
package org.apache.logging.log4j.core.appender.rolling;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.CronScheduledFuture;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.util.CronExpression;
import org.junit.jupiter.api.Test;

class CronTriggeringPolicyFirstFireTest {

    @Test
    void testFirstFireBeforeScheduleWithCronReturnsUsesFireTime() throws Exception {
        final AtomicReference<Date> fireTime = new AtomicReference<>();
        final AtomicReference<Thread> fire = new AtomicReference<>();
        final ConfigurationScheduler scheduler = new ConfigurationScheduler() {
            @Override
            public CronScheduledFuture<?> scheduleWithCron(
                    final CronExpression cronExpression, final Date startDate, final Runnable command) {
                fireTime.set(cronExpression.getNextValidTimeAfter(startDate));
                // Like the real scheduler, run the command on another thread, shortly after the fire time.
                final long runTime = fireTime.get().getTime() + 50;
                final Thread thread = new Thread(() -> {
                    while (System.currentTimeMillis() < runTime) {
                        LockSupport.parkUntil(runTime);
                    }
                    command.run();
                });
                fire.set(thread);
                thread.start();
                // Return only after the command has run, or is blocked waiting for this method to return.
                while (thread.isAlive() && thread.getState() != Thread.State.BLOCKED) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                }
                return new CronScheduledFuture<>(mock(ScheduledFuture.class), fireTime.get());
            }
        };
        final NullConfiguration configuration = new NullConfiguration() {
            @Override
            public ConfigurationScheduler getScheduler() {
                return scheduler;
            }
        };
        final RollingFileManager manager = mock(RollingFileManager.class);
        when(manager.getFileTime()).thenReturn(System.currentTimeMillis());
        when(manager.getPatternProcessor()).thenReturn(new PatternProcessor("test.log.%d{yyyyMMdd-HHmmss}"));
        final CronExpression everySecond = new CronExpression("* * * * * ?");
        final CronTriggeringPolicy policy =
                CronTriggeringPolicy.createPolicy(configuration, "false", everySecond.getCronExpression());

        policy.initialize(manager);
        fire.get().join();

        verify(manager).rollover(eq(everySecond.getPrevFireTime(fireTime.get())), eq(fireTime.get()));
    }
}
