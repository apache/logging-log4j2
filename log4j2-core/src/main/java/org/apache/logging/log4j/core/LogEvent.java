package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.Message;

import java.util.Map;
import java.util.Stack;

/**
 *
 */
public interface LogEvent {

     /**
     * Get level.
     * @return level.
     */
    Level getLevel();

    /**
     * Get logger name.
     * @return logger name, may be null.
     */
    String getLoggerName();

    /**
     * Get source of logging request.
     * @return source of logging request, may be null.
     */
    StackTraceElement getSource();

    /**
     * Get the message associated with the event.
     *
     * @return message.
     */
    Message getMessage();

    /**
     * Get thread name.
     * @return thread name, may be null.
     */
    String getThreadName();


    /**
     * Get event time in milliseconds since 1970.
     * @return milliseconds since 1970.
     */
    long getMillis();


    /**
     * Get throwable associated with logging request.
     * @return throwable, may be null.
     */
    Throwable getThrown();


    /**
     * Get the MDC data;
     *
     * @return A copy of the Mapped Diagnostic Context or null.
     */
    Map<String, Object> getContextMap();

    /**
     * Get the NDC data;
     *
     * @return A copy of the Nested Diagnostic Context of null;
     */
    Stack<Object> getContextStack();

}
