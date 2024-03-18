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
import java.util.function.Supplier;
import org.apache.logging.log4j.internal.ScopedContextAnchor;

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

    public static final ScopedContext INITIAL_CONTEXT = new ScopedContext();

    /**
     * @hidden
     * Returns an unmodifiable copy of the current ScopedContext Map. This method should
     * only be used by implementations of Log4j API.
     * @return the Map of Renderable objects.
     */
    public static Map<String, Renderable> getContextMap() {
        Optional<ScopedContext> context = ScopedContextAnchor.getContext();
        if (context.isPresent()
                && context.get().contextMap != null
                && !context.get().contextMap.isEmpty()) {
            return Collections.unmodifiableMap(context.get().contextMap);
        }
        return Collections.emptyMap();
    }

    /**
     * Return the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        Optional<ScopedContext> context = ScopedContextAnchor.getContext();
        if (context.isPresent()) {
            Renderable renderable = context.get().contextMap.get(key);
            if (renderable != null) {
                return (T) renderable.getObject();
            }
        }
        return null;
    }

    /**
     * Returns an Optional holding the active ScopedContext.
     * @return an Optional containing the active ScopedContext, if there is one.
     */
    public static Optional<ScopedContext> current() {
        return ScopedContextAnchor.getContext();
    }

    private final ScopedContext parent;
    private final String key;
    private final Renderable value;
    private final Map<String, Renderable> contextMap;

    private ScopedContext() {
        this.parent = null;
        this.key = null;
        this.value = null;
        this.contextMap = null;
    }

    private ScopedContext(Map<String, Renderable> map) {
        this.parent = null;
        this.key = null;
        this.value = null;
        this.contextMap = map;
    }

    private ScopedContext(ScopedContext parent, String key, Renderable value) {
        this.parent = parent;
        this.key = key;
        this.value = value;
        this.contextMap = null;
    }

    /**
     * Adds a key/value pair to the ScopedContext being constructed.
     * @param key the key to add.
     * @param value the value associated with the key.
     * @return the ScopedContext being constructed.
     */
    public ScopedContext where(String key, Object value) {
        return addObject(key, value);
    }

    /**
     * Adds a key/value pair to the ScopedContext being constructed.
     * @param key the key to add.
     * @param supplier the function to generate the value.
     * @return the ScopedContext being constructed.
     */
    public ScopedContext where(String key, Supplier<Object> supplier) {
        return addObject(key, supplier.get());
    }

    private ScopedContext addObject(String key, Object obj) {
        if (obj != null) {
            Renderable renderable = obj instanceof Renderable ? (Renderable) obj : new ObjectRenderable(obj);
            return new ScopedContext(this, key, renderable);
        }
        return this;
    }

    /**
     * Executes a code block that includes all the key/value pairs added to the ScopedContext.
     * @param op the code block to execute.
     */
    public void run(Runnable op) {
        new ScopedContextRunner(this, op).run();
    }

    /**
     * Executes a code block that includes all the key/value pairs added to the ScopedContext.
     * @param op the code block to execute.
     * @return the return value from the code block.
     */
    public <R> R call(Callable<R> op) throws Exception {
        return new ScopedContextCaller<R>(this, op).call();
    }

    private static class ScopedContextRunner implements Runnable {
        private final Map<String, Renderable> contextMap = new HashMap<>();
        private final ScopedContext context;
        private final Runnable op;

        public ScopedContextRunner(ScopedContext context, Runnable op) {
            this.context = context;
            this.op = op;
        }

        @Override
        public void run() {
            ScopedContext scopedContext = context;
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
                scopedContext = new ScopedContext(contextMap);
            }
            ScopedContextAnchor.addScopedContext(scopedContext);
            try {
                op.run();
            } finally {
                ScopedContextAnchor.removeScopedContext();
            }
        }
    }

    private static class ScopedContextCaller<R> implements Callable<R> {
        private final Map<String, Renderable> contextMap = new HashMap<>();
        private final ScopedContext context;
        private final Callable<R> op;

        public ScopedContextCaller(ScopedContext context, Callable<R> op) {
            this.context = context;
            this.op = op;
        }

        @Override
        public R call() throws Exception {
            ScopedContext scopedContext = context;
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
                scopedContext = new ScopedContext(contextMap);
            }
            ScopedContextAnchor.addScopedContext(scopedContext);
            try {
                return op.call();
            } finally {
                ScopedContextAnchor.removeScopedContext();
            }
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
