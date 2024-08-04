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
package org.apache.logging.slf4j;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.slf4j.spi.MDCAdapter;

public class Log4jMDCAdapter implements MDCAdapter {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final ThreadLocalMapOfStacks mapOfStacks = new ThreadLocalMapOfStacks();

    @Override
    public void put(final String key, final String val) {
        ThreadContext.put(key, val);
    }

    @Override
    public String get(final String key) {
        return ThreadContext.get(key);
    }

    @Override
    public void remove(final String key) {
        ThreadContext.remove(key);
    }

    @Override
    public void clear() {
        ThreadContext.clearMap();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return ThreadContext.getContext();
    }

    @Override
    public void setContextMap(final Map<String, String> map) {
        ThreadContext.clearMap();
        ThreadContext.putAll(map);
    }

    @Override
    public void pushByKey(final String key, final String value) {
        if (key == null) {
            ThreadContext.push(value);
        } else {
            final String oldValue = mapOfStacks.peekByKey(key);
            if (!Objects.equals(ThreadContext.get(key), oldValue)) {
                LOGGER.warn("The key {} was used in both the string and stack-valued MDC.", key);
            }
            mapOfStacks.pushByKey(key, value);
            ThreadContext.put(key, value);
        }
    }

    @Override
    public String popByKey(final String key) {
        if (key == null) {
            return ThreadContext.getDepth() > 0 ? ThreadContext.pop() : null;
        }
        final String value = mapOfStacks.popByKey(key);
        if (!Objects.equals(ThreadContext.get(key), value)) {
            LOGGER.warn("The key {} was used in both the string and stack-valued MDC.", key);
        }
        final String oldValue = mapOfStacks.peekByKey(key);
        if (oldValue != null) {
            ThreadContext.put(key, oldValue);
        } else {
            ThreadContext.remove(key);
        }
        return value;
    }

    @Override
    public Deque<String> getCopyOfDequeByKey(final String key) {
        if (key == null) {
            final ContextStack stack = ThreadContext.getImmutableStack();
            final Deque<String> copy = new ArrayDeque<>(stack.size());
            stack.forEach(copy::push);
            return copy;
        }
        return mapOfStacks.getCopyOfDequeByKey(key);
    }

    @Override
    public void clearDequeByKey(final String key) {
        if (key == null) {
            ThreadContext.clearStack();
        } else {
            mapOfStacks.clearByKey(key);
            ThreadContext.remove(key);
        }
    }

    // Used by tests
    void clearDeque() {
        mapOfStacks.clear();
    }

    private static class ThreadLocalMapOfStacks {

        private final ThreadLocal<Map<String, Deque<String>>> tlMapOfStacks = ThreadLocal.withInitial(HashMap::new);

        private Map<String, Deque<String>> getMapOfStacks() {
            Map<String, Deque<String>> localMap = tlMapOfStacks.get();
            if (localMap == null) {
                tlMapOfStacks.set(localMap = new HashMap<>());
            }
            return localMap;
        }

        private void removeIfEmpty(final Map<String, Deque<String>> mapOfStacks) {
            if (!Constants.ENABLE_THREADLOCALS && mapOfStacks != null && mapOfStacks.isEmpty()) {
                tlMapOfStacks.remove();
            }
        }

        public void pushByKey(final String key, final String value) {
            getMapOfStacks().computeIfAbsent(key, ignored -> new ArrayDeque<>()).push(value);
        }

        public String popByKey(final String key) {
            final Map<String, Deque<String>> mapOfStacks = tlMapOfStacks.get();
            if (mapOfStacks != null) {
                final Deque<String> deque = mapOfStacks.get(key);
                if (deque != null) {
                    final String result = deque.poll();
                    if (!Constants.ENABLE_THREADLOCALS && deque.isEmpty()) {
                        mapOfStacks.remove(key);
                        removeIfEmpty(mapOfStacks);
                    }
                    return result;
                }
            }
            return null;
        }

        public Deque<String> getCopyOfDequeByKey(final String key) {
            final Map<String, Deque<String>> mapOfStacks = tlMapOfStacks.get();
            if (mapOfStacks != null) {
                final Deque<String> deque = mapOfStacks.get(key);
                return deque != null ? new ArrayDeque<>(deque) : null;
            }
            return null;
        }

        public void clear() {
            final Map<String, Deque<String>> mapOfStacks = tlMapOfStacks.get();
            if (Constants.ENABLE_THREADLOCALS) {
                mapOfStacks.clear();
            } else {
                tlMapOfStacks.remove();
            }
        }

        public void clearByKey(final String key) {
            final Map<String, Deque<String>> mapOfStacks = tlMapOfStacks.get();
            if (mapOfStacks != null) {
                mapOfStacks.computeIfPresent(key, (ignored, deque) -> {
                    if (Constants.ENABLE_THREADLOCALS) {
                        deque.clear();
                        return deque;
                    }
                    return null;
                });
            }
            removeIfEmpty(mapOfStacks);
        }

        public String peekByKey(final String key) {
            final Map<String, Deque<String>> mapOfStacks = tlMapOfStacks.get();
            if (mapOfStacks != null) {
                final Deque<String> deque = mapOfStacks.get(key);
                return deque != null ? deque.peek() : null;
            }
            return null;
        }
    }
}
