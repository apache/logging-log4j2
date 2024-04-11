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

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.apache.logging.log4j.spi.ScopedContextProvider;
import org.apache.logging.log4j.util.ProviderUtil;

/**
 * Context that can be used for data to be logged in a block of code.
 * <p>
 * While this is influenced by ScopedValues from Java 21 it does not share the same API. While it can perform a
 * similar function as a set of ScopedValues it is really meant to allow a block of code to include a set of keys and
 * values in all the log events within that block. The underlying implementation must provide support for
 * logging the ScopedContext for that to happen.
 * </p>
 * <p>
 * The ScopedContext will not be bound to the current thread until either a run or call method is invoked. The
 * contexts are nested so creating and running or calling via a second ScopedContext will result in the first
 * ScopedContext being hidden until the call is returned. Thus the values from the first ScopedContext need to
 * be added to the second to be included.
 * </p>
 * <p>
 * The ScopedContext can be passed to child threads by including the ExecutorService to be used to manage the
 * run or call methods. The caller should interact with the ExecutorService as if they were submitting their
 * run or call methods directly to it. The ScopedContext performs no error handling other than to ensure the
 * ThreadContext and ScopedContext are cleaned up from the executed Thread.
 * </p>
 * @since 2.24.0
 */
public final class ScopedContext {

    private static final ScopedContextProvider provider =
            ProviderUtil.getProvider().getScopedContextProvider();

    private ScopedContext() {}

    /**
     * Creates a ScopedContext Instance with a key/value pair.
     *
     * @param key   the key to add.
     * @param value the value associated with the key.
     * @return the Instance constructed if a valid key and value were provided. Otherwise, either the
     * current Instance is returned or a new Instance is created if there is no current Instance.
     */
    public static Instance where(final String key, final Object value) {
        return provider.newScopedContext(key, value);
    }

    /**
     * Adds a key/value pair to the ScopedContext being constructed.
     *
     * @param key      the key to add.
     * @param supplier the function to generate the value.
     * @return the ScopedContext being constructed.
     */
    public static Instance where(final String key, final Supplier<Object> supplier) {
        return where(key, supplier.get());
    }

    /**
     * Creates a ScopedContext Instance with a Map of keys and values.
     * @param map the Map.
     * @return the ScopedContext Instance constructed.
     */
    public static Instance where(final Map<String, ?> map) {
        return provider.newScopedContext(map);
    }

    public static Instance withThreadContext() {
        return provider.newScopedContext(true);
    }

    /**
     * Creates a ScopedContext with a single key/value pair and calls a method.
     * @param key the key.
     * @param value the value associated with the key.
     * @param task the Runnable to call.
     */
    public static void runWhere(final String key, final Object value, final Runnable task) {
        provider.newScopedContext(key, value).run(task);
    }

    /**
     * Creates a ScopedContext with a single key/value pair and calls a method on a separate Thread.
     * @param key the key.
     * @param value the value associated with the key.
     * @param executorService the ExecutorService to dispatch the work.
     * @param task the Runnable to call.
     */
    public static Future<Void> runWhere(
            final String key, final Object value, final ExecutorService executorService, final Runnable task) {
        return provider.newScopedContext(key, value).run(executorService, task);
    }

    /**
     * Creates a ScopedContext with a Map of keys and values and calls a method.
     * @param map the Map.
     * @param task the Runnable to call.
     */
    public static void runWhere(final Map<String, ?> map, final Runnable task) {
        provider.newScopedContext(map).run(task);
    }

    /**
     * Creates a ScopedContext with a single key/value pair and calls a method.
     * @param key the key.
     * @param value the value associated with the key.
     * @param task the Runnable to call.
     */
    public static <R> R callWhere(final String key, final Object value, final Callable<R> task) throws Exception {
        return provider.newScopedContext(key, value).call(task);
    }

    /**
     * Creates a ScopedContext with a single key/value pair and calls a method on a separate Thread.
     * @param key the key.
     * @param value the value associated with the key.
     * @param executorService the ExecutorService to dispatch the work.
     * @param task the Callable to call.
     */
    public static <R> Future<R> callWhere(
            final String key, final Object value, final ExecutorService executorService, final Callable<R> task) {
        return provider.newScopedContext(key, value).call(executorService, task);
    }

    /**
     * Creates a ScopedContext with a Map of keys and values and calls a method.
     * @param map the Map.
     * @param task the Runnable to call.
     */
    public static <R> R callWhere(final Map<String, ?> map, final Callable<R> task) throws Exception {
        return provider.newScopedContext(map).call(task);
    }

    /**
     * Return the object with the specified key from the current context.
     * @param key the key.
     * @return the value of the key or null.
     */
    public static Object get(String key) {
        return provider.getValue(key);
    }

    /**
     * Return String value of the key from the current ScopedContext, if there is one and the key exists.
     * @param key The key.
     * @return The value of the key in the current ScopedContext.
     */
    public static String getString(String key) {
        return provider.getString(key);
    }

    /**
     * A holder of scoped context data.
     */
    public interface Instance {

        /**
         * Adds a key/value pair to the ScopedContext being constructed.
         *
         * @param key   the key to add.
         * @param value the value associated with the key.
         * @return the ScopedContext being constructed.
         */
        Instance where(String key, Object value);

        /**
         * Adds a key/value pair to the ScopedContext being constructed.
         *
         * @param key      the key to add.
         * @param supplier the function to generate the value.
         * @return the ScopedContext being constructed.
         */
        Instance where(String key, Supplier<Object> supplier);

        /**
         * Creates an Instance to declare the ThreadContext should be included.
         *
         * @return the ScopedContext being constructed.
         */
        Instance withThreadContext();

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext.
         *
         * @param task the code block to execute.
         */
        void run(Runnable task);

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        Future<Void> run(ExecutorService executorService, Runnable task);

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param key   the key to add.
         * @param value the value associated with the key.
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        Future<Void> runWhere(String key, Object value, ExecutorService executorService, Runnable task);

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param key   the key to add.
         * @param supplier the function to generate the value.
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        Future<Void> runWhere(String key, Supplier<Object> supplier, ExecutorService executorService, Runnable task);

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext.
         *
         * @param task the code block to execute.
         * @return the return value from the code block.
         */
        <R> R call(Callable<R> task) throws Exception;

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        <R> Future<R> call(ExecutorService executorService, Callable<R> task);

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param key   the key to add.
         * @param value the value associated with the key.
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        <R> Future<R> callWhere(String key, Object value, ExecutorService executorService, Callable<R> task);

        /**
         * Executes a code block that includes all the key/value pairs added to the ScopedContext on a different Thread.
         *
         * @param key   the key to add.
         * @param supplier the function to generate the value.
         * @param executorService The ExecutorService to use.
         * @param task the code block to execute.
         * @return a Future representing pending completion of the task
         */
        <R> Future<R> callWhere(
                String key, Supplier<Object> supplier, ExecutorService executorService, Callable<R> task);

        /**
         * Wraps the provided Runnable method with a Runnable method that will instantiate the Scoped and Thread
         * Contexts in the target Thread before the caller's run method is called.
         * @param task the Runnable task to perform.
         * @return a Runnable.
         */
        Runnable wrap(Runnable task);

        /**
         * Wraps the provided Callable method with a Callable method that will instantiate the Scoped and Thread
         * Contexts in the target Thread before the caller's call method is called.
         * @param task the Callable task to perform.
         * @return a Callable.
         */
        <R> Callable<R> wrap(Callable<R> task);
    }
}
