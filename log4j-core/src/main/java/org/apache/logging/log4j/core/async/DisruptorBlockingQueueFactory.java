package org.apache.logging.log4j.core.async;

import java.util.concurrent.BlockingQueue;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import com.conversantmedia.util.concurrent.SpinPolicy;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Factory for creating instances of {@link DisruptorBlockingQueue}.
 *
 * @since 2.7
 */
@Plugin(name = "DisruptorBlockingQueue", category = Node.CATEGORY, elementType = BlockingQueueFactory.ELEMENT_TYPE)
public class DisruptorBlockingQueueFactory<E> implements BlockingQueueFactory<E> {

    private final SpinPolicy spinPolicy;

    private DisruptorBlockingQueueFactory(final SpinPolicy spinPolicy) {
        this.spinPolicy = spinPolicy;
    }

    @Override
    public BlockingQueue<E> create(int capacity) {
        return new DisruptorBlockingQueue<>(capacity, spinPolicy);
    }

    @PluginFactory
    public static <E> DisruptorBlockingQueueFactory<E> createFactory(
        @PluginAttribute(value = "SpinPolicy", defaultString = "WAITING") final SpinPolicy spinPolicy
    ) {
        return new DisruptorBlockingQueueFactory<>(spinPolicy);
    }
}
