package org.apache.logging.log4j.core;

/**
 *
 */
public interface LoggerFactory {

    Logger newInstance(LoggerContext ctx, String name);
}
