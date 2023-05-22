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
package org.apache.logging.log4j.plugins.di.spi;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.StringJoiner;

import org.apache.logging.log4j.plugins.di.Key;

class DependencyChains {
    static final DependencyChain EMPTY = new EmptyChain();

    static DependencyChain singleton(final Key<?> key) {
        return new LinkedChain(key);
    }

    private static final class EmptyChain implements DependencyChain {
        @Override
        public boolean hasDependency(final Key<?> key) {
            return false;
        }

        @Override
        public DependencyChain withDependency(final Key<?> key) {
            return new LinkedChain(key);
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Iterator<Key<?>> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(final Object o) {
            return this == o || o != null && getClass() == o.getClass();
        }

        @Override
        public String toString() {
            return "[]";
        }
    }

    private static final class LinkedChain implements DependencyChain {
        private final Key<?> head;
        private final LinkedChain tail;

        private LinkedChain(final Key<?> head) {
            this(head, null);
        }

        private LinkedChain(final Key<?> head, final LinkedChain tail) {
            this.head = head;
            this.tail = tail;
        }

        @Override
        public boolean hasDependency(final Key<?> key) {
            return key.equals(head) || tail != null && tail.hasDependency(key);
        }

        @Override
        public LinkedChain withDependency(final Key<?> key) {
            if (key.equals(head)) {
                return this;
            }
            final LinkedChain newTail = tail != null ? tail.withDependency(key) : new LinkedChain(key);
            return new LinkedChain(head, newTail);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<Key<?>> iterator() {
            return new Iter(this);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final LinkedChain keys = (LinkedChain) o;
            return head.equals(keys.head) && Objects.equals(tail, keys.tail);
        }

        @Override
        public int hashCode() {
            return Objects.hash(head, tail);
        }

        @Override
        public String toString() {
            final StringJoiner joiner = new StringJoiner(", ", "[", "]");
            for (final Key<?> key : this) {
                joiner.add(key.toString());
            }
            return joiner.toString();
        }

        private static final class Iter implements Iterator<Key<?>> {
            private LinkedChain current;

            private Iter(final LinkedChain current) {
                this.current = current;
            }

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public Key<?> next() {
                final Key<?> head = current.head;
                current = current.tail;
                return head;
            }
        }
    }
}
