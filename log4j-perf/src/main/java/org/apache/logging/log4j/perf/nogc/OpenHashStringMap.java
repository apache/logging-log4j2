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
package org.apache.logging.log4j.perf.nogc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;

/**
 * Open hash map-based implementation of the {@code ReadOnlyStringMap} interface.
 * Implementation based on <a href="http://fastutil.di.unimi.it/">fastutil</a>'s
 * <a href="http://fastutil.di.unimi.it/docs/it/unimi/dsi/fastutil/objects/Object2ObjectOpenHashMap.html">Object2ObjectOpenHashMap</a>.
 * <p>
 * A type-specific hash map with a fast, small-footprint implementation.
 *
 * <P>
 * Instances of this class use a hash table to represent a map. The table is
 * filled up to a specified <em>load factor</em>, and then doubled in size to
 * accommodate new entries. If the table is emptied below <em>one fourth</em> of
 * the load factor, it is halved in size. However, halving is not performed when
 * deleting entries from an iterator, as it would interfere with the iteration
 * process.
 *
 * <p>
 * Note that {@link #clear()} does not modify the hash table size. Rather, the
 * {@link #trim(int)} method lets you control the size of
 * the table; this is particularly useful if you reuse instances of this class.
 * <p>
 * <ul>
 *   <li>Garbage-free iteration over key-value pairs with {@code BiConsumer} and {@code TriConsumer}.</li>
 *   <li>Fast copy. If the ThreadContextMap is also an instance of {@code OpenHashStringMap},
 *     the full thread context data can be transferred with two array copies and five field updates.</li>
 * </ul>
 *
 * @since 2.7
 */
public class OpenHashStringMap<K, V> implements StringMap, ThreadContextMap {
    /** The initial default size of a hash table. */
    public static final int DEFAULT_INITIAL_SIZE = 16;

    /** The default load factor of a hash table. */
    public static final float DEFAULT_LOAD_FACTOR = .75f;

    private static final String FROZEN = "Frozen collection cannot be modified";
    private static final long serialVersionUID = -1486744623338827187L;

    /** The array of keys. */
    protected transient K[] keys;
    /** The array of values. */
    protected transient V[] values;
    /** The mask for wrapping a position counter. */
    protected transient int mask;
    /** Whether this set contains the key zero. */
    protected transient boolean containsNullKey;
    /** The current table size. */
    protected transient int arraySize;
    /**
     * Threshold after which we rehash. It must be the table size times {@link #loadFactor}.
     */
    protected transient int maxFill;
    /** Number of entries in the set (including the key zero, if present). */
    protected int size;
    /** The acceptable load factor. */
    protected final float loadFactor;

    private final V defRetValue = null;
    private boolean immutable;
    private transient boolean iterating;

