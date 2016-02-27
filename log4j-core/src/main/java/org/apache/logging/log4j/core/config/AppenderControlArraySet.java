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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Data structure with similar semantics to CopyOnWriteArraySet, but giving direct access to the underlying array.
 *
 * @since 2.6
 */
@PerformanceSensitive
public class AppenderControlArraySet {
    private final AtomicReference<AppenderControl[]> appenderArray = new AtomicReference<>(new AppenderControl[0]);

    /**
     * Adds an AppenderControl to this set. If this set already contains the element, the call leaves the set unchanged
     * and returns false.
     *
     * @param control The AppenderControl to add.
     * @return true if this set did not already contain the specified element
     */
    public boolean add(final AppenderControl control) {
        boolean success;
        do {
            final AppenderControl[] original = appenderArray.get();
            for (final AppenderControl existing : original) {
                if (existing.equals(control)) {
                    return false; // the appender is already in the list
                }
            }
            final AppenderControl[] copy = Arrays.copyOf(original, original.length + 1);
            copy[copy.length - 1] = control;
            success = appenderArray.compareAndSet(original, copy);
        } while (!success); // could not swap: array was modified by another thread
        return true; // successfully added
    }

    /**
     * Removes the AppenderControl with the specific name and returns it (or {@code null} if no such appender existed).
     *
     * @param name The name of the AppenderControl to remove
     * @return the removed AppenderControl or {@code null}
     */
    public AppenderControl remove(final String name) {
        boolean success;
        do {
            success = true;
            final AppenderControl[] original = appenderArray.get();
            for (int i = 0; i < original.length; i++) {
                final AppenderControl appenderControl = original[i];
                if (Objects.equals(name, appenderControl.getAppenderName())) {
                    final AppenderControl[] copy = removeElementAt(i, original);
                    if (appenderArray.compareAndSet(original, copy)) {
                        return appenderControl; // successfully removed
                    }
                    success = false; // could not swap: array was modified by another thread
                    break;
                }
            }
        } while (!success);
        return null; // not found
    }

    private AppenderControl[] removeElementAt(final int i, final AppenderControl[] array) {
        final AppenderControl[] result = Arrays.copyOf(array, array.length - 1);
        System.arraycopy(array, i + 1, result, i, result.length - i);
        return result;
    }

    /**
     * Returns all Appenders as a Map.
     *
     * @return a Map with the Appender name as the key and the Appender as the value.
     */
    public Map<String, Appender> asMap() {
        final Map<String, Appender> result = new HashMap<>();
        for (final AppenderControl appenderControl : appenderArray.get()) {
            result.put(appenderControl.getAppenderName(), appenderControl.getAppender());
        }
        return result;
    }

    /**
     * Atomically sets the values to an empty array and returns the old array.
     *
     * @return the contents before this collection was cleared.
     */
    public AppenderControl[] clear() {
        return appenderArray.getAndSet(new AppenderControl[0]);
    }

    public boolean isEmpty() {
        return appenderArray.get().length == 0;
    }

    /**
     * Returns the underlying array.
     *
     * @return the array supporting this collection
     */
    public AppenderControl[] get() {
        return appenderArray.get();
    }
}
