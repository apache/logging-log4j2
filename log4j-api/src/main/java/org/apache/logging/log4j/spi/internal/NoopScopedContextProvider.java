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
package org.apache.logging.log4j.spi.internal;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.apache.logging.log4j.ScopedContext;
import org.apache.logging.log4j.spi.ScopedContextProvider;

/**
 * An implementation of {@link ScopedContextProvider} that does not propagate any data.
 * @since 2.24.0
 */
public class NoopScopedContextProvider implements ScopedContextProvider {

    private static final ScopedContext.Instance SCOPED_CONTEXT_INSTANCE = new NoopInstance();
    public static final ScopedContextProvider SCOPED_CONTEXT_PROVIDER_INSTANCE = new NoopScopedContextProvider();

    @Override
    public Map<String, ?> getContextMap() {
        return Collections.emptyMap();
    }

    @Override
    public Object getValue(final String key) {
        return null;
    }

    @Override
    public String getString(final String key) {
        return null;
    }

    @Override
    public ScopedContext.Instance newScopedContext() {
        return SCOPED_CONTEXT_INSTANCE;
    }

    @Override
    public ScopedContext.Instance newScopedContext(final String key, final Object value) {
        return SCOPED_CONTEXT_INSTANCE;
    }

    @Override
    public ScopedContext.Instance newScopedContext(final Map<String, ?> map) {
        return SCOPED_CONTEXT_INSTANCE;
    }

    private static class NoopInstance implements ScopedContext.Instance {

        @Override
        public ScopedContext.Instance where(final String key, final Object value) {
            return this;
        }

        @Override
        public ScopedContext.Instance where(final String key, final Supplier<Object> supplier) {
            return this;
        }

        @Override
        public void run(final Runnable task) {
            task.run();
        }

        @Override
        public Future<Void> run(final ExecutorService executorService, final Runnable task) {
            return executorService.submit(task, null);
        }

        @Override
        public <R> R call(final Callable<R> task) throws Exception {
            return task.call();
        }

        @Override
        public <R> Future<R> call(final ExecutorService executorService, final Callable<R> task) {
            return executorService.submit(task);
        }
    }
}
