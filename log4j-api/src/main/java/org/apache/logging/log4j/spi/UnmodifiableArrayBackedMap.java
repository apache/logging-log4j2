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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class represents an immutable map, which stores its key-value pairs
 * inside a single array of type String[]. Keys are therefore accessed by
 * accessing the (2 * index) element in the list, and values by (2 * index + 1).
 *
 * Performance:
 * <ul>
 * <li>Implements very low-cost copies: shallow-copy the array.</li>
 * <li>Doesn't matter for mutable operations, since we don't allow them.</li>
 * <li>Iterates very quickly, since it iterates directly across the array. This
 * contrasts with HashMap's requirement to scan each bucket in the table and
 * chase each pointer.</li>
 * <li>Is linear on gets, puts, and removes, since the table must be scanned to
 * find a matching key.</li>
 * </ul>
 *
 * Allocation:
 * <ul>
 * <li>Zero on reads.</li>
 * <li>Copy-and-modify operations allocate exactly two objects: the new array
 * and the new Map instance. This is substantially better than HashMap, which
 * requires a new Node for each entry.</li>
 * </ul>
 *
 */
class UnmodifiableArrayBackedMap extends AbstractMap<String, String> implements Serializable {
    /**
     * Implementation of Map.Entry. The implementation is simple since each instance
     * contains an index in the array, then getKey() and getValue() retrieve from
     * the array.  Blocks modifications.
     */
    private class UnmodifiableEntry implements Map.Entry<String, String> {
        private final int index;

        public UnmodifiableEntry(int index) {
            this.index = index;
        }

        @Override
        public String getKey() {
            return keysAndValues[index * 2];
        }

        @Override
        public String getValue() {
            return keysAndValues[index * 2 + 1];
        }

        /**
         * Per spec, the hashcode is a function of the key and value. Calculation
         * exactly matches HashMap.
         */
        public int hashCode() {
            String key = keysAndValues[index * 2];
            String value = keysAndValues[index * 2 + 1];
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public String setValue(String value) {
            throw new UnsupportedOperationException("Cannot update Entry instances in UnmodifiableArrayBackedMap");
        }
    }

    /**
     * Simple Entry iterator, tracking solely the index in the array.  Blocks modifications.
     */
    private class UnmodifiableEntryIterator implements Iterator<Map.Entry<String, String>> {
        private int index;

        @Override
        public boolean hasNext() {
            return index < numEntries;
        }

        @Override
        public Entry<String, String> next() {
            return new UnmodifiableEntry(index++);
        }
    }

    /**
     * Simple Entry set, providing a reference to UnmodifiableEntryIterator and
     * blocking modifications.
     */
    private class UnmodifiableEntrySet extends AbstractSet<Map.Entry<String, String>> {

        @Override
        public boolean add(Entry<String, String> e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends Entry<String, String>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Entry<String, String>> iterator() {
            return new UnmodifiableEntryIterator();
        }

        @Override
        public int size() {
            return numEntries;
        }
    }

    private static final long serialVersionUID = 6849423432534211514L;

    public static final UnmodifiableArrayBackedMap EMPTY_MAP = new UnmodifiableArrayBackedMap();

    private final String[] keysAndValues;

    private int numEntries;

    private UnmodifiableArrayBackedMap() {
        this(0);
    }

    private UnmodifiableArrayBackedMap(int capacity) {
        this.keysAndValues = new String[capacity * 2];
    }

    UnmodifiableArrayBackedMap(UnmodifiableArrayBackedMap other) {
        this.keysAndValues = other.keysAndValues;
        this.numEntries = other.numEntries;
    }

