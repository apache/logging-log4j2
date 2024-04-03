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
package org.apache.logging.log4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.apache.logging.log4j.spi.ScopedContextDataProvider;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Context that can be used for data to be logged in a block of code.
 *
 * While this is influenced by ScopedValues from Java 21 it does not share the same API. While it can perform a
 * similar function as a set of ScopedValues it is really meant to allow a block of code to include a set of keys and
 * values in all the log events within that block. The underlying implementation must provide support for
 * logging the ScopedContext for that to happen.
 *
 * The ScopedContext will not be bound to the current thread until either a run or call method is invoked. The
 * contexts are nested so creating and running or calling via a second ScopedContext will result in the first
 * ScopedContext being hidden until the call is returned. Thus the values from the first ScopedContext need to
 * be added to the second to be included.
 *
 * The ScopedContext can be passed to child threads by including the ExecutorService to be used to manage the
 * run or call methods. The caller should interact with the ExecutorService as if they were submitting their
 * run or call methods directly to it. The ScopedContext performs no error handling other than to ensure the
 * ThreadContext and ScopedContext are cleaned up from the executed Thread.
 *
 * @since 2.24.0
 */
public class ScopedContext {

    public static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * @hidden
     * Returns an unmodifiable copy of the current ScopedContext Map. This method should
     * only be used by implementations of Log4j API.
     * @return the Map of Renderable objects.
     */
    public static Map<String, Renderable> getContextMap() {
        Optional<Instance> context = ScopedContextDataProvider.getContext();
        if (context.isPresent()
                && context.get().contextMap != null
                && !context.get().contextMap.isEmpty()) {
            return Collections.unmodifiableMap(context.get().contextMap);
        }
        return Collections.emptyMap();
    }

    /**
     * @hidden
     * Returns the number of entries in the context map.
     * @return the number of items in the context map.
     */
    public static int size() {
        Optional<Instance> context = ScopedContextDataProvider.getContext();
        return context.map(instance -> instance.contextMap.size()).orElse(0);
    }

