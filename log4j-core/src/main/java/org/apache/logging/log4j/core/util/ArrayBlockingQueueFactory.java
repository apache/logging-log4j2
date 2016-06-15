package org.apache.logging.log4j.core.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Factory for creating instances of {@link ArrayBlockingQueue}.
 *
 * @since 2.7
 */
public class ArrayBlockingQueueFactory<E> implements BlockingQueueFactory<E> {
    @Override
    public BlockingQueue<E> create(int capacity) {
        return new ArrayBlockingQueue<>(capacity);
    }
}
