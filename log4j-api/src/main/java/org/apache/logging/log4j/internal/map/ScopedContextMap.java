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
package org.apache.logging.log4j.internal.map;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents an immutable map, which stores its state inside a single Object[]:
 * Multiple maps may share the same object array to avoid copying.
 */
public class ScopedContextMap extends AbstractMap<String, Object> implements Serializable {
    /**
     * Implementation of Map.Entry. The implementation is simple since each instance
     * contains an index in the array, then getKey() and getValue() retrieve from
     * the array. Blocks modifications.
     */
    private class MapEntry implements Entry<String, Object> {
        /**
         * This field is functionally final, but marking it as such can cause
         * performance problems. Consider marking it final after
         * https://bugs.openjdk.org/browse/JDK-8324186 is solved.
         */
        private int index;

        private String key;
        private Object value;

        public MapEntry(int index) {
            this.index = index;
        }

        @Override
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public Object setValue(Object value) {
            Object old = this.value;
            this.value = value;
            return old;
        }

        public void clear() {
            this.key = null;
            this.value = null;
        }

        /**
         * Per spec, the hashcode is a function of the key and value. Calculation
         * exactly matches HashMap.
         */
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
    }

    /**
     * Simple Entry iterator, tracking solely the index in the array. Blocks
     * modifications.
     */
    private class UnmodifiableEntryIterator implements Iterator<Entry<String, Object>> {
        private int index;
        private Iterator<Integer> skip = skipList.iterator();
        private Integer skipItem = skip.next();

        @Override
        public boolean hasNext() {
            return index < numEntries;
        }

        @Override
        public Entry<String, Object> next() {
            while (skipItem != null && index < numEntries && skipItem == index) {
                index++;
                skipItem = skip.next();
            }
            return index < numEntries ? backingArray[index] : null;
        }
    }

    /**
     * Simple Entry set, providing a reference to UnmodifiableEntryIterator and
     * blocking modifications.
     */
    private class UnmodifiableEntrySet extends AbstractSet<Entry<String, Object>> {

        @Override
        public boolean add(Entry<String, Object> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Entry<String, Object>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Entry<String, Object>> iterator() {
            return new UnmodifiableEntryIterator();
        }

        @Override
        public int size() {
            return numEntries - skipList.size();
        }
    }

    public static ScopedContextMap getInstance(String key, Object value) {
        ScopedContextMap newMap = new ScopedContextMap(1, new ArrayList<>());
        newMap.add(key, value);
        return newMap;
    }

    public static ScopedContextMap emptyMap() {
        return new ScopedContextMap(0, new ArrayList<>());
    }

    /**
     * Creates a new instance that contains the entries in the Map.
     *
     * @param entriesToAdd the Map.
     * @return The new Map.
     */
    public static ScopedContextMap getInstance(Map<String, Object> entriesToAdd) {
        // create a new array that can hold the maximum output size
        ScopedContextMap newMap = new ScopedContextMap(entriesToAdd.size(), new ArrayList<>());

        for (Entry<String, Object> entry : entriesToAdd.entrySet()) {
            newMap.add(entry.getKey(), entry.getValue());
        }
        return newMap;
    }

    /**
     * backingArray is functionally final, but marking it as such can cause
     * performance problems. Consider marking it final after
     * https://bugs.openjdk.org/browse/JDK-8324186 is solved.
     */
    private MapEntry[] backingArray;

    private List<Integer> skipList;

    private int numEntries;

    private ScopedContextMap(int capacity, List<Integer> skipList) {
        this.backingArray = new MapEntry[capacity];
        for (int i = 0; i < backingArray.length; ++i) {
            this.backingArray[i] = new MapEntry(i);
        }
        this.skipList = new ArrayList<>(skipList);
    }

    private ScopedContextMap(ScopedContextMap other, int entriesToAdd) {
        this(other.backingArray.length + entriesToAdd, other.skipList);
        System.arraycopy(other.backingArray, 0, this.backingArray, 0, other.backingArray.length);
        numEntries = other.numEntries;
    }

    private void add(String key, Object value) {
        Integer itemIndex = itemIndex(key);
        if (itemIndex != null) {
            skipList.add(itemIndex);
        }
        backingArray[numEntries].key = key;
        backingArray[numEntries].value = value;
        numEntries++;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Instance cannot be cleared, reuse EMPTY_MAP instead.");
    }

    /**
     * Scans the array to find a matching key. Linear performance.
     */
    @Override
    public boolean containsKey(Object key) {
        return containsKey((String) key);
    }

    public boolean containsKey(String key) {
        return itemIndex(key) != null;
    }

    private Integer itemIndex(String key) {
        int hashCode = key.hashCode();
        Iterator<Integer> skip = skipList.iterator();
        Integer skipItem = nextSkipItem(skip);
        for (int i = 0; i < numEntries; i++) {
            if (skipItem != null && skipItem == i) {
                skipItem = nextSkipItem(skip);
                continue;
            }
            if (backingArray[i].key.equals(key)) {
                return i;
            }
        }
        return null;
    }

    private Integer nextSkipItem(Iterator<Integer> iter) {
        return iter.hasNext() ? iter.next() : null;
    }

    /**
     * Scans the array to find a matching value, with linear time. Allows null
     * parameter.
     */
    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }
        for (int i = 0; i < numEntries; i++) {
            if (value.equals(backingArray[i].value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new instance that contains the same entries as this map, plus
     * either the new entry or updated value passed in the parameters.
     *
     * @param key The key.
     * @param value The value.
     * @return A new Map.
     */
    public ScopedContextMap copyAndPut(String key, Object value) {
        ScopedContextMap newMap = new ScopedContextMap(this, numEntries + 1);
        newMap.add(key, value);
        return newMap;
    }

    /**
     * Creates a new instance that contains the same entries as this map, plus the
     * new entries or updated values passed in the parameters.
     *
     * @param entriesToAdd the Map to add to this one.
     * @return The new Map.
     */
    public ScopedContextMap copyAndPutAll(Map<String, Object> entriesToAdd) {
        ScopedContextMap newMap = new ScopedContextMap(this, numEntries + entriesToAdd.size());
        for (Entry<String, Object> entry : entriesToAdd.entrySet()) {
            newMap.add(entry.getKey(), entry.getValue());
        }
        return newMap;
    }

    /**
     * This version of forEach is defined on the Map interface.
     */
    @Override
    public void forEach(java.util.function.BiConsumer<? super String, ? super Object> action) {
        Iterator<Integer> skip = skipList.iterator();
        Integer skipItem = nextSkipItem(skip);
        for (int i = 0; i < numEntries; i++) {
            if (skipItem != null && skipItem == i) {
                skipItem = nextSkipItem(skip);
                continue;
            }
            action.accept(backingArray[i].key, backingArray[i].value);
        }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new UnmodifiableEntrySet();
    }

    /**
     * Scans the array to find a matching key. Linear-time.
     * Looks in reverse order because newer values for a key are appended to the end.
     */
    @Override
    public Object get(Object key) {
        if (numEntries == 0) {
            return null;
        }
        int hashCode = key.hashCode();
        for (int i = numEntries - 1; i >= 0; i--) {
            if (backingArray[i].key.equals(key)) {
                return backingArray[i].value;
            }
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        throw new UnsupportedOperationException("putAll() is not supported, use copyAndPutAll instead");
    }

    @Override
    public String remove(Object key) {
        throw new UnsupportedOperationException("remove() is not supported, use copyAndRemove instead");
    }

    @Override
    public int size() {
        return numEntries - skipList.size();
    }
}
