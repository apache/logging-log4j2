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
package org.apache.logging.log4j.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.util.StringBuilderFormattable;

/**
 * TODO
 */
public class MutableThreadContextStack implements ThreadContextStack, StringBuilderFormattable {

    private static final long serialVersionUID = 50505011L;

    /**
     * The underlying list (never null).
     */
    private final List<String> list;

    private boolean frozen;

    /**
     * Constructs an empty MutableThreadContextStack.
     */
    public MutableThreadContextStack() {
        this(new ArrayList<String>());
    }

    /**
     * Constructs a new instance.
     * @param list Initial elements to be stored in this stack implementation.
     */
    public MutableThreadContextStack(final List<String> list) {
        this.list = new ArrayList<>(list);
    }

    private MutableThreadContextStack(final MutableThreadContextStack stack) {
        this.list = new ArrayList<>(stack.list);
    }

    private void checkInvariants() {
        if (frozen) {
            throw new UnsupportedOperationException("context stack has been frozen");
        }
    }

    @Override
    public String pop() {
        checkInvariants();
        if (list.isEmpty()) {
            return null;
        }
        final int last = list.size() - 1;
        final String result = list.remove(last);
        return result;
    }

    @Override
    public String peek() {
        if (list.isEmpty()) {
            return null;
        }
        final int last = list.size() - 1;
        return list.get(last);
    }

    @Override
    public void push(final String message) {
        checkInvariants();
        list.add(message);
    }

    @Override
    public int getDepth() {
        return list.size();
    }

    @Override
    public List<String> asList() {
        return list;
    }

    @Override
    public void trim(final int depth) {
        checkInvariants();
        if (depth < 0) {
            throw new IllegalArgumentException("Maximum stack depth cannot be negative");
        }
        if (list == null) {
            return;
        }
        final List<String> copy = new ArrayList<>(list.size());
        final int count = Math.min(depth, list.size());
        for (int i = 0; i < count; i++) {
            copy.add(list.get(i));
        }
        list.clear();
        list.addAll(copy);
    }

    @Override
    public ThreadContextStack copy() {
        return new MutableThreadContextStack(this);
    }

    @Override
    public void clear() {
        checkInvariants();
        list.clear();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] ts) {
        return list.toArray(ts);
    }

    @Override
    public boolean add(final String s) {
        checkInvariants();
        return list.add(s);
    }

    @Override
    public boolean remove(final Object o) {
        checkInvariants();
        return list.remove(o);
    }

    @Override
    public boolean containsAll(final Collection<?> objects) {
        return list.containsAll(objects);
    }

    @Override
    public boolean addAll(final Collection<? extends String> strings) {
        checkInvariants();
        return list.addAll(strings);
    }

    @Override
    public boolean removeAll(final Collection<?> objects) {
        checkInvariants();
        return list.removeAll(objects);
    }

    @Override
    public boolean retainAll(final Collection<?> objects) {
        checkInvariants();
        return list.retainAll(objects);
    }

    @Override
    public String toString() {
        return String.valueOf(list);
    }

    @Override
    public void formatTo(final StringBuilder buffer) {
        buffer.append('[');
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                buffer.append(',').append(' ');
            }
            buffer.append(list.get(i));
        }
        buffer.append(']');
    }

    @Override
    public int hashCode() {
        return 31 + Objects.hashCode(list);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ThreadContextStack)) {
            return false;
        }
        final ThreadContextStack other = (ThreadContextStack) obj;
        final List<String> otherAsList = other.asList();
        return Objects.equals(this.list, otherAsList);
    }

    @Override
    public ContextStack getImmutableStackOrNull() {
        return copy();
    }

    /**
     * "Freezes" this context stack so it becomes immutable: all mutator methods will throw an exception from now on.
     */
    public void freeze() {
        frozen = true;
    }

    /**
     * Returns whether this context stack is frozen.
     * @return whether this context stack is frozen.
     */
    public boolean isFrozen() {
        return frozen;
    }
}
