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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * This class represents an immutable map, which stores its state inside a single Object[]:
 * <ol>
 * <li>[0] contains the number of entries</li>
 * <li>Others contain alternating key-value pairs, for example [1]="1" and [2]="value_for_1"</li>
 * </ol>
 *
 * Keys are calculated using (index * 2 + 1) and values are (index * 2 + 2).
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
public class UnmodifiableArrayBackedMap extends AbstractMap<String, String> implements Serializable, ReadOnlyStringMap {
    /**
     * Implementation of Map.Entry. The implementation is simple since each instance
     * contains an index in the array, then getKey() and getValue() retrieve from
     * the array. Blocks modifications.
     */
    private class UnmodifiableEntry implements Map.Entry<String, String> {
        /**
         * This field is functionally final, but marking it as such can cause
         * performance problems. Consider marking it final after
         * https://bugs.openjdk.org/browse/JDK-8324186 is solved.
         */
        private int index;

        public UnmodifiableEntry(int index) {
            this.index = index;
        }

        @Override
        public String getKey() {
            return (String) backingArray[getArrayIndexForKey(index)];
        }

        @Override
        public String getValue() {
            return (String) backingArray[getArrayIndexForValue(index)];
        }

        /**
         * Per spec, the hashcode is a function of the key and value. Calculation
         * exactly matches HashMap.
         */
        public int hashCode() {
            String key = (String) backingArray[getArrayIndexForKey(index)];
            String value = (String) backingArray[getArrayIndexForValue(index)];
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public String setValue(String value) {
            throw new UnsupportedOperationException("Cannot update Entry instances in UnmodifiableArrayBackedMap");
        }
    }

    /**
     * Simple Entry iterator, tracking solely the index in the array. Blocks
     * modifications.
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

    public static final UnmodifiableArrayBackedMap EMPTY_MAP = new UnmodifiableArrayBackedMap(0);

    private static final int NUM_FIXED_ARRAY_ENTRIES = 1;

    private static int getArrayIndexForKey(int entryIndex) {
        return 2 * entryIndex + NUM_FIXED_ARRAY_ENTRIES;
    }

    private static int getArrayIndexForValue(int entryIndex) {
        return 2 * entryIndex + 1 + NUM_FIXED_ARRAY_ENTRIES;
    }

    public static UnmodifiableArrayBackedMap getMap(Object[] backingArray) {
        if (backingArray == null || backingArray.length == 1) {
            return EMPTY_MAP;
        } else {
            return new UnmodifiableArrayBackedMap(backingArray);
        }
    }

    /**
     * backingArray is functionally final, but marking it as such can cause
     * performance problems. Consider marking it final after
     * https://bugs.openjdk.org/browse/JDK-8324186 is solved.
     */
    private Object[] backingArray;

    private int numEntries;

    private UnmodifiableArrayBackedMap(int capacity) {
        this.backingArray = new Object[capacity * 2 + 1];
        this.backingArray[0] = 0;
    }

    private UnmodifiableArrayBackedMap(Object[] backingArray) {
        this.numEntries = (backingArray == null ? 0 : (int) backingArray[0]);
        this.backingArray = backingArray;
    }

    UnmodifiableArrayBackedMap(UnmodifiableArrayBackedMap other) {
        this.backingArray = other.backingArray;
        this.numEntries = other.numEntries;
    }

