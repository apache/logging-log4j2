package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Logger;

/**
 *
 */
public interface LoggerContext {

    Logger getLogger(String name);

    boolean hasLogger(String name);
}
