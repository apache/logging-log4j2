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

import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class CronScheduledFuture<V> implements ScheduledFuture<V> {

    private volatile ScheduledFuture<?> scheduledFuture;

    public CronScheduledFuture(final ScheduledFuture<V> future) {
        this.scheduledFuture = future;
    }

    void setScheduledFuture(final ScheduledFuture<?> future) {
        this.scheduledFuture = future;
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        return scheduledFuture.getDelay(unit);
    }

    @Override
    public int compareTo(final Delayed delayed) {
        return scheduledFuture.compareTo(delayed);
    }

    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return scheduledFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return scheduledFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return scheduledFuture.isDone();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get() throws InterruptedException, ExecutionException {
        return (V) scheduledFuture.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return (V) scheduledFuture.get(timeout, unit);
    }
}
