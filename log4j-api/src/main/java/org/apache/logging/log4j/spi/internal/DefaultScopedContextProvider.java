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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ScopedContext;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.spi.ScopedContextProvider;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringMap;

/**
 * An implementation of {@link ScopedContextProvider} that uses the simplest implementation.
 * @since 2.24.0
 */
public class DefaultScopedContextProvider implements ScopedContextProvider {

    public static final Logger LOGGER = StatusLogger.getLogger();
    public static final ScopedContextProvider INSTANCE = new DefaultScopedContextProvider();

    private final ThreadLocal<MapInstance> scopedContext = new ThreadLocal<>();
    private final MapInstance emptyMap = new MapInstance(this, Collections.emptyMap());

    /**
     * Returns an immutable Map containing all the key/value pairs as Object objects.
     * @return The current context Instance.
     */
    private MapInstance getContext() {
        return Optional.ofNullable(scopedContext.get()).orElse(emptyMap);
    }

    @Override
    public Map<String, ?> getContextMap() {
        return getContext().contextMap;
    }

    /**
     * Return the value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    @Override
    public Object getValue(final String key) {
        return getContext().contextMap.get(key);
    }

    /**
     * Return String value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    @Override
    public String getString(final String key) {
        final Object obj = getContext().contextMap.get(key);
        return obj != null ? obj.toString() : null;
    }

    /**
     * Adds all the String rendered objects in the context map to the provided Map.
     * @param map The Map to add entries to.
     */
    @Override
    public void addContextMapTo(final StringMap map) {
        getContext().contextMap.forEach((key, value) -> map.putValue(key, value.toString()));
    }

    @Override
    public ScopedContext.Instance newScopedContext() {
        return getContext();
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
        return getContext().where(key, value);
    }

    /**
     * Creates a ScopedContext Instance with a Map of keys and values.
     * @param map the Map.
     * @return the ScopedContext Instance constructed.
     */
    @Override
    public ScopedContext.Instance newScopedContext(final Map<String, ?> map) {
        final Map<String, Object> objectMap = new HashMap<>(getContext().contextMap);
        map.forEach((key, value) -> {
            if (value == null) {
                objectMap.remove(key);
            } else {
                objectMap.put(key, value);
            }
        });
        return new MapInstance(this, Collections.unmodifiableMap(objectMap));
    }

    /**
     * A simple implementation of {@link ScopedContext.Instance} based on an immutable map.
     */
    private static final class MapInstance implements ScopedContext.Instance {

        private final DefaultScopedContextProvider provider;
        private final Map<String, ?> contextMap;

        /**
         * @param provider An instance of this provider.
         * @param contextMap An immutable map.
         */
        private MapInstance(final DefaultScopedContextProvider provider, final Map<String, ?> contextMap) {
            this.provider = provider;
            this.contextMap = contextMap;
        }

        /**
         * Adds a key/value pair to the ScopedContext being constructed.
         *
         * @param key   the key to add.
         * @param value the value associated with the key.
         * @return the ScopedContext being constructed.
         */
        @Override
        public ScopedContext.Instance where(final String key, final Object value) {
            return new KeyValueInstance(this, null, key, value);
        }

        /**
         * Adds a key/value pair to the ScopedContext being constructed.
         *
         * @param key      the key to add.
         * @param supplier the function to generate the value.
         * @return the ScopedContext being constructed.
         */
        @Override
        public ScopedContext.Instance where(final String key, final Supplier<Object> supplier) {
            return new KeyValueInstance(this, null, key, supplier.get());
        }

        private static void setThreadContext(
                final Map<String, String> threadContextMap, final Collection<String> threadContextStack) {
            ThreadContext.clearMap();
            ThreadContext.putAll(threadContextMap);
            if (threadContextStack != null) {
                ThreadContext.setStack(threadContextStack);
            } else {
                ThreadContext.clearStack();
            }
        }

        private MapInstance setScopedContext(final MapInstance context) {
            final MapInstance current = provider.scopedContext.get();
            if (context != null) {
                provider.scopedContext.set(context);
            } else {
                provider.scopedContext.remove();
            }
            return current;
        }

        /**
         * Wraps the provided Runnable method with a Runnable method that will instantiate the Scoped and Thread
         * Contexts in the target Thread before the caller's run method is called.
         * @param task the Runnable task to perform.
         * @return a Runnable.
         */
        @Override
        public Runnable wrap(final Runnable task) {
            final Map<String, String> contextMap = ThreadContext.getImmutableContext();
            final ThreadContext.ContextStack contextStack = ThreadContext.getImmutableStack();
            return () -> {
                setThreadContext(contextMap, contextStack);
                final MapInstance oldInstance = setScopedContext(this);
                try {
                    task.run();
                } finally {
                    setScopedContext(oldInstance);
                    ThreadContext.clearAll();
                }
            };
        }

        /**
         * Wraps the provided Callable method with a Callable method that will instantiate the Scoped and Thread
         * Contexts in the target Thread before the caller's call method is called.
         * @param task the Callable task to perform.
         * @return a Callable.
         */
        @Override
        public <R> Callable<R> wrap(final Callable<? extends R> task) {
            final Map<String, String> contextMap = ThreadContext.getImmutableContext();
            final ThreadContext.ContextStack contextStack = ThreadContext.getImmutableStack();
            return () -> {
                setThreadContext(contextMap, contextStack);
                final MapInstance oldInstance = setScopedContext(this);
                try {
                    return task.call();
                } finally {
                    setScopedContext(oldInstance);
                    ThreadContext.clearAll();
                }
            };
        }
    }

    /**
     * Forms a list of linked instances that registers the differences from a {@link MapInstance}.
     */
    private static final class KeyValueInstance implements ScopedContext.Instance {

        private final MapInstance base;
        private final KeyValueInstance parent;
        private final String key;
        private final Object value;

        private KeyValueInstance(
                final MapInstance base, final KeyValueInstance parent, final String key, final Object value) {
            this.base = base;
            this.parent = parent;
            this.key = key;
            this.value = value;
        }

        @Override
        public ScopedContext.Instance where(final String key, final Object value) {
            return new KeyValueInstance(base, this, key, value);
        }

        @Override
        public ScopedContext.Instance where(final String key, final Supplier<Object> supplier) {
            return new KeyValueInstance(base, this, key, supplier.get());
        }

        private MapInstance toMapInstance() {
            final Map<String, Object> objectMap = new HashMap<>(base.contextMap);
            KeyValueInstance current = this;
            while (current != null) {
                if (current.value == null) {
                    objectMap.remove(current.key);
                } else {
                    objectMap.put(current.key, current.value);
                }
                current = current.parent;
            }
            return new MapInstance(base.provider, Collections.unmodifiableMap(objectMap));
        }

        @Override
        public Runnable wrap(final Runnable task) {
            return toMapInstance().wrap(task);
        }

        @Override
        public <R> Callable<R> wrap(final Callable<? extends R> task) {
            return toMapInstance().wrap(task);
        }
    }
}
