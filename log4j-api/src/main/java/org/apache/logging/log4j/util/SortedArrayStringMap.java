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
package org.apache.logging.log4j.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.util.internal.SerializationUtil;

/**
 * <em>Consider this class private.</em>
 * Array-based implementation of the {@code ReadOnlyStringMap} interface. Keys are held in a sorted array.
 * <p>
 * This is not a generic collection, but makes some trade-offs to optimize for the Log4j context data use case:
 * </p>
 * <ul>
 *   <li>Garbage-free iteration over key-value pairs with {@code BiConsumer} and {@code TriConsumer}.</li>
 *   <li>Fast copy. If the ThreadContextMap is also an instance of {@code SortedArrayStringMap}, the full thread context
 *     data can be transferred with two array copies and two field updates.</li>
 *   <li>Acceptable performance for small data sets. The current implementation stores keys in a sorted array, values
 *     are stored in a separate array at the same index.
 *     Worst-case performance of {@code get} and {@code containsKey} is O(log N),
 *     worst-case performance of {@code put} and {@code remove} is O(N log N).
 *     The expectation is that for the small values of {@code N} (less than 100) that are the vast majority of
 *     ThreadContext use cases, the constants dominate performance more than the asymptotic performance of the
 *     algorithms used.
 *     </li>
 *     <li>Compact representation.</li>
 * </ul>
 *
 * @since 2.7
 */
@InternalApi
public class SortedArrayStringMap implements IndexedStringMap {

    /**
     * The default initial capacity.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 4;

    private static final long serialVersionUID = -5748905872274478116L;
    private static final int HASHVAL = 31;

    private static final TriConsumer<String, Object, StringMap> PUT_ALL =
            (key, value, contextData) -> contextData.putValue(key, value);

    /**
     * An empty array instance to share when the table is not inflated.
     */
    private static final String[] EMPTY = Strings.EMPTY_ARRAY;

    private static final String FROZEN = "Frozen collection cannot be modified";

    private transient String[] keys = EMPTY;
    private transient Object[] values = EMPTY;

    /**
     * The number of key-value mappings contained in this map.
     */
    private transient int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     * @serial
     */
    // If table == EMPTY_TABLE then this is the initial capacity at which the
    // table will be created when inflated.
    private int threshold;

    private boolean immutable;
    private transient boolean iterating;

