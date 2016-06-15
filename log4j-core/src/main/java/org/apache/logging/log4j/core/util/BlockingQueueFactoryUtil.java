package org.apache.logging.log4j.core.util;

import java.lang.reflect.InvocationTargetException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Utility class for obtaining a {@link BlockingQueueFactory}. If the Conversant Disruptor library is available, then
 * {@link DisruptorBlockingQueueFactory} will be used; otherwise, {@link ArrayBlockingQueueFactory} will be used.
 *
 * @since 2.7
 */
public final class BlockingQueueFactoryUtil {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final BlockingQueueFactory<LogEvent> LOG_EVENT_BLOCKING_QUEUE_FACTORY;

    static {
        BlockingQueueFactory<LogEvent> factory = null;
        if (LoaderUtil.isClassAvailable("com.conversantmedia.util.concurrent.DisruptorBlockingQueue")) {
            try {
                factory = newDisruptorBlockingQueueFactory();
            } catch (final ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.warn("Found Conversant Disruptor, but an error is preventing use of it." +
                    " Falling back to default ArrayBlockingQueue implementation.", e);
            }
        }
        if (factory == null) {
            factory = new ArrayBlockingQueueFactory<>();
        }
        LOG_EVENT_BLOCKING_QUEUE_FACTORY = factory;
    }

    @SuppressWarnings("unchecked")
    private static BlockingQueueFactory<LogEvent> newDisruptorBlockingQueueFactory()
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
        InvocationTargetException {
        return LoaderUtil.newCheckedInstanceOf("org.apache.logging.log4j.core.util.DisruptorBlockingQueueFactory",
            BlockingQueueFactory.class);
    }

    /**
     * Returns a suitable BlockingQueueFactory for LogEvents.
     *
     * @return a BlockingQueueFactory instance
     */
    public static BlockingQueueFactory<LogEvent> getLogEventBlockingQueueFactory() {
        return LOG_EVENT_BLOCKING_QUEUE_FACTORY;
    }

    private BlockingQueueFactoryUtil() {
    }
}
