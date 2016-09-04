package org.apache.logging.log4j.core.async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Factory for creating instances of {@link ArrayBlockingQueue}.
 *
 * @since 2.7
 */
@Plugin(name = "ArrayBlockingQueue", category = Node.CATEGORY, elementType = BlockingQueueFactory.ELEMENT_TYPE)
public class ArrayBlockingQueueFactory<E> implements BlockingQueueFactory<E> {
    @Override
    public BlockingQueue<E> create(int capacity) {
        return new ArrayBlockingQueue<>(capacity);
    }

    @PluginFactory
    public static <E> ArrayBlockingQueueFactory<E> createFactory() {
        return new ArrayBlockingQueueFactory<>();
    }
}
