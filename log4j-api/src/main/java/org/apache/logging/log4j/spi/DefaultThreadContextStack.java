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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.logging.log4j.ThreadContext.ContextStack;
import org.apache.logging.log4j.util.Strings;

/**
 * A copy-on-write thread-safe variant of {@code org.apache.logging.log4j.spi.ThreadContextStack} in which all mutative operations (add,
 * pop, and so on) are implemented by making a fresh copy of the underlying list.
 */
public class DefaultThreadContextStack implements ThreadContextStack {

    private static final long serialVersionUID = 5050501L;

    private static final ThreadLocal<MutableThreadContextStack> stack = new ThreadLocal<MutableThreadContextStack>();

    private final boolean useStack;

    public DefaultThreadContextStack(final boolean useStack) {
        this.useStack = useStack;
    }
    
    private MutableThreadContextStack getNonNullStackCopy() {
        final MutableThreadContextStack values = stack.get();
        return (MutableThreadContextStack) (values == null ? new MutableThreadContextStack() : values.copy());
    }

    @Override
    public boolean add(final String s) {
        if (!useStack) {
            return false;
        }
        final MutableThreadContextStack copy = getNonNullStackCopy();
        copy.add(s);
        copy.freeze();
        stack.set(copy);
        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends String> strings) {
        if (!useStack || strings.isEmpty()) {
            return false;
        }
        final MutableThreadContextStack copy = getNonNullStackCopy();
        copy.addAll(strings);
        copy.freeze();
        stack.set(copy);
        return true;
    }

    @Override
    public List<String> asList() {
        final MutableThreadContextStack values = stack.get();
        if (values == null) {
            return Collections.emptyList();
        }
        return values.asList();
    }

    @Override
    public void clear() {
        stack.remove();
    }

    @Override
    public boolean contains(final Object o) {
        final MutableThreadContextStack values = stack.get();
        return values != null && values.contains(o);
    }

    @Override
    public boolean containsAll(final Collection<?> objects) {
        if (objects.isEmpty()) { // quick check before accessing the ThreadLocal
            return true; // looks counter-intuitive, but see
                         // j.u.AbstractCollection
        }
        final MutableThreadContextStack values = stack.get();
        return values != null && values.containsAll(objects);
    }

    @Override
    public ThreadContextStack copy() {
        MutableThreadContextStack values = null;
        if (!useStack || (values = stack.get()) == null) {
            return new MutableThreadContextStack();
        }
        return values.copy();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof DefaultThreadContextStack) {
            final DefaultThreadContextStack other = (DefaultThreadContextStack) obj;
            if (this.useStack != other.useStack) {
                return false;
            }
        }
        if (!(obj instanceof ThreadContextStack)) {
            return false;
        }
        final ThreadContextStack other = (ThreadContextStack) obj;
        final MutableThreadContextStack values = stack.get();
        if (values == null) {
            return other == null;
        }
        return values.equals(other);
    }

    @Override
    public int getDepth() {
        final MutableThreadContextStack values = stack.get();
        return values == null ? 0 : values.getDepth();
    }

    @Override
    public int hashCode() {
        final MutableThreadContextStack values = stack.get();
        final int prime = 31;
        int result = 1;
        // Factor in the stack itself to compare vs. other implementors.
        result = prime * result + ((values == null) ? 0 : values.hashCode());
        return result;
    }

    @Override
    public boolean isEmpty() {
        final MutableThreadContextStack values = stack.get();
        return values == null || values.isEmpty();
    }

    @Override
    public Iterator<String> iterator() {
        final MutableThreadContextStack values = stack.get();
        if (values == null) {
            final List<String> empty = Collections.emptyList();
            return empty.iterator();
        }
        return values.iterator();
    }

    @Override
    public String peek() {
        final MutableThreadContextStack values = stack.get();
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.peek();
    }

    @Override
    public String pop() {
        if (!useStack) {
            return Strings.EMPTY;
        }
        final MutableThreadContextStack values = stack.get();
        if (values == null || values.size() == 0) {
            throw new NoSuchElementException("The ThreadContext stack is empty");
        }
        final MutableThreadContextStack copy = (MutableThreadContextStack) values.copy();
        final String result = copy.pop();
        copy.freeze();
        stack.set(copy);
        return result;
    }

    @Override
    public void push(final String message) {
        if (!useStack) {
            return;
        }
        add(message);
    }

    @Override
    public boolean remove(final Object o) {
        if (!useStack) {
            return false;
        }
        final MutableThreadContextStack values = stack.get();
        if (values == null || values.size() == 0) {
            return false;
        }
        final MutableThreadContextStack copy = (MutableThreadContextStack) values.copy();
        final boolean result = copy.remove(o);
        copy.freeze();
        stack.set(copy);
        return result;
    }

    @Override
    public boolean removeAll(final Collection<?> objects) {
        if (!useStack || objects.isEmpty()) {
            return false;
        }
        final MutableThreadContextStack values = stack.get();
        if (values == null || values.isEmpty()) {
            return false;
        }
        final MutableThreadContextStack copy = (MutableThreadContextStack) values.copy();
        final boolean result = copy.removeAll(objects);
        copy.freeze();
        stack.set(copy);
        return result;
    }

    @Override
    public boolean retainAll(final Collection<?> objects) {
        if (!useStack || objects.isEmpty()) {
            return false;
        }
        final MutableThreadContextStack values = stack.get();
        if (values == null || values.isEmpty()) {
            return false;
        }
        final MutableThreadContextStack copy = (MutableThreadContextStack) values.copy();
        final boolean result = copy.retainAll(objects);
        copy.freeze();
        stack.set(copy);
        return result;
    }

    @Override
    public int size() {
        final MutableThreadContextStack values = stack.get();
        return values == null ? 0 : values.size();
    }

    @Override
    public Object[] toArray() {
        final MutableThreadContextStack result = stack.get();
        if (result == null) {
            return new String[0];
        }
        return result.toArray(new Object[result.size()]);
    }

    @Override
    public <T> T[] toArray(final T[] ts) {
        final MutableThreadContextStack result = stack.get();
        if (result == null) {
            if (ts.length > 0) { // as per the contract of j.u.List#toArray(T[])
                ts[0] = null;
            }
            return ts;
        }
        return result.toArray(ts);
    }

    @Override
    public String toString() {
        final MutableThreadContextStack values = stack.get();
        return values == null ? "[]" : values.toString();
    }

    @Override
    public void trim(final int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("Maximum stack depth cannot be negative");
        }
        final MutableThreadContextStack values = stack.get();
        if (values == null) {
            return;
        }
        final MutableThreadContextStack copy = (MutableThreadContextStack) values.copy();
        copy.trim(depth);
        copy.freeze();
        stack.set(copy);
    }

    /* (non-Javadoc)
     * @see org.apache.logging.log4j.ThreadContext.ContextStack#getImmutableStackOrNull()
     */
    @Override
    public ContextStack getImmutableStackOrNull() {
        return stack.get();
    }
}
