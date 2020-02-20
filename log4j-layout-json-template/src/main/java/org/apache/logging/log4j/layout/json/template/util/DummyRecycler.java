package org.apache.logging.log4j.layout.json.template.util;

import java.util.function.Supplier;

public class DummyRecycler<V> implements Recycler<V> {

    private final Supplier<V> supplier;

    public DummyRecycler(final Supplier<V> supplier) {
        this.supplier = supplier;
    }

    @Override
    public V acquire() {
        return supplier.get();
    }

    @Override
    public void release(final V value) {}

}
