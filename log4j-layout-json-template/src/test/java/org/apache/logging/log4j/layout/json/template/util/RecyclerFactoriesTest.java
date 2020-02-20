package org.apache.logging.log4j.layout.json.template.util;

import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.convert.TypeConverterRegistry;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;

public class RecyclerFactoriesTest {

    @Test
    public void testRecyclerFactoryConverter() throws Exception {

        // Check if the type converter is registered.
        final TypeConverter<?> converter = TypeConverterRegistry
                .getInstance()
                .findCompatibleConverter(RecyclerFactory.class);
        Assertions.assertThat(converter).isNotNull();

        // Check dummy recycler factory.
        {
            final Object actualDummyRecyclerFactory = converter.convert("dummy");
            Assertions
                    .assertThat(actualDummyRecyclerFactory)
                    .isSameAs(DummyRecyclerFactory.getInstance());
        }

        // Check thread-local recycler factory.
        {
            final Object actualThreadLocalRecyclerFactory = converter.convert("threadLocal");
            Assertions
                    .assertThat(actualThreadLocalRecyclerFactory)
                    .isSameAs(ThreadLocalRecyclerFactory.getInstance());
        }

        // Check queueing recycler factory.
        {
            final Object actualQueueingRecyclerFactory = converter.convert("queue");
            Assertions
                    .assertThat(actualQueueingRecyclerFactory)
                    .isInstanceOf(QueueingRecyclerFactory.class);
        }

        // Check queueing recycler factory with supplier.
        {
            final Object recyclerFactory = converter.convert(
                    "queue:supplier=java.util.ArrayDeque.new");
            Assertions
                    .assertThat(recyclerFactory)
                    .isInstanceOf(QueueingRecyclerFactory.class);
            final QueueingRecyclerFactory queueingRecyclerFactory =
                    (QueueingRecyclerFactory) recyclerFactory;
            final Recycler<Object> recycler = queueingRecyclerFactory
                    .create(Object::new, Function.identity());
            Assertions
                    .assertThat(recycler)
                    .isInstanceOf(QueueingRecycler.class);
            final QueueingRecycler<Object> queueingRecycler =
                    (QueueingRecycler<Object>) recycler;
            Assertions
                    .assertThat(queueingRecycler.getQueue())
                    .isInstanceOf(ArrayDeque.class);
        }

        // Check queueing recycler factory with capacity.
        {
            final Object actualQueueingRecyclerFactory = converter.convert(
                    "queue:capacity=100");
            Assertions
                    .assertThat(actualQueueingRecyclerFactory)
                    .isInstanceOf(QueueingRecyclerFactory.class);
        }

        // Check queueing recycler factory with supplier and capacity.
        {
            final Object recyclerFactory = converter.convert(
                    "queue:" +
                            "supplier=java.util.concurrent.ArrayBlockingQueue.new," +
                            "capacity=100");
            Assertions
                    .assertThat(recyclerFactory)
                    .isInstanceOf(QueueingRecyclerFactory.class);
            final QueueingRecyclerFactory queueingRecyclerFactory =
                    (QueueingRecyclerFactory) recyclerFactory;
            final Recycler<Object> recycler = queueingRecyclerFactory
                    .create(Object::new, Function.identity());
            Assertions
                    .assertThat(recycler)
                    .isInstanceOf(QueueingRecycler.class);
            final QueueingRecycler<Object> queueingRecycler =
                    (QueueingRecycler<Object>) recycler;
            Assertions
                    .assertThat(queueingRecycler.getQueue())
                    .isInstanceOf(ArrayBlockingQueue.class);
            final ArrayBlockingQueue<Object> queue =
                    (ArrayBlockingQueue<Object>) queueingRecycler.getQueue();
            Assertions.assertThat(queue.remainingCapacity()).isEqualTo(100);
        }

    }

}