    /**
     * Return the value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        Optional<Instance> context = ScopedContextDataProvider.getContext();
        if (context.isPresent()) {
            Renderable renderable = context.get().contextMap.get(key);
            if (renderable != null) {
                return (T) renderable.getObject();
            }
        }
        return null;
    }

    /**
     * Return String value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    public static String getString(String key) {
        Optional<Instance> context = ScopedContextDataProvider.getContext();
        if (context.isPresent()) {
            Renderable renderable = context.get().contextMap.get(key);
            if (renderable != null) {
                return renderable.render();
            }
        }
        return null;
    }

    /**
     * Adds all the String rendered objects in the context map to the provided Map.
     * @param map The Map to add entries to.
     */
    public static void addAll(Map<String, String> map) {
        Optional<Instance> context = ScopedContextDataProvider.getContext();
        if (context.isPresent()) {
            Map<String, Renderable> contextMap = context.get().contextMap;
            if (contextMap != null && !contextMap.isEmpty()) {
                contextMap.forEach((key, value) -> map.put(key, value.render()));
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
    public static Instance where(String key, Object value) {
        if (value != null) {
            Renderable renderable = value instanceof Renderable ? (Renderable) value : new ObjectRenderable(value);
            Instance parent = current().isPresent() ? current().get() : null;
            return new Instance(parent, key, renderable);
        } else {
            if (current().isPresent()) {
                Map<String, Renderable> map = getContextMap();
                map.remove(key);
                return new Instance(map);
            }
        }
        return current().isPresent() ? current().get() : new Instance();
    }

    /**
     * Adds a key/value pair to the ScopedContext being constructed.
     *
     * @param key      the key to add.
     * @param supplier the function to generate the value.
     * @return the ScopedContext being constructed.
     */
    public static Instance where(String key, Supplier<Object> supplier) {
        return where(key, supplier.get());
    }

    /**
     * Creates a ScopedContext Instance with a Map of keys and values.
     * @param map the Map.
     * @return the ScopedContext Instance constructed.
     */
    public static Instance where(Map<String, ?> map) {
        if (map != null && !map.isEmpty()) {
            Map<String, Renderable> renderableMap = new HashMap<>();
            if (current().isPresent()) {
                renderableMap.putAll(current().get().contextMap);
            }
            map.forEach((key, value) -> {
                if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                    renderableMap.remove(key);
                } else {
                    renderableMap.put(
                            key, value instanceof Renderable ? (Renderable) value : new ObjectRenderable(value));
                }
            });
            return new Instance(renderableMap);
        } else {
            return current().isPresent() ? current().get() : new Instance();
        }
    }

    /**
     * Creates a ScopedContext with a single key/value pair and calls a method.
     * @param key the key.
     * @param obj the value associated with the key.
     * @param op the Runnable to call.
     */
    public static void runWhere(String key, Object obj, Runnable op) {
        if (obj != null) {
            Renderable renderable = obj instanceof Renderable ? (Renderable) obj : new ObjectRenderable(obj);
            Map<String, Renderable> map = new HashMap<>();
            if (current().isPresent()) {
                map.putAll(current().get().contextMap);
            }
            map.put(key, renderable);
            new Instance(map).run(op);
        } else {
            Map<String, Renderable> map = new HashMap<>();
            if (current().isPresent()) {
                map.putAll(current().get().contextMap);
            }
            map.remove(key);
            new Instance(map).run(op);
        }
    }

    /**
     * Creates a ScopedContext with a single key/value pair and calls a method on a separate Thread.
     * @param key the key.
     * @param obj the value associated with the key.
     * @param executorService the ExecutorService to dispatch the work.
     * @param op the Runnable to call.
     */
    public static Future<?> runWhere(String key, Object obj, ExecutorService executorService, Runnable op) {
        if (obj != null) {
            Renderable renderable = obj instanceof Renderable ? (Renderable) obj : new ObjectRenderable(obj);
            Map<String, Renderable> map = new HashMap<>();
            if (current().isPresent()) {
                map.putAll(current().get().contextMap);
            }
            map.put(key, renderable);
            if (executorService != null) {
                return executorService.submit(new Runner(
                        new Instance(map), ThreadContext.getContext(), ThreadContext.getImmutableStack(), op));
            } else {
                new Instance(map).run(op);
                return CompletableFuture.completedFuture(0);
            }
        } else {
            Map<String, Renderable> map = new HashMap<>();
            if (current().isPresent()) {
                map.putAll(current().get().contextMap);
            }
            map.remove(key);
            if (executorService != null) {
                return executorService.submit(new Runner(
                        new Instance(map), ThreadContext.getContext(), ThreadContext.getImmutableStack(), op));
            } else {
                new Instance(map).run(op);
                return CompletableFuture.completedFuture(0);
            }
        }
    }

    /**
     * Creates a ScopedContext with a Map of keys and values and calls a method.
     * @param map the Map.
     * @param op the Runnable to call.
     */
    public static void runWhere(Map<String, ?> map, Runnable op) {
        if (map != null && !map.isEmpty()) {
            Map<String, Renderable> renderableMap = new HashMap<>();
            if (current().isPresent()) {
                renderableMap.putAll(current().get().contextMap);
            }
            map.forEach((key, value) -> {
                renderableMap.put(key, value instanceof Renderable ? (Renderable) value : new ObjectRenderable(value));
            });
            new Instance(renderableMap).run(op);
        } else {
            op.run();
        }
    }

    /**
     * Creates a ScopedContext with a single key/value pair and calls a method.
     * @param key the key.
     * @param obj the value associated with the key.
     * @param op the Runnable to call.
     */
    public static <R> R callWhere(String key, Object obj, Callable<R> op) throws Exception {
        if (obj != null) {
            Renderable renderable = obj instanceof Renderable ? (Renderable) obj : new ObjectRenderable(obj);
            Map<String, Renderable> map = new HashMap<>();
            if (current().isPresent()) {
                map.putAll(current().get().contextMap);
            }
            map.put(key, renderable);
            return new Instance(map).call(op);
        } else {
            Map<String, Renderable> map = new HashMap<>();
            if (current().isPresent()) {
                map.putAll(current().get().contextMap);
            }
            map.remove(key);
            return new Instance(map).call(op);
        }
    }

    /**
     * Creates a ScopedContext with a single key/value pair and calls a method on a separate Thread.
     * @param key the key.
     * @param obj the value associated with the key.
     * @param executorService the ExecutorService to dispatch the work.
     * @param op the Callable to call.
     */
    public static <R> Future<R> callWhere(String key, Object obj, ExecutorService executorService, Callable<R> op)
            throws Exception {
        if (obj != null) {
            Renderable renderable = obj instanceof Renderable ? (Renderable) obj : new ObjectRenderable(obj);
            Map<String, Renderable> map = new HashMap<>();
            if (current().isPresent()) {
                map.putAll(current().get().contextMap);
            }
            map.put(key, renderable);
            if (executorService != null) {
                return executorService.submit(new Caller<R>(
                        new Instance(map), ThreadContext.getContext(), ThreadContext.getImmutableStack(), op));
            } else {
                R ret = new Instance(map).call(op);
                return CompletableFuture.completedFuture(ret);
            }
        } else {
            if (executorService != null) {
                Map<String, Renderable> map = new HashMap<>();
                if (current().isPresent()) {
                    map.putAll(current().get().contextMap);
                }
                map.remove(key);
                return executorService.submit(new Caller<R>(
                        new Instance(map), ThreadContext.getContext(), ThreadContext.getImmutableStack(), op));
            } else {
                R ret = op.call();
                return CompletableFuture.completedFuture(ret);
            }
        }
    }

    /**
     * Creates a ScopedContext with a Map of keys and values and calls a method.
     * @param map the Map.
     * @param op the Runnable to call.
     */
    public static <R> R callWhere(Map<String, ?> map, Callable<R> op) throws Exception {
        if (map != null && !map.isEmpty()) {
            Map<String, Renderable> renderableMap = new HashMap<>();
            if (current().isPresent()) {
                renderableMap.putAll(current().get().contextMap);
            }
            map.forEach((key, value) -> {
                renderableMap.put(key, value instanceof Renderable ? (Renderable) value : new ObjectRenderable(value));
            });
            return new Instance(renderableMap).call(op);
        } else {
            return op.call();
        }
    }

    /**
     * Returns an Optional holding the active ScopedContext.Instance
     * @return an Optional containing the active ScopedContext, if there is one.
     */
    private static Optional<Instance> current() {
        return ScopedContextDataProvider.getContext();
    }

    public static class Instance {

        private final Instance parent;
        private final String key;
        private final Renderable value;
        private final Map<String, Renderable> contextMap;

        private Instance() {
            this.parent = null;
            this.key = null;
            this.value = null;
            this.contextMap = null;
        }

        private Instance(Map<String, Renderable> map) {
            this.parent = null;
            this.key = null;
            this.value = null;
            this.contextMap = map;
        }

        private Instance(Instance parent, String key, Renderable value) {
            this.parent = parent;
            this.key = key;
            this.value = value;
            this.contextMap = null;
        }

        /**
         * Adds a key/value pair to the ScopedContext being constructed.
         *
         * @param key   the key to add.
         * @param value the value associated with the key.
         * @return the ScopedContext being constructed.
         */
        public Instance where(String key, Object value) {
            return addObject(key, value);
        }

        /**
         * Adds a key/value pair to the ScopedContext being constructed.
         *
         * @param key      the key to add.
         * @param supplier the function to generate the value.
         * @return the ScopedContext being constructed.
         */
        public Instance where(String key, Supplier<Object> supplier) {
            return addObject(key, supplier.get());
        }

        private Instance addObject(String key, Object obj) {
            if (obj != null) {
                Renderable renderable = obj instanceof Renderable ? (Renderable) obj : new ObjectRenderable(obj);
                return new Instance(this, key, renderable);
            }
            return this;
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext.
         *
         * @param op the code block to execute.
         */
        public void run(Runnable op) {
            new Runner(this, null, null, op).run();
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param op the code block to execute.
         * @return a Future representing pending completion of the task
         */
        public Future<?> run(ExecutorService executorService, Runnable op) {
            return executorService.submit(
                    new Runner(this, ThreadContext.getContext(), ThreadContext.getImmutableStack(), op));
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext.
         *
         * @param op the code block to execute.
         * @return the return value from the code block.
         */
        public <R> R call(Callable<R> op) throws Exception {
            return new Caller<R>(this, null, null, op).call();
        }

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param op the code block to execute.
         * @return a Future representing pending completion of the task
         */
        public <R> Future<R> call(ExecutorService executorService, Callable<R> op) {
            return executorService.submit(
                    new Caller<R>(this, ThreadContext.getContext(), ThreadContext.getImmutableStack(), op));
        }
    }

    private static class Runner implements Runnable {
        private final Map<String, Renderable> contextMap = new HashMap<>();
        private final Map<String, String> threadContextMap;
        private final ThreadContext.ContextStack contextStack;
        private final Instance context;
        private final Runnable op;

        public Runner(
                Instance context,
                Map<String, String> threadContextMap,
                ThreadContext.ContextStack contextStack,
                Runnable op) {
            this.context = context;
            this.threadContextMap = threadContextMap;
            this.contextStack = contextStack;
            this.op = op;
        }

        @Override
        public void run() {
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
                scopedContext = new Instance(contextMap);
            }
            if (threadContextMap != null && !threadContextMap.isEmpty()) {
                ThreadContext.putAll(threadContextMap);
            }
            if (contextStack != null) {
                ThreadContext.setStack(contextStack);
            }
            ScopedContextDataProvider.addScopedContext(scopedContext);
            try {
                op.run();
            } finally {
                ScopedContextDataProvider.removeScopedContext();
                ThreadContext.clearAll();
            }
        }
    }

    private static class Caller<R> implements Callable<R> {
        private final Map<String, Renderable> contextMap = new HashMap<>();
        private final Instance context;
        private final Map<String, String> threadContextMap;
        private final ThreadContext.ContextStack contextStack;
        private final Callable<R> op;

        public Caller(
                Instance context,
                Map<String, String> threadContextMap,
                ThreadContext.ContextStack contextStack,
                Callable<R> op) {
            this.context = context;
            this.threadContextMap = threadContextMap;
            this.contextStack = contextStack;
            this.op = op;
        }

        @Override
        public R call() throws Exception {
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
                scopedContext = new Instance(contextMap);
            }
            if (threadContextMap != null && !threadContextMap.isEmpty()) {
                ThreadContext.putAll(threadContextMap);
            }
            if (contextStack != null) {
                ThreadContext.setStack(contextStack);
            }
            ScopedContextDataProvider.addScopedContext(scopedContext);
            try {
                return op.call();
            } finally {
                ScopedContextDataProvider.removeScopedContext();
                ThreadContext.clearAll();
            }
        }
    }

    /**
     * Interface for converting Objects stored in the ContextScope to Strings for logging.
     *
     * Users implementing this interface are encouraged to make the render method as lightweight as possible,
     * Typically by creating the String representation of the object during its construction and just returning
     * the String.
     */
    public static interface Renderable {
        /**
         * Render the object as a String.
         * @return the String representation of the Object.
         */
        default String render() {
            return this.toString();
        }

        default Object getObject() {
            return this;
        }
    }

    private static class ObjectRenderable implements Renderable {
        private final Object object;

        public ObjectRenderable(Object object) {
            this.object = object;
        }

        @Override
        public String render() {
            return object.toString();
        }

        @Override
        public Object getObject() {
            return object;
        }
    }
}