    public SortedArrayStringMap() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public SortedArrayStringMap(final int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity must be at least zero but was " + initialCapacity);
        }
        threshold = ceilingNextPowerOfTwo(initialCapacity == 0 ? 1 : initialCapacity);
    }

    public SortedArrayStringMap(final ReadOnlyStringMap other) {
        if (other instanceof SortedArrayStringMap) {
            initFrom0((SortedArrayStringMap) other);
        } else if (other != null) {
            resize(ceilingNextPowerOfTwo(other.size()));
            other.forEach(PUT_ALL, this);
        }
    }

    public SortedArrayStringMap(final Map<String, ?> map) {
        resize(ceilingNextPowerOfTwo(map.size()));
        for (final Map.Entry<String, ?> entry : map.entrySet()) {
            // The key might not actually be a String.
            putValue(Objects.toString(entry.getKey(), null), entry.getValue());
        }
    }

    private void assertNotFrozen() {
        if (immutable) {
            throw new UnsupportedOperationException(FROZEN);
        }
    }

    private void assertNoConcurrentModification() {
        if (iterating) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public void clear() {
        if (keys == EMPTY) {
            return;
        }
        assertNotFrozen();
        assertNoConcurrentModification();

        Arrays.fill(keys, 0, size, null);
        Arrays.fill(values, 0, size, null);
        size = 0;
    }

    @Override
    public boolean containsKey(final String key) {
        return indexOfKey(key) >= 0;
    }

    @Override
    public Map<String, String> toMap() {
        final Map<String, String> result = new HashMap<>(size());
        for (int i = 0; i < size(); i++) {
            final Object value = getValueAt(i);
            result.put(getKeyAt(i), value == null ? null : String.valueOf(value));
        }
        return result;
    }

    @Override
    public void freeze() {
        immutable = true;
    }

    @Override
    public boolean isFrozen() {
        return immutable;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getValue(final String key) {
        final int index = indexOfKey(key);
        if (index < 0) {
            return null;
        }
        return (V) values[index];
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int indexOfKey(final String key) {
        if (keys == EMPTY) {
            return -1;
        }
        if (key == null) { // null key is located at the start of the array
            return nullKeyIndex(); // insert at index zero
        }
        final int start = size > 0 && keys[0] == null ? 1 : 0;
        return Arrays.binarySearch(keys, start, size, key);
    }

    private int nullKeyIndex() {
        return size > 0 && keys[0] == null ? 0 : ~0;
    }

    @Override
    public void putValue(final String key, final Object value) {
        assertNotFrozen();
        assertNoConcurrentModification();

        if (keys == EMPTY) {
            inflateTable(threshold);
        }
        final int index = indexOfKey(key);
        if (index >= 0) {
            keys[index] = key;
            values[index] = value;
        } else { // not found, so insert.
            insertAt(~index, key, value);
        }
    }

    private void insertAt(final int index, final String key, final Object value) {
        ensureCapacity();
        System.arraycopy(keys, index, keys, index + 1, size - index);
        System.arraycopy(values, index, values, index + 1, size - index);
        keys[index] = key;
        values[index] = value;
        size++;
    }

    @Override
    public void putAll(final ReadOnlyStringMap source) {
        if (source == this || source == null || source.isEmpty()) {
            // this.putAll(this) does not modify this collection
            // this.putAll(null) does not modify this collection
            // this.putAll(empty ReadOnlyStringMap) does not modify this collection
            return;
        }
        assertNotFrozen();
        assertNoConcurrentModification();

        if (source instanceof SortedArrayStringMap) {
            if (this.size == 0) {
                initFrom0((SortedArrayStringMap) source);
            } else {
                merge((SortedArrayStringMap) source);
            }
        } else if (source != null) {
            source.forEach(PUT_ALL, this);
        }
    }

    private void initFrom0(final SortedArrayStringMap other) {
        if (keys.length < other.size) {
            keys = new String[other.threshold];
            values = new Object[other.threshold];
        }
        System.arraycopy(other.keys, 0, keys, 0, other.size);
        System.arraycopy(other.values, 0, values, 0, other.size);

        size = other.size;
        threshold = other.threshold;
    }

    private void merge(final SortedArrayStringMap other) {
        final String[] myKeys = keys;
        final Object[] myVals = values;
        final int newSize = other.size + this.size;
        threshold = ceilingNextPowerOfTwo(newSize);
        if (keys.length < threshold) {
            keys = new String[threshold];
            values = new Object[threshold];
        }
        // move largest collection to the beginning of this data structure, smallest to the end
        boolean overwrite = true;
        if (other.size() > size()) {
            // move original key-values to end
            System.arraycopy(myKeys, 0, keys, other.size, this.size);
            System.arraycopy(myVals, 0, values, other.size, this.size);

            // insert additional key-value pairs at the beginning
            System.arraycopy(other.keys, 0, keys, 0, other.size);
            System.arraycopy(other.values, 0, values, 0, other.size);
            size = other.size;

            // loop over original keys and insert (drop values for same key)
            overwrite = false;
        } else {
            System.arraycopy(myKeys, 0, keys, 0, this.size);
            System.arraycopy(myVals, 0, values, 0, this.size);

            // move additional key-value pairs to end
            System.arraycopy(other.keys, 0, keys, this.size, other.size);
            System.arraycopy(other.values, 0, values, this.size, other.size);

            // new values are at the end, will be processed below. Overwrite is true.
        }
        for (int i = size; i < newSize; i++) {
            final int index = indexOfKey(keys[i]);
            if (index < 0) { // key does not exist
                insertAt(~index, keys[i], values[i]);
            } else if (overwrite) { // existing key: only overwrite when looping over the new values
                keys[index] = keys[i];
                values[index] = values[i];
            }
        }
        // prevent memory leak: null out references
        Arrays.fill(keys, size, newSize, null);
        Arrays.fill(values, size, newSize, null);
    }

    private void ensureCapacity() {
        if (size >= threshold) {
            resize(threshold * 2);
        }
    }

    private void resize(final int newCapacity) {
        final String[] oldKeys = keys;
        final Object[] oldValues = values;

        keys = new String[newCapacity];
        values = new Object[newCapacity];

        System.arraycopy(oldKeys, 0, keys, 0, size);
        System.arraycopy(oldValues, 0, values, 0, size);

        threshold = newCapacity;
    }

    /**
     * Inflates the table.
     */
    private void inflateTable(final int toSize) {
        threshold = toSize;
        keys = new String[toSize];
        values = new Object[toSize];
    }

    @Override
    public void remove(final String key) {
        if (keys == EMPTY) {
            return;
        }

        final int index = indexOfKey(key);
        if (index >= 0) {
            assertNotFrozen();
            assertNoConcurrentModification();

            System.arraycopy(keys, index + 1, keys, index, size - 1 - index);
            System.arraycopy(values, index + 1, values, index, size - 1 - index);
            keys[size - 1] = null;
            values[size - 1] = null;
            size--;
        }
    }

    @Override
    public String getKeyAt(final int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return keys[index];
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getValueAt(final int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return (V) values[index];
    }

    @Override
    public int size() {
        return size;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> void forEach(final BiConsumer<String, ? super V> action) {
        iterating = true;
        try {
            for (int i = 0; i < size; i++) {
                action.accept(keys[i], (V) values[i]);
            }
        } finally {
            iterating = false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V, T> void forEach(final TriConsumer<String, ? super V, T> action, final T state) {
        iterating = true;
        try {
            for (int i = 0; i < size; i++) {
                action.accept(keys[i], (V) values[i], state);
            }
        } finally {
            iterating = false;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof SortedArrayStringMap)) {
            return false;
        }
        final SortedArrayStringMap other = (SortedArrayStringMap) obj;
        if (this.size() != other.size()) {
            return false;
        }
        for (int i = 0; i < size(); i++) {
            if (!Objects.equals(keys[i], other.keys[i])) {
                return false;
            }
            if (!Objects.equals(values[i], other.values[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 37;
        result = HASHVAL * result + size;
        result = HASHVAL * result + hashCode(keys, size);
        result = HASHVAL * result + hashCode(values, size);
        return result;
    }

    private static int hashCode(final Object[] values, final int length) {
        int result = 1;
        for (int i = 0; i < length; i++) {
            result = HASHVAL * result + (values[i] == null ? 0 : values[i].hashCode());
        }
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(keys[i]).append('=');
            sb.append(values[i] == this ? "(this map)" : values[i]);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Save the state of the {@code SortedArrayStringMap} instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the SortedArrayStringMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     */
    private void writeObject(final java.io.ObjectOutputStream s) throws IOException {
        // Write out the threshold, and any hidden stuff
        s.defaultWriteObject();

        // Write out number of buckets
        if (keys == EMPTY) {
            s.writeInt(ceilingNextPowerOfTwo(threshold));
        } else {
            s.writeInt(keys.length);
        }

        // Write out size (number of Mappings)
        s.writeInt(size);

        // Write out keys and values (alternating)
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                s.writeObject(keys[i]);
                SerializationUtil.writeWrappedObject(
                        (values[i] instanceof Serializable) ? (Serializable) values[i] : null, s);
            }
        }
    }

    /**
     * Calculate the next power of 2, greater than or equal to x.
     * <p>
     * From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
     *
     * @param x Value to round up
     * @return The next power of 2 from x inclusive
     */
    private static int ceilingNextPowerOfTwo(final int x) {
        final int BITS_PER_INT = 32;
        return 1 << (BITS_PER_INT - Integer.numberOfLeadingZeros(x - 1));
    }

    /**
     * Reconstitute the {@code SortedArrayStringMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(final java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        SerializationUtil.assertFiltered(s);
        // Read in the threshold (ignored), and any hidden stuff
        s.defaultReadObject();

        // set other fields that need values
        keys = EMPTY;
        values = EMPTY;

        // Read in number of buckets
        final int capacity = s.readInt();
        if (capacity < 0) {
            throw new InvalidObjectException("Illegal capacity: " + capacity);
        }

        // Read number of mappings
        final int mappings = s.readInt();
        if (mappings < 0) {
            throw new InvalidObjectException("Illegal mappings count: " + mappings);
        }

        // allocate the bucket array;
        if (mappings > 0) {
            inflateTable(capacity);
        } else {
            threshold = capacity;
        }

        // Read the keys and values, and put the mappings in the arrays
        for (int i = 0; i < mappings; i++) {
            keys[i] = (String) s.readObject();
            values[i] = SerializationUtil.readWrappedObject(s);
        }
        size = mappings;
    }
}
