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
package org.apache.logging.log4j.spi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ScopedContext;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.internal.map.ScopedContextMap;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringMap;

/**
 * An implementation of {@link ScopedContextProvider} that uses the simplest implementation.
 * @since 2.24.0
 */
public abstract class AbstractScopedContextProvider implements ScopedContextProvider {

    public static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Returns an immutable Instance.
     * @return The current context Instance.
     */
    protected abstract Optional<Instance> getContext();

    /**
     * Add the ScopeContext.
     * @param context The ScopeContext.
     */
    protected abstract void addScopedContext(final Instance context);

    /**
     * Remove the top ScopeContext.
     */
    protected abstract void removeScopedContext();

    @Override
    public Map<String, Object> getContextMap() {
        final Optional<Instance> context = getContext();
        if (context.isPresent()) {
            return (context.get()).contextMap;
        }
        return Collections.emptyMap();
    }

    /**
     * Return the value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    @Override
    public Object getValue(final String key) {
        return getContextMap().get(key);
    }

    /**
     * Return String value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    @Override
    public String getString(final String key) {
        final Object obj = getValue(key);
        return obj != null ? obj.toString() : null;
    }

    /**
     * Adds all the String rendered objects in the context map to the provided Map.
     * @param map The Map to add entries to.
     */
    @Override
    public void addContextMapTo(final StringMap map) {
        final Optional<Instance> context = getContext();
        if (context.isPresent()) {
            final Map<String, ?> contextMap = (context.get()).contextMap;
            if (contextMap != null && !contextMap.isEmpty()) {
                contextMap.forEach((key, value) -> map.putValue(key, value.toString()));
            }
        }
    }

    /**
     * Creates a ScopedContext Instance with a key/value pair.
     *
     * @param key   the key to add.
     * @param value the value associated with the key.
     * @return the Instance constructed if a valid key and value were provided. Otherwise, either the
     * current Instance is returned or a new Instance is created if there is no current Instance.
     */
    @Override
    public ScopedContext.Instance newScopedContext(final String key, final Object value) {
        Optional<Instance> context = getContext();
        final Instance parent = context.orElse(null);
        final boolean withThreadContext = parent != null && parent.withThreadContext;
        return newInstance(parent, key, value, withThreadContext);
    }

