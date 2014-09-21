package org.apache.logging.log4j.spi;

import java.io.Closeable;

/**
 * A basic registry for {@link LoggerContext} objects and their associated external
 * Logger classes. This registry should not be used for Log4j Loggers; it is instead used for creating bridges to
 * other external log systems.
 *
 * @param <L> the external logger class for this registry (e.g., {@code org.slf4j.Logger})
 * @since 2.1
 */
public interface LoggerAdapter<L> extends Closeable {

    /**
     * Gets a named logger. Implementations should defer to the abstract methods in {@link AbstractLoggerAdapter}.
     *
     * @param name the name of the logger to get
     * @return the named logger
     */
    L getLogger(String name);
}
