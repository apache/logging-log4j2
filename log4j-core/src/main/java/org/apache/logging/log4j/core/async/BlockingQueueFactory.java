package org.apache.logging.log4j.core.async;

import java.util.concurrent.BlockingQueue;

/**
 * Factory for creating instances of {@link BlockingQueue}.
 *
 * @since 2.7
 */
public interface BlockingQueueFactory<E> {

    /**
     * The {@link org.apache.logging.log4j.core.config.plugins.Plugin#elementType() element type} to use for plugins
     * implementing this interface.
     */
    String ELEMENT_TYPE = "BlockingQueueFactory";

    /**
     * Creates a new BlockingQueue with the specified maximum capacity. Note that not all implementations of
     * BlockingQueue support a bounded capacity in which case the value is ignored.
     *
     * @param capacity maximum size of the queue if supported
     * @return a new BlockingQueue
     */
    BlockingQueue<E> create(int capacity);
}
