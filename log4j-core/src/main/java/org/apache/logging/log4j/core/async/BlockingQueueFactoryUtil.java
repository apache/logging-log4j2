package org.apache.logging.log4j.core.async;

import java.lang.reflect.InvocationTargetException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Utility class for obtaining a {@link BlockingQueueFactory}. By default, {@link ArrayBlockingQueueFactory} is used,
 * but this can be overridden by the system property {@code log4j.BlockingQueueFactory}.
 *
 * @since 2.7
 */
public final class BlockingQueueFactoryUtil {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Returns a new BlockingQueueFactory.
     *
     * @return a BlockingQueueFactory instance
     */
    public static <E> BlockingQueueFactory<E> getBlockingQueueFactory() {
        BlockingQueueFactory<E> factory = null;
        try {
            factory = newBlockingQueueFactory();
        } catch (final ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Specified log4j.BlockingQueueFactory could not be instantiated.", e);
        }
        if (factory == null) {
            factory = new ArrayBlockingQueueFactory<>();
        }
        return factory;
    }

    @SuppressWarnings("unchecked")
    private static <E> BlockingQueueFactory<E> newBlockingQueueFactory()
        throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
        InvocationTargetException {
        return LoaderUtil.newCheckedInstanceOfProperty(BlockingQueueFactory.PROPERTY, BlockingQueueFactory.class);
    }

    private BlockingQueueFactoryUtil() {
    }
}
