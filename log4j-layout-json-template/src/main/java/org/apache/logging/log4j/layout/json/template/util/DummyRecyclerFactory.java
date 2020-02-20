package org.apache.logging.log4j.layout.json.template.util;

import java.util.function.Function;
import java.util.function.Supplier;

public class DummyRecyclerFactory implements RecyclerFactory {

    private static final DummyRecyclerFactory INSTANCE = new DummyRecyclerFactory();

    private DummyRecyclerFactory() {}

    public static DummyRecyclerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public <V> Recycler<V> create(
            final Supplier<V> supplier,
            final Function<V, V> cleaner) {
        return new DummyRecycler<V>(supplier);
    }

}