    private void add(String key, String value) {
        backingArray[getArrayIndexForKey(numEntries)] = key;
        backingArray[getArrayIndexForValue(numEntries)] = value;
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

    @Override
    public boolean containsKey(String key) {
        int hashCode = key.hashCode();
        for (int i = 0; i < numEntries; i++) {
            if (backingArray[getArrayIndexForKey(i)].hashCode() == hashCode
                    && backingArray[getArrayIndexForKey(i)].equals(key)) {
                return true;
            }
        }

        return false;
    }

    public Object[] getBackingArray() {
        return backingArray;
    }

    /**
     * Scans the array to find a matching value, with linear time. Allows null
     * parameter.
     */
    @Override
    public boolean containsValue(Object value) {
        for (int i = 0; i < numEntries; i++) {
            Object valueInMap = backingArray[getArrayIndexForValue(i)];
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
    public UnmodifiableArrayBackedMap copyAndPut(String key, String value) {
        UnmodifiableArrayBackedMap newMap = new UnmodifiableArrayBackedMap(numEntries + 1);
        // include the numEntries value (array index 0)
        if (this.numEntries > 0) {
            System.arraycopy(this.backingArray, 1, newMap.backingArray, 1, numEntries * 2);
            newMap.numEntries = numEntries;
        }
        newMap.addOrOverwriteKey(key, value);
        newMap.updateNumEntriesInArray();
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
    public UnmodifiableArrayBackedMap copyAndPutAll(Map<String, String> entriesToAdd) {
        // create a new array that can hold the maximum output size
        UnmodifiableArrayBackedMap newMap = new UnmodifiableArrayBackedMap(numEntries + entriesToAdd.size());

        // copy the contents of the current map (if any)
        if (numEntries > 0) {
            System.arraycopy(backingArray, 0, newMap.backingArray, 0, numEntries * 2 + 1);
            newMap.numEntries = numEntries;
        }

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

        newMap.updateNumEntriesInArray();
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
    public UnmodifiableArrayBackedMap copyAndRemove(String key) {
        int indexToRemove = -1;
        for (int oldIndex = numEntries - 1; oldIndex >= 0; oldIndex--) {
            if (backingArray[getArrayIndexForKey(oldIndex)].hashCode() == key.hashCode()
                    && backingArray[getArrayIndexForKey(oldIndex)].equals(key)) {
                indexToRemove = oldIndex;
                break;
            }
        }

        if (indexToRemove == -1) {
            // key not found, no change necessary
            return this;
        } else if (numEntries == 1) {
            // we have 1 item and we're about to remove it
            return EMPTY_MAP;
        }
        UnmodifiableArrayBackedMap newMap = new UnmodifiableArrayBackedMap(numEntries);
        if (indexToRemove > 0) {
            // copy entries before the removed one
            System.arraycopy(backingArray, 1, newMap.backingArray, 1, indexToRemove * 2);
        }
        if (indexToRemove + 1 < numEntries) {
            // copy entries after the removed one
            int nextIndexToCopy = indexToRemove + 1;
            int numRemainingEntries = numEntries - nextIndexToCopy;
            System.arraycopy(
                    backingArray,
                    getArrayIndexForKey(nextIndexToCopy),
                    newMap.backingArray,
                    getArrayIndexForKey(indexToRemove),
                    numRemainingEntries * 2);
        }

        newMap.numEntries = numEntries - 1;
        newMap.updateNumEntriesInArray();
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
    public UnmodifiableArrayBackedMap copyAndRemoveAll(Iterable<String> keysToRemoveIterable) {
        if (isEmpty()) {
            // shortcut: if this map is empty, the result will continue to be empty
            return EMPTY_MAP;
        }

        // now we build a Set of keys to remove
        Set<String> keysToRemoveSet;
        if (keysToRemoveIterable instanceof Set) {
            // we already have a set, let's cast it and reuse it
            keysToRemoveSet = (Set<String>) keysToRemoveIterable;
        } else {
            // iterate through the keys and build a set
            keysToRemoveSet = new HashSet<>();
            for (String key : keysToRemoveIterable) {
                keysToRemoveSet.add(key);
            }
        }

        int firstIndexToKeep = -1;
        int lastIndexToKeep = -1;
        int destinationIndex = 0;
        int numEntriesKept = 0;
        // build the new map
        UnmodifiableArrayBackedMap newMap = new UnmodifiableArrayBackedMap(numEntries);
        for (int indexInCurrentMap = 0; indexInCurrentMap < numEntries; indexInCurrentMap++) {
            // for each key in this map, check whether it's in the set we built above
            Object key = backingArray[getArrayIndexForKey(indexInCurrentMap)];
            if (!keysToRemoveSet.contains(key)) {
                // this key should be kept
                if (firstIndexToKeep == -1) {
                    firstIndexToKeep = indexInCurrentMap;
                }
                lastIndexToKeep = indexInCurrentMap;
            } else if (lastIndexToKeep > 0) {
                // we hit a remove, copy any keys that are known ready
                int numEntriesToCopy = lastIndexToKeep - firstIndexToKeep + 1;
                System.arraycopy(
                        backingArray,
                        getArrayIndexForKey(firstIndexToKeep),
                        newMap.backingArray,
                        getArrayIndexForKey(destinationIndex),
                        numEntriesToCopy * 2);
                firstIndexToKeep = -1;
                lastIndexToKeep = -1;
                destinationIndex += numEntriesToCopy;
                numEntriesKept += numEntriesToCopy;
            }
        }

        if (lastIndexToKeep > -1) {
            // at least one key still requires copying
            int numEntriesToCopy = lastIndexToKeep - firstIndexToKeep + 1;
            System.arraycopy(
                    backingArray,
                    getArrayIndexForKey(firstIndexToKeep),
                    newMap.backingArray,
                    getArrayIndexForKey(destinationIndex),
                    numEntriesToCopy * 2);
            numEntriesKept += numEntriesToCopy;
        }

        if (numEntriesKept == 0) {
            return EMPTY_MAP;
        }

        newMap.numEntries = numEntriesKept;
        newMap.updateNumEntriesInArray();

        return newMap;
    }

    /**
     * Copies the locally-tracked numEntries into the first array slot. Requires
     * autoboxing so call should be minimized - for example, once per bulk update
     * operation.
     */
    private void updateNumEntriesInArray() {
        backingArray[0] = numEntries;
    }

    /**
     * This version of forEach is defined on the Map interface.
     */
    @Override
    public void forEach(java.util.function.BiConsumer<? super String, ? super String> action) {
        for (int i = 0; i < numEntries; i++) {
            // BiConsumer should be able to handle values of any type V. In our case the values are of type String.
            final String key = (String) backingArray[getArrayIndexForKey(i)];
            final String value = (String) backingArray[getArrayIndexForValue(i)];
            action.accept(key, value);
        }
    }

    /**
     * This version of forEach is defined on the ReadOnlyStringMap interface.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V> void forEach(final org.apache.logging.log4j.util.BiConsumer<String, ? super V> action) {
        for (int i = 0; i < numEntries; i++) {
            // BiConsumer should be able to handle values of any type V. In our case the values are of type String.
            final String key = (String) backingArray[getArrayIndexForKey(i)];
            final V value = (V) backingArray[getArrayIndexForValue(i)];
            action.accept(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <V, S> void forEach(final TriConsumer<String, ? super V, S> action, final S state) {
        for (int i = 0; i < numEntries; i++) {
            // TriConsumer should be able to handle values of any type V. In our case the values are of type String.
            final String key = (String) backingArray[getArrayIndexForKey(i)];
            final V value = (V) backingArray[getArrayIndexForValue(i)];
            action.accept(key, value, state);
        }
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
        return getValue((String) key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getValue(String key) {
        if (numEntries == 0) {
            return null;
        }
        int hashCode = key.hashCode();
        for (int i = 0; i < numEntries; i++) {
            if (backingArray[getArrayIndexForKey(i)].hashCode() == hashCode
                    && backingArray[getArrayIndexForKey(i)].equals(key)) {
                return (V) backingArray[getArrayIndexForValue(i)];
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
            if (backingArray[getArrayIndexForKey(i)].hashCode() == keyHashCode
                    && backingArray[getArrayIndexForKey(i)].equals(key)) {
                // found a match, overwrite then return
                backingArray[getArrayIndexForValue(i)] = value;
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

    @Override
    public Map<String, String> toMap() {
        return this;
    }
}
