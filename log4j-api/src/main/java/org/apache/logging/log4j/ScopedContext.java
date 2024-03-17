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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

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
 * @since 2.24.0
 */
public class ScopedContext {

    private static final ThreadLocal<Deque<Map<String, Renderable>>> scopedContext = new ThreadLocal<>();

    /**
     * Returns an immutable Map containing all the key/value pairs as Renderable objects.
     * @return An immutable copy of the Map at the current scope.
     */
    public static Map<String, Renderable> getContext() {
        Deque<Map<String, Renderable>> stack = scopedContext.get();
        if (stack != null && !stack.isEmpty()) {
            return Collections.unmodifiableMap(stack.getFirst());
        }
        return Collections.emptyMap();
    }

    private static void addScopedContext(Map<String, Renderable> contextMap) {
        Deque<Map<String, Renderable>> stack = scopedContext.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            scopedContext.set(stack);
        }
        stack.addFirst(contextMap);
    }

    private static void removeScopedContext() {
        Deque<Map<String, Renderable>> stack = scopedContext.get();
        if (stack != null) {
            if (!stack.isEmpty()) {
                stack.removeFirst();
            }
            if (stack.isEmpty()) {
                scopedContext.remove();
            }
        }
    }

    /**
     * Return a new ScopedContext.
     * @return the ScopedContext.
     */
    public static ScopedContext newInstance() {
        return newInstance(false);
    }

    /**
     * Return a new ScopedContext.
     * @param inherit true if this context should inherit the values of its parent.
     * @return the ScopedContext.
     */
    public static ScopedContext newInstance(boolean inherit) {
        return new ScopedContext(inherit);
    }

    /**
     * Return the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        Renderable renderable = getContext().get(key);
        if (renderable != null) {
            return (T) renderable.getObject();
        } else {
            return null;
        }
    }

    private final Map<String, Renderable> contextMap = new HashMap<>();

    private ScopedContext(boolean inherit) {
        Map<String, Renderable> parent = ScopedContext.getContext();
        if (inherit && !parent.isEmpty()) {
            contextMap.putAll(parent);
        }
    }

    /**
     * Add all the values in the specified Map to the ScopedContext being constructed.
     *
     * @param map The Map to add to the ScopedContext being constructed.
     * @return the ScopedContext being constructed.
     */
    public ScopedContext where(Map<String, Object> map) {
        map.forEach(this::addObject);
        return this;
    }

    /**
     * Adds a key/value pair to the ScopedContext being constructed.
     * @param key the key to add.
     * @param value the value associated with the key.
     * @return the ScopedContext being constructed.
     */
    public ScopedContext where(String key, Object value) {
        addObject(key, value);
        return this;
    }

    /**
     * Adds a key/value pair to the ScopedContext being constructed.
     * @param key the key to add.
     * @param supplier the function to generate the value.
     * @return the ScopedContext being constructed.
     */
    public ScopedContext where(String key, Supplier<Object> supplier) {
        addObject(key, supplier.get());
        return this;
    }

    private void addObject(String key, Object obj) {
        if (obj != null) {
            if (obj instanceof Renderable) {
                contextMap.put(key, (Renderable) obj);
            } else {
                contextMap.put(key, new ObjectRenderable(obj));
            }
        }
    }

    /**
     * Executes a code block that includes all the key/value pairs added to the ScopedContext.
     * @param op the code block to execute.
     */
    public void run(Runnable op) {
        addScopedContext(contextMap);
        try {
            op.run();
        } finally {
            removeScopedContext();
        }
    }

    /**
     * Executes a code block that includes all the key/value pairs added to the ScopedContext.
     * @param op the code block to execute.
     * @return the return value from the code block.
     */
    public <R> R call(Callable<? extends R> op) throws Exception {
        addScopedContext(contextMap);
        try {
            return op.call();
        } finally {
            removeScopedContext();
        }
    }

    /**
     * Interface for converting Objects stored in the ContextScope to Strings for logging.
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
