package org.apache.logging.log4j.layout.json.template.util;

import java.util.function.Function;
import java.util.function.Supplier;

public class ThreadLocalRecyclerFactory implements RecyclerFactory {

    private static final ThreadLocalRecyclerFactory INSTANCE =
            new ThreadLocalRecyclerFactory();

    private ThreadLocalRecyclerFactory() {}

    public static ThreadLocalRecyclerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public <V> Recycler<V> create(
            final Supplier<V> supplier,
            final Function<V, V> cleaner) {
        return new ThreadLocalRecycler<>(supplier, cleaner);
    }

}
