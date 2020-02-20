package org.apache.logging.log4j.layout.json.template.util;

import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

public class QueueingRecyclerFactory implements RecyclerFactory {

    private final Supplier<Queue<Object>> queueSupplier;

    public QueueingRecyclerFactory(final Supplier<Queue<Object>> queueSupplier) {
        this.queueSupplier = queueSupplier;
    }

    @Override
    public <V> Recycler<V> create(
            final Supplier<V> supplier,
            final Function<V, V> cleaner) {
        @SuppressWarnings("unchecked")
        final Queue<V> queue = (Queue<V>) queueSupplier.get();
        return new QueueingRecycler<V>(supplier, cleaner, queue);
    }

}
