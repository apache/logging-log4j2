package org.apache.logging.log4j.core.util;

import java.util.concurrent.BlockingQueue;

/**
 * Factory for creating instances of {@link BlockingQueue}.
 *
 * @since 2.7
 */
public interface BlockingQueueFactory<E> {
    BlockingQueue<E> create(int capacity);
}
