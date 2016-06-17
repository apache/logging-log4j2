package org.apache.logging.log4j.core.async;

import java.util.concurrent.BlockingQueue;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;

/**
 * Factory for creating instances of {@link DisruptorBlockingQueue}.
 *
 * @since 2.7
 */
public class DisruptorBlockingQueueFactory<E> implements BlockingQueueFactory<E> {
    @Override
    public BlockingQueue<E> create(int capacity) {
        return new DisruptorBlockingQueue<E>(capacity);
    }
}
