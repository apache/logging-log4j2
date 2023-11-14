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
package org.apache.logging.log4j.internal;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class CopyOnWriteNavigableSet<E> extends TreeSet<E> {
    private static final long serialVersionUID = 1L;

    private volatile NavigableSet<E> set;
    private final Comparator<? super E> comparator;

    public CopyOnWriteNavigableSet(final Comparator<? super E> comparator) {
        set = new TreeSet<>(comparator);
        this.comparator = comparator;
    }

    private CopyOnWriteNavigableSet(final CopyOnWriteNavigableSet<E> copyOnWriteSet) {
        this.set = new TreeSet<>(copyOnWriteSet.set);
        this.comparator = copyOnWriteSet.comparator;
    }

    @Override
    public Iterator<E> iterator() {
        return set.iterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return set.descendingIterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return set.descendingSet();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return set.contains(o);
    }

    @Override
    public boolean add(final E e) {
        final NavigableSet<E> newSet = new TreeSet<E>(set);
        final boolean result = newSet.add(e);
        if (result) {
            set = newSet;
        }
        return result;
    }

    @Override
    public boolean remove(final Object o) {
        final NavigableSet<E> newSet = new TreeSet<E>(set);
        final boolean result = newSet.remove(o);
        if (result) {
            set = newSet;
        }
        return result;
    }

    @Override
    public void clear() {
        final NavigableSet<E> newSet = new TreeSet<E>(set);
        newSet.clear();
        set = newSet;
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        final NavigableSet<E> newSet = new TreeSet<E>(set);
        final boolean result = newSet.addAll(c);
        if (result) {
            set = newSet;
        }
        return result;
    }

    @Override
    public NavigableSet<E> subSet(final E fromElement, final boolean fromInclusive, final E toElement, final boolean toInclusive) {
        return set.subSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public NavigableSet<E> headSet(final E toElement, final boolean inclusive) {
        return set.headSet(toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(final E fromElement, final boolean inclusive) {
        return set.tailSet(fromElement, inclusive);
    }

    @Override
    public SortedSet<E> subSet(final E fromElement, final E toElement) {
        return set.subSet(fromElement, toElement);
    }

    @Override
    public SortedSet<E> headSet(final E toElement) {
        return set.headSet(toElement);
    }

    @Override
    public SortedSet<E> tailSet(final E fromElement) {
        return set.tailSet(fromElement);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E first() {
        return set.first();
    }

    @Override
    public E last() {
        return set.last();
    }

    @Override
    public E lower(final E e) {
        return set.lower(e);
    }

    @Override
    public E floor(final E e) {
        return set.floor(e);
    }

    @Override
    public E ceiling(final E e) {
        return set.ceiling(e);
    }

    @Override
    public E higher(final E e) {
        return set.higher(e);
    }

    @Override
    public E pollFirst() {
        return set.pollFirst();
    }

    @Override
    public E pollLast() {
        return set.pollLast();
    }

    @Override
    public Object clone() {
        return new CopyOnWriteNavigableSet<E>(this);
    }

    @Override
    public Spliterator<E> spliterator() {
        return set.spliterator();
    }

    @Override
    public boolean equals(final Object o) {
        return set.equals(o);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return set.removeAll(c);
    }

    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @Override
    public <T> T[] toArray(final T[] a) {
        return set.toArray(a);
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return set.retainAll(c);
    }

    @Override
    public String toString() {
        return set.toString();
    }

    @Override
    public <T> T[] toArray(final IntFunction<T[]> generator) {
        return set.toArray(generator);
    }

    @Override
    public boolean removeIf(final Predicate<? super E> filter) {
        final NavigableSet<E> newSet = new TreeSet<E>(set);
        final boolean result = newSet.removeIf(filter);
        if (result) {
            this.set = newSet;
        }
        return result;
    }

    @Override
    public Stream<E> stream() {
        return set.stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return set.parallelStream();
    }

    @Override
    public void forEach(final Consumer<? super E> action) {
        set.forEach(action);
    }
}
