package org.apache.logging.log4j;

import org.apache.logging.log4j.message.Message;

/**
 *
 */
public interface LogLevel {

    LogLevel toLevel(String sArg);

    /**
     * Convert the string passed as argument to a level. If the
     * conversion fails, then this method returns the value of
     * <code>defaultLevel</code>.
     */
    LogLevel toLevel(String sArg, Level defaultLevel);


    /**
     * Log a message object with the current level
     *
     * @param message the message object to log.
     */
    void log(Logger logger, String message);

    /**
     * Log a message at the current level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.

     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    void log(Logger logger, String message, Throwable t);

    /**
     * Log a message object with the current level.
     *
     * @param message the message object to log.
     */
    void log(Logger logger, Object message);

    /**
     * Log a message at the current level including the
     * stack trace of the {@link Throwable}<code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    void log(Logger logger, Object message, Throwable t);

    /**
     * Log a message with parameters at the current level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    void log(Logger logger, String message, Object... params);

    /**
     * Log a message with the specific Marker at the current level.
     *
     * @param msg the message string to be logged
     */
    void log(Logger logger, Message msg);

    /**
     * Log a message with the specific Marker at the current level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    void log(Logger logger, Message msg, Throwable t);

    /**
     * Log a message with the specific Marker at the current level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    void log(Logger logger, Marker marker, Message msg);

    /**
     * Log a message with the specific Marker at the current level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    void log(Logger logger, Marker marker, Message msg, Throwable t);
}
