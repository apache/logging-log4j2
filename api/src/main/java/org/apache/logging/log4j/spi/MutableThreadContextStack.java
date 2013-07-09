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
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class MutableThreadContextStack implements ThreadContextStack {

    private static final long serialVersionUID = 50505011L;

    /**
     * The underlying list (never null).
     */
    private final List<String> list;

    public MutableThreadContextStack(final List<String> list) {
        this.list = new ArrayList<String>(list);
    }

    private MutableThreadContextStack(final MutableThreadContextStack stack) {
        this.list = new ArrayList<String>(stack.list);
    }

    @Override
    public String pop() {
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
        if (depth < 0) {
            throw new IllegalArgumentException("Maximum stack depth cannot be negative");
        }
        if (list == null) {
            return;
        }
        final List<String> copy = new ArrayList<String>(list.size());
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
        return list.add(s);
    }

    @Override
    public boolean remove(final Object o) {
        return list.remove(o);
    }

    @Override
    public boolean containsAll(final Collection<?> objects) {
        return list.containsAll(objects);
    }

    @Override
    public boolean addAll(final Collection<? extends String> strings) {
        return list.addAll(strings);
    }

    @Override
    public boolean removeAll(final Collection<?> objects) {
        return list.removeAll(objects);
    }

    @Override
    public boolean retainAll(final Collection<?> objects) {
        return list.retainAll(objects);
    }
}
