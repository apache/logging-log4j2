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

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class CronScheduledFuture<V> implements ScheduledFuture<V> {

    private volatile FutureData futureData;

    public CronScheduledFuture(final ScheduledFuture<V> future, final Date runDate) {
        this.futureData = new FutureData(future, runDate);
    }

    public Date getFireTime() {
        return futureData.runDate;
    }

    void reset(final ScheduledFuture<?> future, final Date runDate) {
        futureData = new FutureData(future, runDate);
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        return futureData.scheduledFuture.getDelay(unit);
    }

    @Override
    public int compareTo(final Delayed delayed) {
        return futureData.scheduledFuture.compareTo(delayed);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return futureData.scheduledFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return futureData.scheduledFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return futureData.scheduledFuture.isDone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        return (V) futureData.scheduledFuture.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return (V) futureData.scheduledFuture.get(timeout, unit);
    }

    private class FutureData {

        private final ScheduledFuture<?> scheduledFuture;
        private final Date runDate;

        FutureData(final ScheduledFuture<?> future, final Date runDate) {
            this.scheduledFuture = future;
            this.runDate = runDate;
        }
    }
}
