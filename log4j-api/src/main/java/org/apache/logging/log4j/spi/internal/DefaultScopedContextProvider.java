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

import java.util.Optional;
import org.apache.logging.log4j.spi.AbstractScopedContextProvider;
import org.apache.logging.log4j.spi.ScopedContextProvider;

/**
 * An implementation of {@link ScopedContextProvider}.
 * @since 2.24.0
 */
public class DefaultScopedContextProvider extends AbstractScopedContextProvider {

    public static final ScopedContextProvider INSTANCE = new DefaultScopedContextProvider();
    private static final int ENTRIES = 4;

    private final ThreadLocal<InstanceArray> scopedContext = new ThreadLocal<>();

    /**
     * Returns an immutable Map containing all the key/value pairs as Object objects.
     * @return An immutable copy of the Map at the current scope.
     */
    @Override
    protected Optional<Instance> getContext() {
        final InstanceArray array = scopedContext.get();
        return array != null ? Optional.of(array.array[array.index]) : Optional.empty();
    }

    /**
     * Add the ScopeContext.
     * @param context The ScopeContext.
     */
    @Override
    protected void addScopedContext(final Instance context) {
        InstanceArray array = scopedContext.get();
        if (array == null) {
            array = new InstanceArray();
            scopedContext.set(array);
        }
        array.add(context);
    }

    /**
     * Remove the top ScopeContext.
     */
    @Override
    protected void removeScopedContext() {
        final InstanceArray array = scopedContext.get();
        if (array != null) {
            if (!array.isEmpty()) {
                array.remove();
            }
            if (array.isEmpty()) {
                scopedContext.remove();
            }
        }
    }

    private static class InstanceArray {
        public int index;
        public Instance[] array;

        public InstanceArray() {
            this(ENTRIES);
        }

        public InstanceArray(int capacity) {
            this.index = -1;
            array = new Instance[capacity];
        }

        public boolean isEmpty() {
            return index < 0;
        }

        public void add(Instance instance) {
            int next = ++index;
            if (next == array.length) {
                expand();
            }
            array[next] = instance;
        }

        public void remove() {
            array[index--] = null;
        }

        private void expand() {
            Instance[] newArray = new Instance[array.length + ENTRIES];
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
        }
    }
}
