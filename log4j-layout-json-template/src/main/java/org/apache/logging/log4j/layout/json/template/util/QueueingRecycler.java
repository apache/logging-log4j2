package org.apache.logging.log4j.layout.json.template.util;

import java.util.Queue;
import java.util.function.Function;
import java.util.function.Supplier;

public class QueueingRecycler<V> implements Recycler<V> {

    private final Supplier<V> supplier;

    private final Function<V, V> cleaner;

    private final Queue<V> queue;

    public QueueingRecycler(
            final Supplier<V> supplier,
            final Function<V, V> cleaner,
            final Queue<V> queue) {
        this.supplier = supplier;
        this.cleaner = cleaner;
        this.queue = queue;
    }

    // Visible for tests.
    Queue<V> getQueue() {
        return queue;
    }

    @Override
    public V acquire() {
        final V value = queue.poll();
        return value == null
                ? supplier.get()
                : cleaner.apply(value);
    }

    @Override
    public void release(final V value) {
        queue.offer(value);
    }

}
