package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.Logger;

/**
 *
 */
public interface LoggerFactory<C extends LoggerContext> {

    Logger newInstance(C ctx, String name);
}
