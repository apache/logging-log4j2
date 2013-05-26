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

    public MutableThreadContextStack(List<String> list) {
        this.list = new ArrayList<String>(list);
    }

    private MutableThreadContextStack(MutableThreadContextStack stack) {
        this.list = new ArrayList<String>(stack.list);
    }

    public String pop() {
        if (list.isEmpty()) {
            return null;
        }
        final int last = list.size() - 1;
        final String result = list.remove(last);
        return result;
    }

    public String peek() {
        if (list.isEmpty()) {
            return null;
        }
        final int last = list.size() - 1;
        return list.get(last);
    }

    public void push(final String message) {
        list.add(message);
    }

    public int getDepth() {
        return list.size();
    }

    public List<String> asList() {
        return list;
    }

    public void trim(final int depth) {
        if (depth < 0) {
            throw new IllegalArgumentException("Maximum stack depth cannot be negative");
        }
        if (list == null) {
            return;
        }
        final List<String> copy = new ArrayList<String>(list.size());
        int count = Math.min(depth, list.size());
        for(int i = 0; i < count; i++) {
            copy.add(list.get(i));
        }
        list.clear();
        list.addAll(copy);
    }

    public ThreadContextStack copy() {
        return new MutableThreadContextStack(this);
    }

    public void clear() {
        list.clear();
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean contains(Object o) {
        return list.contains(o);
    }

    public Iterator<String> iterator() {
        return list.iterator();
    }

    public Object[] toArray() {
        return list.toArray();
    }

    public <T> T[] toArray(T[] ts) {
        return list.toArray(ts);
    }

    public boolean add(String s) {
        return list.add(s);
    }

    public boolean remove(Object o) {
        return list.remove(o);
    }

    public boolean containsAll(Collection<?> objects) {
        return list.containsAll(objects);
    }

    public boolean addAll(Collection<? extends String> strings) {
        return list.addAll(strings);
    }

    public boolean removeAll(Collection<?> objects) {
        return list.removeAll(objects);
    }

    public boolean retainAll(Collection<?> objects) {
        return list.retainAll(objects);
    }
}
