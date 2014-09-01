package org.apache.logging.log4j.spi;

import java.util.concurrent.ConcurrentMap;

/**
 * A basic registry for {@link LoggerContext} objects and their associated external
 * Logger classes. This registry should not be used for Log4j Loggers; it is instead used for creating bridges to
 * other external log systems.
 *
 * @param <L> the external logger class for this registry (e.g., {@code org.slf4j.Logger})
 * @since 2.1
 */
public interface ExternalLoggerContextRegistry<L> {

    /**
     * Gets a named logger linked to the {@link LoggerContext} returned by {@link #getContext()}. If no logger of
     * the given name exists, then a new logger will be created using {@link #newLogger(String, LoggerContext)}.
     *
     * @param name the name of the logger to get
     * @return the named logger
     */
    L getLogger(String name);

    /**
     * Creates a new named logger for a given {@link LoggerContext}.
     *
     * @param name    the name of the logger to create
     * @param context the LoggerContext this logger will be associated with
     * @return the new named logger
     */
    L newLogger(String name, LoggerContext context);

    /**
     * Gets the {@link LoggerContext} that should be used to look up or create loggers.
     *
     * @return the LoggerContext to be used for lookup and creation purposes
     */
    LoggerContext getContext();

    /**
     * Gets or creates the ConcurrentMap of named loggers for a given LoggerContext.
     *
     * @param context the LoggerContext to get loggers for
     * @return the map of loggers for the given LoggerContext
     */
    ConcurrentMap<String, L> getLoggersInContext(LoggerContext context);

    /**
     * Shuts down this registry. Implementations should clear out any instance data and perform any relevant clean-up.
     */
    void stop();
}
