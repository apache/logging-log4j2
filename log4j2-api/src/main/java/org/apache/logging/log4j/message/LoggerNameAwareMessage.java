package org.apache.logging.log4j.message;

import org.apache.logging.log4j.Logger;

/**
 *  Message that is interested in the name of the Logger.
 */
public interface LoggerNameAwareMessage
{
    void setLoggerName(String name);

    String getLoggerName();
}
