package org.apache.logging.log4j.layout.json.template.util;

import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadLocalRecycler<V> implements Recycler<V> {

    private final Supplier<V> supplier;

    private final Function<V, V> cleaner;

    private final ThreadLocal<V> holder;

    public ThreadLocalRecycler(
            final Supplier<V> supplier,
            final Function<V, V> cleaner) {
        this.supplier = supplier;
        this.cleaner = cleaner;
        this.holder = new ThreadLocal<>();
    }

    @Override
    public V acquire() {
        final V value = holder.get();
        if (value == null) {
            return supplier.get();
        } else {
            holder.set(null);
            return cleaner.apply(value);
        }
    }

    @Override
    public void release(final V value) {
        holder.set(value);
    }

}
