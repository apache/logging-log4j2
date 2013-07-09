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
package org.apache.logging.log4j.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A copy-on-write thread-safe variant of
 * {@code org.apache.logging.log4j.spi.ThreadContextStack} in which all mutative
 * operations (add, pop, and so on) are implemented by making a fresh copy of
 * the underlying list.
 */
public class DefaultThreadContextStack implements ThreadContextStack {

    private static final long serialVersionUID = 5050501L;

    private static ThreadLocal<List<String>> stack = new ThreadLocal<List<String>>();

    private final boolean useStack;

    public DefaultThreadContextStack(final boolean useStack) {
        this.useStack = useStack;
    }

    @Override
    public String pop() {
        if (!useStack) {
            return "";
        }
        final List<String> list = stack.get();
        if (list == null || list.size() == 0) {
            throw new NoSuchElementException("The ThreadContext stack is empty");
        }
        final List<String> copy = new ArrayList<String>(list);
        final int last = copy.size() - 1;
        final String result = copy.remove(last);
        stack.set(Collections.unmodifiableList(copy));
        return result;
    }

    @Override
    public String peek() {
        final List<String> list = stack.get();
        if (list == null || list.size() == 0) {
            return null;
        }
        final int last = list.size() - 1;
        return list.get(last);
    }

    @Override
    public void push(final String message) {
        if (!useStack) {
            return;
        }
        add(message);
    }

    @Override
    public int getDepth() {
        final List<String> list = stack.get();
        return list == null ? 0 : list.size();
    }

    @Override
    public List<String> asList() {
        final List<String> list = stack.get();
        if (list == null) {
            return Collections.emptyList();
        }
        return list;
    }

    @Override
    public void trim(final int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException(
                    "Maximum stack depth cannot be negative");
        }
        final List<String> list = stack.get();
        if (list == null) {
            return;
        }
        final List<String> copy = new ArrayList<String>();
        final int count = Math.min(depth, list.size());
        for (int i = 0; i < count; i++) {
            copy.add(list.get(i));
        }
        stack.set(copy);
    }

    @Override
    public ThreadContextStack copy() {
        List<String> result = null;
        if (!useStack || (result = stack.get()) == null) {
            return new MutableThreadContextStack(new ArrayList<String>());
        }
        return new MutableThreadContextStack(result);
    }

    @Override
    public void clear() {
        stack.remove();
    }

    @Override
    public int size() {
        final List<String> result = stack.get();
        return result == null ? 0 : result.size();
    }

    @Override
    public boolean isEmpty() {
        final List<String> result = stack.get();
        return result == null || result.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        final List<String> result = stack.get();
        return result != null && result.contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        final List<String> immutable = stack.get();
        if (immutable == null) {
            final List<String> empty = Collections.emptyList();
            return empty.iterator();
        }
        return immutable.iterator();
    }

    @Override
    public Object[] toArray() {
        final List<String> result = stack.get();
        if (result == null) {
            return new String[0];
        }
        return result.toArray(new Object[result.size()]);
    }

    @Override
    public <T> T[] toArray(final T[] ts) {
        final List<String> result = stack.get();
        if (result == null) {
            if (ts.length > 0) { // as per the contract of j.u.List#toArray(T[])
                ts[0] = null;
            }
            return ts;
        }
        return result.toArray(ts);
    }

    @Override
    public boolean add(final String s) {
        if (!useStack) {
            return false;
        }
        final List<String> list = stack.get();
        final List<String> copy = list == null ? new ArrayList<String>()
                : new ArrayList<String>(list);
        copy.add(s);
        stack.set(Collections.unmodifiableList(copy));
        return true;
    }

    @Override
    public boolean remove(final Object o) {
        if (!useStack) {
            return false;
        }
        final List<String> list = stack.get();
        if (list == null || list.size() == 0) {
            return false;
        }
        final List<String> copy = new ArrayList<String>(list);
        final boolean result = copy.remove(o);
        stack.set(Collections.unmodifiableList(copy));
        return result;
    }

    @Override
    public boolean containsAll(final Collection<?> objects) {
        if (objects.isEmpty()) { // quick check before accessing the ThreadLocal
            return true; // looks counter-intuitive, but see
                         // j.u.AbstractCollection
        }
        final List<String> list = stack.get();
        return list != null && list.containsAll(objects);
    }

    @Override
    public boolean addAll(final Collection<? extends String> strings) {
        if (!useStack || strings.isEmpty()) {
            return false;
        }
        final List<String> list = stack.get();
        final List<String> copy = list == null ? new ArrayList<String>()
                : new ArrayList<String>(list);
        copy.addAll(strings);
        stack.set(Collections.unmodifiableList(copy));
        return true;
    }

    @Override
    public boolean removeAll(final Collection<?> objects) {
        if (!useStack || objects.isEmpty()) {
            return false;
        }
        final List<String> list = stack.get();
        if (list == null || list.isEmpty()) {
            return false;
        }
        final List<String> copy = new ArrayList<String>(list);
        final boolean result = copy.removeAll(objects);
        stack.set(Collections.unmodifiableList(copy));
        return result;
    }

    @Override
    public boolean retainAll(final Collection<?> objects) {
        if (!useStack || objects.isEmpty()) {
            return false;
        }
        final List<String> list = stack.get();
        if (list == null || list.isEmpty()) {
            return false;
        }
        final List<String> copy = new ArrayList<String>(list);
        final boolean result = copy.retainAll(objects);
        stack.set(Collections.unmodifiableList(copy));
        return result;
    }
}