    private void add(String key, String value) {
        keysAndValues[numEntries * 2] = key;
        keysAndValues[numEntries * 2 + 1] = value;
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
        int hashCode = key.hashCode();
        for (int i = 0; i < numEntries; i++) {
            if (keysAndValues[i * 2].hashCode() == hashCode && keysAndValues[i * 2].equals(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Scans the array to find a matching value, with linear time. Allows null
     * parameter.
     */
    @Override
    public boolean containsValue(Object value) {
        for (int i = 0; i < numEntries; i++) {
            String valueInMap = keysAndValues[i * 2 + 1];
            if (value == null) {
                if (valueInMap == null) {
                    return true;
                }
            } else if (value.equals(valueInMap)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new instance that contains the same entries as this map, plus
     * either the new entry or updated value passed in the parameters.
     *
     * @param key
     * @param value
     * @return
     */
    UnmodifiableArrayBackedMap copyAndPut(String key, String value) {
        UnmodifiableArrayBackedMap newMap = new UnmodifiableArrayBackedMap(numEntries + 1);
        System.arraycopy(this.keysAndValues, 0, newMap.keysAndValues, 0, numEntries * 2);
        newMap.numEntries = numEntries;
        newMap.addOrOverwriteKey(key, value);

        return newMap;
    }

    /**
     * Creates a new instance that contains the same entries as this map, plus the
     * new entries or updated values passed in the parameters.
     *
     * @param key
     * @param value
     * @return
     */
    UnmodifiableArrayBackedMap copyAndPutAll(Map<String, String> entriesToAdd) {
        // create a new array that can hold the maximum output size
        UnmodifiableArrayBackedMap newMap = new UnmodifiableArrayBackedMap(numEntries + entriesToAdd.size());

        // copy the contents of the current map (if any)
        System.arraycopy(keysAndValues, 0, newMap.keysAndValues, 0, numEntries * 2);
        newMap.numEntries = numEntries;

        for (Map.Entry<String, String> entry : entriesToAdd.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!this.isEmpty()) {
                // The unique elements passed in may overlap the unique elements here - must
                // check
                newMap.addOrOverwriteKey(key, value);
            } else {
                // There is no chance of overlapping keys, we can simply add
                newMap.add(key, value);
            }
        }

        return newMap;
    }

    /**
     * Creates a new instance that contains the same entries as this map, minus the
     * entry with the specified key (if such an entry exists).
     *
     * @param key
     * @param value
     * @return
     */
    UnmodifiableArrayBackedMap copyAndRemove(String key) {
        boolean foundKeyToRemove = false;

        UnmodifiableArrayBackedMap newMap = new UnmodifiableArrayBackedMap(numEntries);
        for (int oldIndex = 0; oldIndex < numEntries; oldIndex++) {
            if (!foundKeyToRemove
                    && keysAndValues[oldIndex * 2].hashCode() == key.hashCode()
                    && keysAndValues[oldIndex * 2].equals(key)) {
                foundKeyToRemove = true;
                continue;
            } else {
                String keyToKeep = keysAndValues[oldIndex * 2];
                String value = keysAndValues[oldIndex * 2 + 1];
                newMap.add(keyToKeep, value);
            }
        }

        if (!foundKeyToRemove) {
            return this;
        }
        return newMap;
    }

    /**
     * Creates a new instance that contains the same entries as this map, minus all
     * of the keys passed in the arguments.
     *
     * @param key
     * @param value
     * @return
     */
    UnmodifiableArrayBackedMap copyAndRemoveAll(Iterable<String> keysToRemoveIterable) {
        // we invest time building a HashSet of the keys to remove, allowing for fast
        // lookups below
        Set<String> keysToRemoveSet = new HashSet<>();
        for (String key : keysToRemoveIterable) {
            keysToRemoveSet.add(key);
        }

        // build the new map
        UnmodifiableArrayBackedMap newMap = new UnmodifiableArrayBackedMap(numEntries);
        for (int indexInCurrentMap = 0; indexInCurrentMap < numEntries; indexInCurrentMap++) {
            // for each key in this map, check whether it's in the set we built above
            String key = keysAndValues[indexInCurrentMap * 2];
            if (!keysToRemoveSet.contains(key)) {
                // this key should be removed - or in this case, not copied.
                String value = keysAndValues[indexInCurrentMap * 2 + 1];
                newMap.add(key, value);
            }
        }

        return newMap;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new UnmodifiableEntrySet();
    }

    /**
     * Scans the array to find a matching key. Linear-time.
     */
    @Override
    public String get(Object key) {
        int hashCode = key.hashCode();
        for (int i = 0; i < numEntries; i++) {
            if (keysAndValues[i * 2].hashCode() == hashCode && keysAndValues[i * 2].equals(key)) {
                return keysAndValues[i * 2 + 1];
            }
        }
        return null;
    }

    /**
     * Find an existing entry (if any) and overwrites the value, if found
     *
     * @param key
     * @param value
     * @return
     */
    private void addOrOverwriteKey(String key, String value) {
        int keyHashCode = key.hashCode();
        for (int i = 0; i < numEntries; i++) {
            if (keysAndValues[i * 2].hashCode() == keyHashCode && keysAndValues[i * 2].equals(key)) {
                // found a match, overwrite then return
                keysAndValues[i * 2 + 1] = value;
                return;
            }
        }

        // no match found, add to the end
        add(key, value);
    }

    @Override
    public String put(String key, String value) {
        throw new UnsupportedOperationException("put() is not supported, use copyAndPut instead");
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException("putAll() is not supported, use copyAndPutAll instead");
    }

    @Override
    public String remove(Object key) {
        throw new UnsupportedOperationException("remove() is not supported, use copyAndRemove instead");
    }

    @Override
    public int size() {
        return numEntries;
    }
}