    /**
     * Creates a new hash map with initial expected
     * {@link #DEFAULT_INITIAL_SIZE} entries and
     * {@link #DEFAULT_LOAD_FACTOR} as load factor.
     */
    public OpenHashStringMap() {
        this(DEFAULT_INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }
    /**
     * Creates a new hash map with {@link #DEFAULT_LOAD_FACTOR} as load factor.
     *
     * @param expected
     *            the expected number of elements in the hash map.
     */
    public OpenHashStringMap(final int expected) {
        this(expected, DEFAULT_LOAD_FACTOR);
    }
    /**
     * Creates a new hash map.
     *
     * <p>
     * The actual table size will be the least power of two greater than
     * <code>expected</code>/<code>f</code>.
     *
     * @param expected
     *            the expected number of elements in the hash set.
     * @param f
     *            the load factor.
     */
    @SuppressWarnings("unchecked")
    public OpenHashStringMap(final int expected, final float f) {
        if (f <= 0 || f > 1) {
            throw new IllegalArgumentException(
                    "Load factor must be greater than 0 and smaller than or equal to 1");
        }
        if (expected < 0){
            throw new IllegalArgumentException(
                    "The expected number of elements must be nonnegative");
        }
        this.loadFactor = f;
        arraySize = HashCommon.arraySize(expected, f);
        mask = arraySize - 1;
        maxFill = HashCommon.maxFill(arraySize, f);
        keys = (K[]) new Object[arraySize + 1];
        values = (V[]) new Object[arraySize + 1];
    }
    /**
     * Creates a new hash map with {@link #DEFAULT_LOAD_FACTOR} as load
     * factor copying a given one.
     *
     * @param map
     *            a {@link Map} to be copied into the new hash map.
     */
    public OpenHashStringMap(final Map<? extends K, ? extends V> map) {
        this(map, DEFAULT_LOAD_FACTOR);
    }
    /**
     * Creates a new hash map copying a given one.
     *
     * @param map
     *            a {@link Map} to be copied into the new hash map.
     * @param f
     *            the load factor.
     */
    public OpenHashStringMap(final Map<? extends K, ? extends V> map, final float f) {
        this(map.size(), f);
        putAll(map);
    }

    /**
     * Creates a new hash map with {@link #DEFAULT_LOAD_FACTOR} as load
     * factor copying a given type-specific one.
     *
     * @param contextData
     *            a type-specific map to be copied into the new hash map.
     */
    public OpenHashStringMap(final ReadOnlyStringMap contextData) {
        this(contextData, DEFAULT_LOAD_FACTOR);
    }
    /**
     * Creates a new hash map copying a given type-specific one.
     *
     * @param contextData
     *            a type-specific map to be copied into the new hash map.
     * @param f
     *            the load factor.
     */
    public OpenHashStringMap(final ReadOnlyStringMap contextData, final float f) {
        this(contextData.size(), f);
        if (contextData instanceof OpenHashStringMap) {
            initFrom0((OpenHashStringMap) contextData);
        } else {
            contextData.forEach(PUT_ALL, this);
        }
    }
    private static final TriConsumer<String, Object, StringMap> PUT_ALL =
            new TriConsumer<String, Object, StringMap>() {
        @Override
        public void accept(final String key, final Object value, final StringMap contextData) {
            contextData.putValue(key, value);
        }
    };

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

    @SuppressWarnings("unchecked")
    private void initFrom0(final OpenHashStringMap other) {
        // this.loadFactor = other.loadFactor; // final field
        this.arraySize = other.arraySize;
        this.size = other.size;
        this.containsNullKey = other.containsNullKey;
        this.mask = other.mask;
        this.maxFill = other.maxFill;
        keys = (K[]) Arrays.copyOf(other.keys, arraySize + 1);
        values = (V[]) Arrays.copyOf(other.values, arraySize + 1);
    }

    private int realSize() {
        return containsNullKey ? size - 1 : size;
    }

    private void ensureCapacity(final int capacity) {
        final int needed = HashCommon.arraySize(capacity, loadFactor);
        if (needed > arraySize) {
            rehash(needed);
        }
    }

    private void tryCapacity(final long capacity) {
        final int needed = Math.min(
                1 << 30, Math.max(2, HashCommon.nextPowerOfTwo((int) Math.ceil(capacity / loadFactor))));
        if (needed > arraySize) {
            rehash(needed);
        }
    }

    @Override
    public Map<String, String> toMap() {
        final Map<String, String> result = new HashMap<>(size);
        forEach(COPY_INTO_MAP, result);
        return result;
    }

    private static final TriConsumer<String, Object, Map<String, String>> COPY_INTO_MAP =
            new TriConsumer<String, Object, Map<String, String>>() {
        @Override
        public void accept(final String k, final Object v, final Map<String, String> map) {
            map.put(k, v == null ? null : v.toString());
        }
    };

    /*
     * Removes all elements from this map.
     *
     * <P>To increase object reuse, this method does not change the table size.
     * If you want to reduce the table size, you must use {@link #trim()}.
     */
    @Override
    public void clear() {
        if (size == 0) {
            return;
        }
        assertNotFrozen();
        assertNoConcurrentModification();

        size = 0;
        containsNullKey = false;
        Arrays.fill(keys, (null));
        Arrays.fill(values, null);
    }

    @Override
    public boolean containsKey(final String key) {
        return containsObjectKey(key);
    }

    @SuppressWarnings("unchecked")
    private boolean containsObjectKey(final Object k) {
        if (k == null) {
            return containsNullKey;
        }
        K curr;
        final K[] key = this.keys;
        int pos;
        // The starting point.
        if ((curr = key[pos = HashCommon.mix(k.hashCode()) & mask]) == null) {
            return false;
        }
        if (k.equals(curr)) {
            return true;
        }
        // There's always an unused entry.
        while (true) {
            if ((curr = key[pos = (pos + 1) & mask]) == null) {
                return false;
            }
            if (k.equals(curr)) {
                return true;
            }
        }
    }

    @Override
	public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ReadOnlyStringMap)) {
            return false;
        }
        final ReadOnlyStringMap other = (ReadOnlyStringMap) obj;
        if (other.size() != size()) {
            return false;
        }
        int pos = arraySize;
        if (containsNullKey) {
            if (!Objects.equals(getObjectValue(null), other.getValue(null))) {
                return false;
            }
        }
        --pos;
        final K myKeys[] = this.keys;
        for (; pos >= 0; pos--) {
            K k;
            if ((k = myKeys[pos]) != null) {
                if (!Objects.equals(values[pos], other.getValue((String) k))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <VAL> void forEach(final BiConsumer<String, ? super VAL> action) {
        final int startSize = size;
        final K myKeys[] = this.keys;
        int pos = arraySize;

        iterating = true;
        try {
            if (containsNullKey) {
                action.accept((String) myKeys[pos], (VAL) values[pos]);
                if (size != startSize) {
                    throw new ConcurrentModificationException();
                }
            }
            --pos;
            for (; pos >= 0; pos--) {
                if (myKeys[pos] != null) {
                    action.accept((String) myKeys[pos], (VAL) values[pos]);
                    if (size != startSize) {
                        throw new ConcurrentModificationException();
                    }
                }
            }
        } finally {
            iterating = false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <VAL, STATE> void forEach(final TriConsumer<String, ? super VAL, STATE> action, final STATE state) {
        final int startSize = size;
        final K myKeys[] = this.keys;
        int pos = arraySize;

        iterating = true;
        try {
            if (containsNullKey) {
                action.accept((String) myKeys[pos], (VAL) values[pos], state);
                if (size != startSize) {
                    throw new ConcurrentModificationException();
                }
            }
            --pos;
            for (; pos >= 0; pos--) {
                if (myKeys[pos] != null) {
                    action.accept((String) myKeys[pos], (VAL) values[pos], state);
                    if (size != startSize) {
                        throw new ConcurrentModificationException();
                    }
                }
            }
        } finally {
            iterating = false;
        }
    }

    @Override
    public String get(final String key) {
        return (String) getObjectValue(key);
    }

    @SuppressWarnings("unchecked")
    private V getObjectValue(final Object k) {
        if (k == null) {
            return containsNullKey ? values[arraySize] : defRetValue;
        }
        K curr;
        final K[] key = this.keys;
        int pos;
        // The starting point.
        if ((curr = key[pos = HashCommon.mix(k.hashCode()) & mask]) == null) {
            return defRetValue;
        }
        if (k.equals(curr)) {
            return values[pos];
        }
        // There's always an unused entry.
        while (true) {
            if (((curr = key[pos = (pos + 1) & mask]) == null)) {
                return defRetValue;
            }
            if (k.equals(curr)) {
                return values[pos];
            }
        }
    }

    @Override
    public Map<String, String> getCopy() {
        return toMap();
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        return isEmpty() ? null : Collections.unmodifiableMap(toMap());
    }

    @Override
    public <VAL> VAL getValue(final String key) {
        return (VAL) getObjectValue(key);
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(final String key, final String value) {
        putObjectValue((K) key, (V) value);
    }

    private int insert(final K k, final V v) {
        int pos;
        if (k == null) {
            if (containsNullKey) {
                return arraySize;
            }
            containsNullKey = true;
            pos = arraySize;
        } else {
            K curr;
            final K[] key = this.keys;
            // The starting point.
            if (!((curr = key[pos = HashCommon.mix(k.hashCode()) & mask]) == null)) {
                if (curr.equals(k)) {
                    return pos;
                }
                while (!((curr = key[pos = (pos + 1) & mask]) == null)) {
                    if (curr.equals(k)) {
                        return pos;
                    }
                }
            }
        }
        keys[pos] = k;
        values[pos] = v;
        if (size++ >= maxFill) {
            rehash(HashCommon.arraySize(size + 1, loadFactor));
        }
        return -1;
    }

    @Override
    public void putAll(final ReadOnlyStringMap source) {
        assertNotFrozen();
        assertNoConcurrentModification();

        if (size() == 0 && source instanceof OpenHashStringMap) {
            initFrom0((OpenHashStringMap) source);
        } else if (source != null) {
            source.forEach(PUT_ALL, this);
        }
    }

    /** {@inheritDoc} */
    public void putAll(final Map<? extends K, ? extends V> map) {
        if (loadFactor <= .5) {
            // The resulting map will be sized for m.size() elements
            ensureCapacity(map.size());
        } else {
            // The resulting map will be tentatively sized for size() +  m.size() elements
            tryCapacity(size() + map.size());
        }
        for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            putObjectValue(entry.getKey(), entry.getValue());
        }
    }

    private V putObjectValue(final K k, final V v) {
        assertNotFrozen();
        assertNoConcurrentModification();

        final int pos = insert(k, v);
        if (pos < 0) {
            return defRetValue;
        }
        final V oldValue = values[pos];
        values[pos] = v;
        return oldValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void putValue(final String key, final Object value) {
        putObjectValue((K) key, (V) value);
    }

    @Override
    public void remove(final String key) {
        removeObjectKey(key);
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
    private V removeObjectKey(final Object k) {
        assertNotFrozen();
        assertNoConcurrentModification();

        if (k == null) {
            if (containsNullKey) {
                return removeNullEntry();
            }
            return defRetValue;
        }
        final K[] key = this.keys;
        int pos = HashCommon.mix(k.hashCode()) & mask;
        K curr = key[pos & mask];
        // The starting point.
        if (curr == null) {
            return defRetValue;
        }
        if (k.equals(curr)) {
            return removeEntry(pos);
        }
        while (true) {
            if ((curr = key[pos = (pos + 1) & mask]) == null) {
                return defRetValue;
            }
            if (k.equals(curr)) {
                return removeEntry(pos);
            }
        }
    }
    private V removeEntry(final int pos) {
        final V oldValue = values[pos];
        values[pos] = null;
        size--;
        shiftKeys(pos);
        if (size < maxFill / 4 && arraySize > DEFAULT_INITIAL_SIZE) {
            rehash(arraySize / 2);
        }
        return oldValue;
    }
    private V removeNullEntry() {
        containsNullKey = false;
        keys[arraySize] = null;
        final V oldValue = values[arraySize];
        values[arraySize] = null;
        size--;
        if (size < maxFill / 4 && arraySize > DEFAULT_INITIAL_SIZE) {
            rehash(arraySize / 2);
        }
        return oldValue;
    }
    /**
     * Shifts left entries with the specified hash code, starting at the
     * specified position, and empties the resulting free entry.
     *
     * @param pos
     *            a starting position.
     */
    private void shiftKeys(int pos) {
        // Shift entries with the same hash.
        int last, slot;
        K curr;
        final K[] myKeys = this.keys;
        for (;;) {
            pos = ((last = pos) + 1) & mask;
            for (;;) {
                if (((curr = myKeys[pos]) == null)) {
                    myKeys[last] = (null);
                    values[last] = null;
                    return;
                }
                slot = HashCommon.mix(curr.hashCode()) & mask;
                if (last <= pos ? (last >= slot || slot > pos) : (last >= slot && slot > pos)) {
                    break;
                }
                pos = (pos + 1) & mask;
            }
            myKeys[last] = curr;
            values[last] = values[pos];
        }
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * Rehashes this map if the table is too large.
     *
     * <P>
     * Let <var>N</var> be the smallest table size that can hold
     * <code>max(n,{@link #size()})</code> entries, still satisfying the load
     * factor. If the current table size is smaller than or equal to
     * <var>N</var>, this method does nothing. Otherwise, it rehashes this map
     * in a table of size <var>N</var>.
     *
     * <P>
     * This method is useful when reusing maps. {@linkplain #clear() Clearing a
     * map} leaves the table size untouched. If you are reusing a map many times,
     * you can call this method with a typical size to avoid keeping around a
     * very large table just because of a few large transient maps.
     *
     * @param n
     *            the threshold for the trimming.
     * @return true if there was enough memory to trim the map.
     */
    public boolean trim(final int n) {
        final int l = HashCommon.nextPowerOfTwo((int) Math.ceil(n / loadFactor));
        if (l >= n || size > HashCommon.maxFill(l, loadFactor)) {
			return true;
		}
        try {
            rehash(l);
        } catch (final OutOfMemoryError cantDoIt) { // unusual to catch OOME but in this case appropriate
            return false;
        }
        return true;
    }
    /**
     * Rehashes the map.
     *
     * <P>
     * This method implements the basic rehashing strategy, and may be overriden
     * by subclasses implementing different rehashing strategies (e.g.,
     * disk-based rehashing). However, you should not override this method
     * unless you understand the internal workings of this class.
     *
     * @param newN
     *            the new size
     */
    @SuppressWarnings("unchecked")
    protected void rehash(final int newN) {
        final K myKeys[] = this.keys;
        final V myValues[] = this.values;
        final int mask = newN - 1; // Note that this is used by the hashing
        // macro
        final K newKey[] = (K[]) new Object[newN + 1];
        final V newValue[] = (V[]) new Object[newN + 1];
        int i = arraySize, pos;
        for (int j = realSize(); j-- != 0;) {
            while (myKeys[--i] == null) {
                // advance i until we find an existing key
            }
            if (newKey[pos = HashCommon.mix(myKeys[i].hashCode()) & mask] != null) { // rehash & check slot availability
                while (newKey[pos = (pos + 1) & mask] != null) {
                    // find available slot at (or immediately following) pos
                }
            }
            newKey[pos] = myKeys[i];
            newValue[pos] = myValues[i];
        }
        newValue[newN] = myValues[arraySize];
        arraySize = newN;
        this.mask = mask;
        maxFill = HashCommon.maxFill(arraySize, loadFactor);
        this.keys = newKey;
        this.values = newValue;
    }

    /**
     * Returns a hash code for this map.
     *
     * @return a hash code for this map.
     */
    @Override
	public int hashCode() {
        int result = 0;
        for (int j = realSize(), i = 0, t = 0; j-- != 0;) {
            while (keys[i] == null) {
                i++;
            }
            if (this != keys[i]) {
                t = keys[i].hashCode();
            }
            if (this != values[i]) {
                t ^= (values[i] == null ? 0 : values[i].hashCode());
            }
            result += t;
            i++;
        }
        // Zero / null keys have hash zero.
        if (containsNullKey) {
            result += (values[arraySize] == null ? 0 : values[arraySize].hashCode());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        arraySize = HashCommon.arraySize(size, loadFactor);
        maxFill = HashCommon.maxFill(arraySize, loadFactor);
        mask = arraySize - 1;
        final K key[] = this.keys = (K[]) new Object[arraySize + 1];
        final V value[] = this.values = (V[]) new Object[arraySize + 1];
        K k;
        V v;
        for (int i = size, pos; i-- != 0;) {
            k = (K) s.readObject();
            v = (V) s.readObject();
            if (k == null) {
                pos = arraySize;
                containsNullKey = true;
            } else {
                pos = HashCommon.mix(k.hashCode()) & mask;
                while (key[pos] != null) {
                    pos = (pos + 1) & mask;
                }
            }
            key[pos] = k;
            value[pos] = v;
        }
    }

    private void writeObject(final ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            forEach(SERIALIZER, s);
        } catch (final RuntimeException runex) {
            if (runex.getCause() instanceof IOException) {
                throw (IOException) runex.getCause();
            }
            throw runex;
        }
    }

    private static final TriConsumer<String, Object, ObjectOutputStream> SERIALIZER =
            new TriConsumer<String, Object, ObjectOutputStream>() {
                @Override
                public void accept(final String k, final Object v, final ObjectOutputStream objectOutputStream) {
                    try {
                        objectOutputStream.writeObject(k);
                        objectOutputStream.writeObject(v);
                    } catch (final IOException ioex) {
                        throw new IllegalStateException(ioex);
                    }
                }
            };

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        final K myKeys[] = this.keys;
        int pos = arraySize;
        boolean first = true;
        if (containsNullKey) {
            sb.append(myKeys[pos] == this ? "(this map)" : myKeys[pos]);
            sb.append('=');
            sb.append(values[pos] == this ? "(this map)" : values[pos]);
            first = false;
        }
        --pos;
        for (; pos >= 0; pos--) {
            if (myKeys[pos] != null) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                sb.append(myKeys[pos] == this ? "(this map)" : myKeys[pos]);
                sb.append('=');
                sb.append(values[pos] == this ? "(this map)" : values[pos]);
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static class HashCommon {
        private HashCommon() {}

        /** 2<sup>32</sup> &middot; &phi;, &phi; = (&#x221A;5 &minus; 1)/2. */
        private static final int INT_PHI = 0x9E3779B9;

        /** The reciprocal of {@link #INT_PHI} modulo 2<sup>32</sup>. */
        private static final int INV_INT_PHI = 0x144cbc89;

        /** Avalanches the bits of an integer by applying the finalisation step of MurmurHash3.
         *
         * <p>This method implements the finalisation step of Austin Appleby's
         * <a href="http://code.google.com/p/smhasher/">MurmurHash3</a>.
         * Its purpose is to avalanche the bits of the argument to within 0.25% bias.
         *
         * @param x an integer.
         * @return a hash value with good avalanching properties.
         */
        public static int murmurHash3(int x) {
            x ^= x >>> 16;
            x *= 0x85ebca6b;
            x ^= x >>> 13;
            x *= 0xc2b2ae35;
            x ^= x >>> 16;
            return x;
        }

        /**
         * Quickly mixes the bits of an integer.
         *
         * <p>This method mixes the bits of the argument by multiplying by the golden ratio and
         * xorshifting the result. It is borrowed from <a href="https://github.com/OpenHFT/Koloboke">Koloboke</a>, and
         * it has slightly worse behaviour than {@link #murmurHash3(int)} (in open-addressing hash tables the average
         * number of probes is slightly larger), but it's much faster.
         *
         * @param x an integer.
         * @return a hash value obtained by mixing the bits of {@code x}.
         * @see #invMix(int)
         */
        public static int mix(final int x) {
            final int h = x * INT_PHI;
            return h ^ (h >>> 16);
        }

        /** The inverse of {@link #mix(int)}. This method is mainly useful to create unit tests.
         *
         * @param x an integer.
         * @return a value that passed through {@link #mix(int)} would give {@code x}.
         */
        public static int invMix(final int x) {
            return (x ^ x >>> 16) * INV_INT_PHI;
        }

        /** Return the least power of two greater than or equal to the specified value.
         *
         * <p>Note that this function will return 1 when the argument is 0.
         *
         * @param x an integer smaller than or equal to 2<sup>30</sup>.
         * @return the least power of two greater than or equal to the specified value.
         */
        public static int nextPowerOfTwo(int x) {
            if (x == 0) {
                return 1;
            }
            x--;
            x |= x >> 1;
            x |= x >> 2;
            x |= x >> 4;
            x |= x >> 8;
            return (x | x >> 16) + 1;
        }

        /** Return the least power of two greater than or equal to the specified value.
         *
         * <p>Note that this function will return 1 when the argument is 0.
         *
         * @param x a long integer smaller than or equal to 2<sup>62</sup>.
         * @return the least power of two greater than or equal to the specified value.
         */
        public static long nextPowerOfTwo(long x) {
            if (x == 0) {
                return 1;
            }
            x--;
            x |= x >> 1;
            x |= x >> 2;
            x |= x >> 4;
            x |= x >> 8;
            x |= x >> 16;
            return (x | x >> 32) + 1;
        }


        /** Returns the maximum number of entries that can be filled before rehashing.
         *
         * @param n the size of the backing array.
         * @param f the load factor.
         * @return the maximum number of entries before rehashing.
         */
        public static int maxFill(final int n, final float f) {
		/* We must guarantee that there is always at least
		 * one free entry (even with pathological load factors). */
            return Math.min((int) Math.ceil(n * f), n - 1);
        }

        /**
         * Returns the least power of two smaller than or equal to 2<sup>30</sup> and larger than or equal to
         * <code>Math.ceil( expected / f )</code>.
         *
         * @param expected the expected number of elements in a hash table.
         * @param f the load factor.
         * @return the minimum possible size for a backing array.
         * @throws IllegalArgumentException if the necessary size is larger than 2<sup>30</sup>.
         */
        public static int arraySize(final int expected, final float f) {
            final long result = Math.max(2, nextPowerOfTwo((long) Math.ceil(expected / f)));
            if (result > (1 << 30)) {
                throw new IllegalArgumentException("Too large (" + expected +
                        " expected elements with load factor " + f + ")");
            }
            return (int) result;
        }
    }
}
