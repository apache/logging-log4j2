package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;

/**
 *
 */
public interface LogEventFactory {

    LogEvent createEvent(String loggerName, Marker marker, String fqcn, Level level, Message data, Throwable t);
}
