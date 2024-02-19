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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds entries to the {@link ThreadContext stack or map} and them removes them when the object is closed, e.g. as part
 * of a try-with-resources. User code can now look like this:
 * <pre>
 * try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put(key1, value1).put(key2, value2)) {
 *     callSomeMethodThatLogsALot();
 *
 * // Entries for key1 and key2 are automatically removed from the ThreadContext map when done.
 * }
 * </pre>
 *
 * @since 2.6
 */
public class CloseableThreadContext {

    private CloseableThreadContext() {}

    /**
     * Pushes new diagnostic context information on to the Thread Context Stack. The information will be popped off when
     * the instance is closed.
     *
     * @param message The new diagnostic context information.
     * @return a new instance that will back out the changes when closed.
     */
    public static CloseableThreadContext.Instance push(final String message) {
        return new CloseableThreadContext.Instance().push(message);
    }

    /**
     * Pushes new diagnostic context information on to the Thread Context Stack. The information will be popped off when
     * the instance is closed.
     *
     * @param message The new diagnostic context information.
     * @param args    Parameters for the message.
     * @return a new instance that will back out the changes when closed.
     */
    public static CloseableThreadContext.Instance push(final String message, final Object... args) {
        return new CloseableThreadContext.Instance().push(message, args);
    }

    /**
     * Populates the Thread Context Map with the supplied key/value pair. Any existing key in the
     * {@link ThreadContext} will be replaced with the supplied value, and restored back to their original value when
     * the instance is closed.
     *
     * @param key   The  key to be added
     * @param value The value to be added
     * @return a new instance that will back out the changes when closed.
     */
    public static CloseableThreadContext.Instance put(final String key, final String value) {
        return new CloseableThreadContext.Instance().put(key, value);
    }

    /**
     * Populates the Thread Context Stack with the supplied stack. The information will be popped off when
     * the instance is closed.
     *
     * @param messages The list of messages to be added
     * @return a new instance that will back out the changes when closed.
     * @since 2.8
     */
    public static CloseableThreadContext.Instance pushAll(final List<String> messages) {
        return new CloseableThreadContext.Instance().pushAll(messages);
    }

    /**
     * Populates the Thread Context Map with the supplied key/value pairs. Any existing keys in the
     * {@link ThreadContext} will be replaced with the supplied values, and restored back to their original value when
     * the instance is closed.
     *
     * @param values The map of key/value pairs to be added
     * @return a new instance that will back out the changes when closed.
     * @since 2.8
     */
    public static CloseableThreadContext.Instance putAll(final Map<String, String> values) {
        return new CloseableThreadContext.Instance().putAll(values);
    }

    public static class Instance implements AutoCloseable {

        private int pushCount = 0;
        private final Map<String, String> originalValues = new HashMap<>();

        private Instance() {}

        /**
         * Pushes new diagnostic context information on to the Thread Context Stack.
         * The information will be popped off when the instance is closed.
         *
         * @param message The new diagnostic context information.
         * @return the instance that will back out the changes when closed.
         */
        public Instance push(final String message) {
            ThreadContext.push(message);
            pushCount++;
            return this;
        }

        /**
         * Pushes new diagnostic context information on to the Thread Context Stack.
         * The information will be popped off when the instance is closed.
         *
         * @param message The new diagnostic context information.
         * @param args    Parameters for the message.
         * @return the instance that will back out the changes when closed.
         */
        public Instance push(final String message, final Object[] args) {
            ThreadContext.push(message, args);
            pushCount++;
            return this;
        }

        /**
         * Populates the Thread Context Map with the supplied key/value pair. Any existing key in the
         * {@link ThreadContext} will be replaced with the supplied value,
         * and restored back to their original value when the instance is closed.
         *
         * @param key   The  key to be added
         * @param value The value to be added
         * @return a new instance that will back out the changes when closed.
         */
        public Instance put(final String key, final String value) {
            // If there are no existing values, a null will be stored as an old value
            if (!originalValues.containsKey(key)) {
                originalValues.put(key, ThreadContext.get(key));
            }
            ThreadContext.put(key, value);
            return this;
        }

        /**
         * Populates the Thread Context Map with the supplied key/value pairs. Any existing keys in the
         * {@link ThreadContext} will be replaced with the supplied values,
         * and restored back to their original value when the instance is closed.
         *
         * @param values The map of key/value pairs to be added
         * @return a new instance that will back out the changes when closed.
         * @since 2.8
         */
        public Instance putAll(final Map<String, String> values) {
            final Map<String, String> currentValues = ThreadContext.getContext();
            ThreadContext.putAll(values);
            for (final String key : values.keySet()) {
                if (!originalValues.containsKey(key)) {
                    originalValues.put(key, currentValues.get(key));
                }
            }
            return this;
        }

        /**
         * Populates the Thread Context Stack with the supplied stack. The information will be popped off when
         * the instance is closed.
         *
         * @param messages The list of messages to be added
         * @return a new instance that will back out the changes when closed.
         * @since 2.8
         */
        public Instance pushAll(final List<String> messages) {
            for (final String message : messages) {
                push(message);
            }
            return this;
        }

        /**
         * Removes the values from the {@link ThreadContext}.
         * <p>
         * Values pushed to the {@link ThreadContext} <em>stack</em> will be popped off.
         * </p>
         * <p>
         * Values put on the {@link ThreadContext} <em>map</em> will be removed,
         * or restored to their original values it they already existed.
         * </p>
         */
        @Override
        public void close() {
            closeStack();
            closeMap();
        }

        private void closeMap() {
            final Map<String, String> valuesToReplace = new HashMap<>(originalValues.size());
            final List<String> keysToRemove = new ArrayList<>(originalValues.size());
            for (final Map.Entry<String, String> entry : originalValues.entrySet()) {
                final String key = entry.getKey();
                final String originalValue = entry.getValue();
                if (null == originalValue) {
                    keysToRemove.add(key);
                } else {
                    valuesToReplace.put(key, originalValue);
                }
            }
            if (!valuesToReplace.isEmpty()) {
                ThreadContext.putAll(valuesToReplace);
            }
            if (!keysToRemove.isEmpty()) {
                ThreadContext.removeAll(keysToRemove);
            }
        }

        private void closeStack() {
            for (int i = 0; i < pushCount; i++) {
                ThreadContext.pop();
            }
            pushCount = 0;
        }
    }
}
