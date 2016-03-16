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
package org.apache.logging.log4j.core.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ThreadLocal that can clean up all threads.
 * @param <T> The Object type being stored in the ThreadLocal.
 */
public class Log4jThreadLocal<T> extends ThreadLocal<T> {

    private ConcurrentMap<Long, AtomicReference<T>> containers = new ConcurrentHashMap<>();
    private ThreadLocal<AtomicReference<T>> threadLocal = new ThreadLocal<>();

    @Override
    public void set(T value) {
        AtomicReference<T> container = getContainer(Thread.currentThread().getId());
        container.set(value);
        threadLocal.set(container);
    }

    @Override
    public T get() {
        AtomicReference<T> container = threadLocal.get();
        if (container != null) {
            return container.get();
        }
        long id = Thread.currentThread().getId();
        container = getContainer(id);
        T value = initialValue();
        container.set(value);
        containers.put(id, container);
        return value;
    }

    @Override
    public void remove() {
        AtomicReference<T> container = threadLocal.get();
        containers.remove(Thread.currentThread().getId());
        threadLocal.remove();
        container.set(null);
    }

    public void clear() {
        for (Map.Entry<Long, AtomicReference<T>> entry : containers.entrySet()) {
            if (entry.getValue() != null) {
                entry.getValue().set(null);
            }
        }
        containers.clear();
    }

    private AtomicReference<T> getContainer(long id) {
        AtomicReference<T> container = containers.get(id);
        if (container == null) {
            container = new AtomicReference<>();
            containers.put(Thread.currentThread().getId(), container);
        }
        return container;
    }

}
