package org.apache.logging.log4j.spi;

/**
 *
 */
public interface LoggerContextFactory {

    /**
     * @param FQCN The fully qualified class name of the caller.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @return The LoggerContext.
     */
    LoggerContext getContext(String FQCN, boolean currentContext);
}
