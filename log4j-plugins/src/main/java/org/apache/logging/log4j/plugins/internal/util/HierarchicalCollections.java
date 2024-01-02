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
package org.apache.logging.log4j.plugins.internal.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class HierarchicalCollections {
    static <K, V> HierarchicalMap<K, V> newRootMap() {
        return new RootMap<>();
    }

    private static class RootMap<K, V> extends AbstractMap<K, V> implements HierarchicalMap<K, V> {
        private final Map<K, V> map = new ConcurrentHashMap<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return map.entrySet();
        }

        @Override
        public V put(final K key, final V value) {
            return map.put(key, value);
        }

        @Override
        public boolean containsKey(final Object key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsLocalKey(final K key) {
            return map.containsKey(key);
        }

        @Override
        public Set<K> keySet() {
            return map.keySet();
        }

        @Override
        public HierarchicalMap<K, V> newChildMap() {
            return new ChildMap<>(this);
        }
    }

    private static final class ChildMap<K, V> extends RootMap<K, V> {
        private final Map<K, V> parent;

        private ChildMap(final Map<K, V> parent) {
            this.parent = parent;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new HierarchicalSet<>(super.entrySet(), parent.entrySet());
        }

        @Override
        public boolean containsKey(final Object key) {
            return super.containsKey(key) || parent.containsKey(key);
        }

        @Override
        public Set<K> keySet() {
            return new HierarchicalSet<>(super.keySet(), parent.keySet());
        }
    }

    private static final class HierarchicalSet<E> extends AbstractSet<E> {
        private final Set<E> delegate;
        private final Set<E> parent;

        private HierarchicalSet(final Set<E> delegate, final Set<E> parent) {
            this.delegate = delegate;
            this.parent = parent;
        }

        @Override
        public Iterator<E> iterator() {
            return new HierarchicalIterator<>(delegate.iterator(), parent.iterator());
        }

        @Override
        public int size() {
            return delegate.size() + parent.size();
        }

        @Override
        public boolean contains(final Object o) {
            return delegate.contains(o) || parent.contains(o);
        }
    }

    private static final class HierarchicalIterator<E> implements Iterator<E> {
        private final Iterator<E> delegate;
        private final Iterator<E> parent;
        private State state;

        private HierarchicalIterator(final Iterator<E> delegate, final Iterator<E> parent) {
            this.delegate = delegate;
            this.parent = parent;
            this.state = State.ITERATING_DELEGATE;
        }

        @Override
        public boolean hasNext() {
            switch (state) {
                case ITERATING_DELEGATE:
                    if (delegate.hasNext()) {
                        return true;
                    }
                    state = State.ITERATING_PARENT;

                case ITERATING_PARENT:
                    if (parent.hasNext()) {
                        return true;
                    }
                    state = State.DONE;

                case DONE:
                default:
                    return false;
            }
        }

        @Override
        public E next() {
            switch (state) {
                case ITERATING_DELEGATE:
                    if (delegate.hasNext()) {
                        return delegate.next();
                    }
                    state = State.ITERATING_PARENT;

                case ITERATING_PARENT:
                    if (parent.hasNext()) {
                        return parent.next();
                    }
                    state = State.DONE;

                case DONE:
                default:
                    throw new NoSuchElementException("Completed iteration");
            }
        }

        @Override
        public void remove() {
            switch (state) {
                case ITERATING_DELEGATE:
                    delegate.remove();
                    break;

                case ITERATING_PARENT:
                    parent.remove();
                    break;

                case DONE:
                default:
                    throw new IllegalStateException("Completed iteration");
            }
        }

        private enum State {
            ITERATING_DELEGATE,
            ITERATING_PARENT,
            DONE
        }
    }
}
