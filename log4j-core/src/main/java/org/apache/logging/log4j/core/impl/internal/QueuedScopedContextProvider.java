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
package org.apache.logging.log4j.core.impl.internal;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
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
import org.apache.logging.log4j.spi.ScopedContextProvider;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringMap;

public class QueuedScopedContextProvider implements ScopedContextProvider {

    public static final Logger LOGGER = StatusLogger.getLogger();
    public static final ScopedContextProvider INSTANCE = new QueuedScopedContextProvider();

    private final ThreadLocal<Deque<Instance>> scopedContext = new ThreadLocal<>();

    private final Instance EMPTY_INSTANCE = new Instance(this);

    /**
     * Returns an immutable Map containing all the key/value pairs as Object objects.
     * @return An immutable copy of the Map at the current scope.
     */
    private Optional<Instance> getContext() {
        final Deque<Instance> stack = scopedContext.get();
        return stack != null ? Optional.of(stack.getFirst()) : Optional.empty();
    }

    /**
     * Add the ScopeContext.
     * @param context The ScopeContext.
     */
    private void addScopedContext(final Instance context) {
        Deque<Instance> stack = scopedContext.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            scopedContext.set(stack);
        }
        stack.addFirst(context);
    }

    /**
     * Remove the top ScopeContext.
     */
    private void removeScopedContext() {
        final Deque<Instance> stack = scopedContext.get();
        if (stack != null) {
            if (!stack.isEmpty()) {
                stack.removeFirst();
            }
            if (stack.isEmpty()) {
                scopedContext.remove();
            }
        }
    }

    @Override
    public Map<String, ?> getContextMap() {
        final Optional<Instance> context = getContext();
        return context.isPresent()
                        && context.get().contextMap != null
                        && !context.get().contextMap.isEmpty()
                ? Collections.unmodifiableMap(context.get().contextMap)
                : Collections.emptyMap();
    }

    /**
     * Return the value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    @Override
    public Object getValue(final String key) {
        final Optional<Instance> context = getContext();
        return context.map(instance -> instance.contextMap)
                .map(map -> map.get(key))
                .orElse(null);
    }

    /**
     * Return String value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    @Override
    public String getString(final String key) {
        final Optional<Instance> context = getContext();
        if (context.isPresent()) {
            final Object obj = context.get().contextMap.get(key);
            if (obj != null) {
                return obj.toString();
            }
        }
        return null;
    }

    /**
     * Adds all the String rendered objects in the context map to the provided Map.
     * @param map The Map to add entries to.
     */
    @Override
    public void addContextMapTo(final StringMap map) {
        final Optional<Instance> context = getContext();
        if (context.isPresent()) {
            final Map<String, ?> contextMap = context.get().contextMap;
            if (contextMap != null && !contextMap.isEmpty()) {
                contextMap.forEach((key, value) -> map.putValue(key, value.toString()));
            }
        }
    }

    @Override
    public ScopedContext.Instance newScopedContext() {
        return getContext().isPresent() ? getContext().get() : EMPTY_INSTANCE;
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
        if (value != null) {
            final Instance parent = getContext().isPresent() ? getContext().get() : EMPTY_INSTANCE;
            return new Instance(parent, key, value);
        } else {
            if (getContext().isPresent()) {
                final Map<String, ?> map = getContextMap();
                map.remove(key);
                return new Instance(this, map);
            }
        }
        return newScopedContext();
    }

    /**
     * Creates a ScopedContext Instance with a Map of keys and values.
     * @param map the Map.
     * @return the ScopedContext Instance constructed.
     */
    @Override
    public ScopedContext.Instance newScopedContext(final Map<String, ?> map) {
        if (map != null && !map.isEmpty()) {
            final Map<String, Object> objectMap = new HashMap<>();
            if (getContext().isPresent()) {
                objectMap.putAll(getContext().get().contextMap);
            }
            map.forEach((key, value) -> {
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    objectMap.remove(key);
                } else {
                    objectMap.put(key, value);
                }
            });
            return new Instance(this, objectMap);
        } else {
            return getContext().isPresent() ? getContext().get() : EMPTY_INSTANCE;
        }
    }

    private static void setupContext(
            final Map<String, Object> contextMap,
            final Map<String, String> threadContextMap,
            final Collection<String> contextStack,
            final Instance context) {
        Instance scopedContext = context;
        // If the current context has a Map then we can just use it.
        if (context.contextMap == null) {
            do {
                if (scopedContext.contextMap != null) {
                    // Once we hit a scope with an already populated Map we won't need to go any further.
                    contextMap.putAll(scopedContext.contextMap);
                    break;
                } else if (scopedContext.key != null) {
                    contextMap.putIfAbsent(scopedContext.key, scopedContext.value);
                }
                scopedContext = scopedContext.parent;
            } while (scopedContext != null);
            scopedContext = new Instance(context.getProvider(), contextMap);
        }
        if (threadContextMap != null && !threadContextMap.isEmpty()) {
            ThreadContext.putAll(threadContextMap);
        }
        if (contextStack != null) {
            ThreadContext.setStack(contextStack);
        }
        context.getProvider().addScopedContext(scopedContext);
    }

    private static final class Instance implements ScopedContext.Instance {

        private final QueuedScopedContextProvider provider;
        private final Instance parent;
        private final String key;
        private final Object value;
        private final Map<String, ?> contextMap;

        private Instance(final QueuedScopedContextProvider provider) {
            this.provider = provider;
            parent = null;
            key = null;
            value = null;
            contextMap = null;
        }

        private Instance(final QueuedScopedContextProvider provider, final Map<String, ?> map) {
            this.provider = provider;
            parent = null;
            key = null;
            value = null;
            contextMap = map;
        }

        private Instance(final Instance parent, final String key, final Object value) {
            provider = parent.getProvider();
            this.parent = parent;
            this.key = key;
            this.value = value;
            contextMap = null;
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

        private Instance addObject(final String key, final Object obj) {
            return obj != null ? new Instance(this, key, obj) : this;
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext.
         *
         * @param task the code block to execute.
         */
        @Override
        public void run(final Runnable task) {
            new Runner(this, null, null, task).run();
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        @Override
        public Future<Void> run(final ExecutorService executorService, final Runnable task) {
            return executorService.submit(
                    new Runner(this, ThreadContext.getContext(), ThreadContext.getImmutableStack(), task), null);
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext.
         *
         * @param task the code block to execute.
         * @return the return value from the code block.
         */
        @Override
        public <R> R call(final Callable<R> task) throws Exception {
            return new Caller<>(this, null, null, task).call();
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        @Override
        public <R> Future<R> call(final ExecutorService executorService, final Callable<R> task) {
            return executorService.submit(
                    new Caller<>(this, ThreadContext.getContext(), ThreadContext.getImmutableStack(), task));
        }

        /**
         * Wraps the provided Runnable method with a Runnable method that will instantiate the Scoped and Thread
         * Contexts in the target Thread before the caller's run method is called.
         * @param task the Runnable task to perform.
         * @return a Runnable.
         */
        @Override
        public Runnable wrap(Runnable task) {
            return new Runner(this, ThreadContext.getContext(), ThreadContext.getImmutableStack(), task);
        }

        /**
         * Wraps the provided Callable method with a Callable method that will instantiate the Scoped and Thread
         * Contexts in the target Thread before the caller's call method is called.
         * @param task the Callable task to perform.
         * @return a Callable.
         */
        @Override
        public <R> Callable<R> wrap(Callable<R> task) {
            return new Caller<>(this, ThreadContext.getContext(), ThreadContext.getImmutableStack(), task);
        }

        private QueuedScopedContextProvider getProvider() {
            return provider;
        }
    }

    private static class Runner implements Runnable {
        private final Map<String, Object> contextMap = new HashMap<>();
        private final Map<String, String> threadContextMap;
        private final ThreadContext.ContextStack contextStack;
        private final Instance context;
        private final Runnable op;

        public Runner(
                final Instance context,
                final Map<String, String> threadContextMap,
                final ThreadContext.ContextStack contextStack,
                final Runnable op) {
            this.context = context;
            this.threadContextMap = threadContextMap;
            this.contextStack = contextStack;
            this.op = op;
        }

        @Override
        public void run() {
            setupContext(contextMap, threadContextMap, contextStack, context);
            try {
                op.run();
            } finally {
                context.getProvider().removeScopedContext();
                ThreadContext.clearAll();
            }
        }
    }

    private static class Caller<R> implements Callable<R> {
        private final Map<String, Object> contextMap = new HashMap<>();
        private final Instance context;
        private final Map<String, String> threadContextMap;
        private final ThreadContext.ContextStack contextStack;
        private final Callable<R> op;

        public Caller(
                final Instance context,
                final Map<String, String> threadContextMap,
                final ThreadContext.ContextStack contextStack,
                final Callable<R> op) {
            this.context = context;
            this.threadContextMap = threadContextMap;
            this.contextStack = contextStack;
            this.op = op;
        }

        @Override
        public R call() throws Exception {
            setupContext(contextMap, threadContextMap, contextStack, context);
            try {
                return op.call();
            } finally {
                context.getProvider().removeScopedContext();
                ThreadContext.clearAll();
            }
        }
    }
}