    /**
     * Creates a ScopedContext Instance with a Map of keys and values.
     * @param map the Map.
     * @return the ScopedContext Instance constructed.
     */
    @Override
    public ScopedContext.Instance newScopedContext(final Map<String, ?> map) {
        final Map<String, Object> objectMap = new HashMap<>(getContextMap());
        if (map != null && !map.isEmpty()) {
            map.forEach((key, value) -> {
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    objectMap.remove(key);
                } else {
                    objectMap.put(key, value);
                }
            });
        }
        return newInstance(null, objectMap, false);
    }

    /**
     * Creates a ScopedContext Instance that indicates the ThreadContext should be included.
     * @param withThreadContext true if the ThreadContext should be included.
     * @return the ScopedContext Instance constructed.
     */
    @Override
    public ScopedContext.Instance newScopedContext(final boolean withThreadContext) {
        Instance parent = getContext().orElse(null);
        return newInstance(parent, withThreadContext);
    }

    protected Instance newInstance(Instance instance, String key, Object value, boolean withThreadContext) {
        return new Instance(this, instance, key, value, withThreadContext);
    }

    protected Instance newInstance(Instance instance, boolean withThreadContext) {
        return new Instance(this, instance, withThreadContext);
    }

    protected Instance newInstance(
            final Instance instance, final Map<String, Object> map, final Boolean withThreadContext) {
        final Map<String, Object> objectMap = instance == null ? new HashMap<>() : new HashMap<>(instance.contextMap);
        map.forEach((key, value) -> {
            if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                objectMap.remove(key);
            } else {
                objectMap.put(key, value);
            }
        });
        return new Instance(this, instance, objectMap, withThreadContext);
    }

    /**
     * When an Instance is created it may contain a Key/Value pair from a Where method. When a run method is
     * called a Map will be created and all the Key/Value pairs from parent instances will be added to
     * it along with all the keys from the first Instance encountered that contains a Map. In this
     * way we avoid continually copying Maps in order to add a single Key/Value pair.
     */
    protected static class Instance implements ScopedContext.Instance {

        protected final AbstractScopedContextProvider provider;
        protected final Instance parent;
        private final Map<String, Object> contextMap;
        protected final boolean withThreadContext;

        protected boolean isWithThreadContext() {
            return withThreadContext;
        }

        public Instance(
                final AbstractScopedContextProvider provider,
                final Instance parent,
                Map<String, Object> map,
                final boolean withThreadContext) {
            this.provider = provider != null ? provider : parent.provider;
            this.parent = parent;
            this.withThreadContext = withThreadContext;
            if (parent == null) {
                contextMap = ScopedContextMap.getInstance(map);
            } else {
                if (map != null && !map.isEmpty()) {
                    contextMap = ((ScopedContextMap) parent.contextMap).copyAndPutAll(map);
                } else {
                    contextMap = ScopedContextMap.emptyMap();
                }
            }
        }

        public Instance(
                final AbstractScopedContextProvider provider,
                final Instance parent,
                String key,
                Object value,
                final boolean withThreadContext) {
            this.provider = provider != null ? provider : parent.provider;
            this.parent = parent;
            this.withThreadContext = withThreadContext;
            if (parent == null) {
                contextMap = ScopedContextMap.getInstance(key, value);
            } else {
                contextMap = ((ScopedContextMap) parent.contextMap).copyAndPut(key, value);
            }
        }

        private Instance(
                final AbstractScopedContextProvider provider, final Instance parent, final boolean withThreadContext) {
            this.provider = provider != null ? provider : parent.provider;
            this.parent = parent;
            contextMap = parent == null ? ScopedContextMap.emptyMap() : parent.contextMap;
            this.withThreadContext = withThreadContext;
        }

        public AbstractScopedContextProvider getProvider() {
            return provider;
        }

        public Instance getParent() {
            return parent;
        }

        /**
         * Adds a key/value pair to the ScopedContext being constructed.
         *
         * @param key   the key to add.
         * @param value the value associated with the key.
         * @return the ScopedContext being constructed.
         */
        @Override
        public Instance where(final String key, final Object value) {
            return addObject(key, value);
        }

        /**
         * Adds a key/value pair to the ScopedContext being constructed.
         *
         * @param key      the key to add.
         * @param supplier the function to generate the value.
         * @return the ScopedContext being constructed.
         */
        @Override
        public Instance where(final String key, final Supplier<Object> supplier) {
            return addObject(key, supplier.get());
        }

        /**
         * Creates an Instance to declare the ThreadContext should be included.
         *
         * @return the ScopedContext being constructed.
         */
        @Override
        public Instance withThreadContext() {
            return provider.newInstance(this, true);
        }

        protected Instance addObject(final String key, final Object obj) {
            return provider.newInstance(this, key, obj, this.withThreadContext);
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param key   the key to add.
         * @param value the value associated with the key.
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         */
        @Override
        public Future<Void> runWhere(String key, Object value, ExecutorService executorService, Runnable task) {
            Map<String, String> map = this.withThreadContext ? ThreadContext.getContext() : null;
            Instance instance = addObject(key, value);
            return executorService.submit(new Runner(instance, map, ThreadContext.getImmutableStack(), task), null);
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param key   the key to add.
         * @param supplier the function to generate the value.
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         */
        @Override
        public Future<Void> runWhere(
                String key, Supplier<Object> supplier, ExecutorService executorService, Runnable task) {
            return runWhere(key, supplier.get(), executorService, task);
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param key   the key to add.
         * @param value the value associated with the key.
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         */
        @Override
        public <R> Future<R> callWhere(String key, Object value, ExecutorService executorService, Callable<R> task) {
            Map<String, String> map = this.withThreadContext ? ThreadContext.getContext() : null;
            ThreadContext.ContextStack stack = withThreadContext ? ThreadContext.getImmutableStack() : null;
            Instance instance = addObject(key, value);
            return executorService.submit(new Caller<>(instance, map, stack, task));
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param key   the key to add.
         * @param supplier the function to generate the value.
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         */
        @Override
        public <R> Future<R> callWhere(
                String key, Supplier<Object> supplier, ExecutorService executorService, Callable<R> task) {
            return callWhere(key, supplier.get(), executorService, task);
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext.
         *
         * @param task the code block to execute.
         */
        @Override
        public void run(final Runnable task) {
            runTask(null, null, task);
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        @Override
        public Future<Void> run(final ExecutorService executorService, final Runnable task) {
            Map<String, String> map = this.withThreadContext ? ThreadContext.getContext() : null;
            return executorService.submit(new Runner(this, map, ThreadContext.getImmutableStack(), task), null);
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext.
         *
         * @param task the code block to execute.
         * @return the return value from the code block.
         */
        @Override
        public <R> R call(final Callable<R> task) throws Exception {
            return callTask(null, null, task);
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        @Override
        public <R> Future<R> call(final ExecutorService executorService, final Callable<R> task) {
            Map<String, String> map = this.withThreadContext ? ThreadContext.getContext() : null;
            ThreadContext.ContextStack stack = withThreadContext ? ThreadContext.getImmutableStack() : null;
            return executorService.submit(new Caller<>(this, map, stack, task));
        }

        /**
         * Wraps the provided Runnable method with a Runnable method that will instantiate the Scoped and Thread
         * Contexts in the target Thread before the caller's run method is called.
         * @param task the Runnable task to perform.
         * @return a Runnable.
         */
        @Override
        public Runnable wrap(Runnable task) {
            Map<String, String> map = this.withThreadContext ? ThreadContext.getContext() : null;
            ThreadContext.ContextStack stack = withThreadContext ? ThreadContext.getImmutableStack() : null;
            return new Runner(this, map, stack, task);
        }

        /**
         * Wraps the provided Callable method with a Callable method that will instantiate the Scoped and Thread
         * Contexts in the target Thread before the caller's call method is called.
         * @param task the Callable task to perform.
         * @return a Callable.
         */
        @Override
        public <R> Callable<R> wrap(Callable<R> task) {
            Map<String, String> map = this.withThreadContext ? ThreadContext.getContext() : null;
            ThreadContext.ContextStack stack = withThreadContext ? ThreadContext.getImmutableStack() : null;
            return new Caller<>(this, map, stack, task);
        }

        private void runTask(
                final Map<String, String> threadContextMap,
                final ThreadContext.ContextStack contextStack,
                final Runnable task) {
            setupContext(threadContextMap, contextStack);
            try {
                task.run();
            } finally {
                restoreContext(threadContextMap, contextStack);
            }
        }

        private <R> R callTask(
                final Map<String, String> threadContextMap,
                final ThreadContext.ContextStack contextStack,
                final Callable<R> task)
                throws Exception {
            setupContext(threadContextMap, contextStack);
            try {
                return task.call();
            } finally {
                restoreContext(threadContextMap, contextStack);
            }
        }

        private void setupContext(
                final Map<String, String> threadContextMap, final ThreadContext.ContextStack contextStack) {
            if (threadContextMap != null) {
                ThreadContext.clearMap();
                ThreadContext.putAll(threadContextMap);
            }
            if (contextStack != null) {
                ThreadContext.clearStack();
                ThreadContext.setStack(contextStack);
            }
            this.getProvider().addScopedContext(this);
        }

        private void restoreContext(
                final Map<String, String> threadContextMap, final ThreadContext.ContextStack contextStack) {
            this.getProvider().removeScopedContext();
            if (threadContextMap != null) {
                ThreadContext.clearMap();
            }
            if (contextStack != null) {
                ThreadContext.clearStack();
            }
        }
    }

    protected abstract static class AbstractWorker {
        protected final Map<String, String> threadContextMap;
        protected final ThreadContext.ContextStack contextStack;
        protected final Instance context;

        protected AbstractWorker(
                final Instance context,
                final Map<String, String> threadContextMap,
                final ThreadContext.ContextStack contextStack) {
            this.context = context;
            this.threadContextMap = threadContextMap;
            this.contextStack = contextStack;
        }
    }

    private static class Runner extends AbstractWorker implements Runnable {
        private final Runnable op;

        public Runner(
                final Instance context,
                final Map<String, String> threadContextMap,
                final ThreadContext.ContextStack contextStack,
                final Runnable op) {
            super(context, threadContextMap, contextStack);
            this.op = op;
        }

        @Override
        public void run() {
            context.runTask(threadContextMap, contextStack, op);
        }
    }

    private static class Caller<R> extends AbstractWorker implements Callable<R> {
        private final Callable<R> op;

        public Caller(
                final Instance context,
                final Map<String, String> threadContextMap,
                final ThreadContext.ContextStack contextStack,
                final Callable<R> op) {
            super(context, threadContextMap, contextStack);
            this.op = op;
        }

        @Override
        public R call() throws Exception {
            return context.callTask(threadContextMap, contextStack, op);
        }
    }
}
