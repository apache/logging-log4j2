/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.spi;

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Base implementation of a Logger. It is highly recommended that any Logger implementation extend this class.
 */
public abstract class AbstractLoggerProvider implements LoggerProvider, Serializable {

    private static final long serialVersionUID = 2L;

    /**
     * Marker for flow tracing.
     */
    public static final Marker FLOW_MARKER = MarkerManager.getMarker("FLOW");

    /**
     * Marker for method entry tracing.
     */
    public static final Marker ENTRY_MARKER = MarkerManager.getMarker("ENTRY").add(FLOW_MARKER);

    /**
     * Marker for method exit tracing.
     */
    public static final Marker EXIT_MARKER = MarkerManager.getMarker("EXIT").add(FLOW_MARKER);

    /**
     * Marker for exception tracing.
     */
    public static final Marker EXCEPTION_MARKER = MarkerManager.getMarker("EXCEPTION");

    /**
     * Marker for throwing exceptions.
     */
    public static final Marker THROWING_MARKER = MarkerManager.getMarker("THROWING").add(EXCEPTION_MARKER);

    /**
     * Marker for catching exceptions.
     */
    public static final Marker CATCHING_MARKER = MarkerManager.getMarker("CATCHING").add(EXCEPTION_MARKER);

    /**
     * The default MessageFactory class.
     */
    public static final Class<? extends MessageFactory> DEFAULT_MESSAGE_FACTORY_CLASS = ParameterizedMessageFactory.class;

    private static final String FQCN = AbstractLoggerProvider.class.getName();

    private static final String THROWING = "throwing";

    private static final String CATCHING = "catching";

    /**
     * Checks that the message factory a logger was created with is the same as the given messageFactory. If they are
     * different log a warning to the {@linkplain StatusLogger}. A null MessageFactory translates to the default
     * MessageFactory {@link #DEFAULT_MESSAGE_FACTORY_CLASS}.
     *
     * @param logger The logger to check
     * @param messageFactory The message factory to check.
     */
    public static void checkMessageFactory(final LoggerProvider logger, final MessageFactory messageFactory) {
        final String name = logger.getName();
        final MessageFactory loggerMessageFactory = logger.getMessageFactory();
        if (messageFactory != null && !loggerMessageFactory.equals(messageFactory)) {
            StatusLogger.getLogger().warn(
                    "The Logger {} was created with the message factory {} and is now requested with the "
                            + "message factory {}, which may create log events with unexpected formatting.", name,
                    loggerMessageFactory, messageFactory);
        } else if (messageFactory == null && !loggerMessageFactory.getClass().equals(DEFAULT_MESSAGE_FACTORY_CLASS)) {
            StatusLogger
                    .getLogger()
                    .warn("The Logger {} was created with the message factory {} and is now requested with a null "
                            + "message factory (defaults to {}), which may create log events with unexpected formatting.",
                            name, loggerMessageFactory, DEFAULT_MESSAGE_FACTORY_CLASS.getName());
        }
    }

    private final String name;

    private final MessageFactory messageFactory;

    /**
     * Creates a new logger named after the class (or subclass).
     */
    public AbstractLoggerProvider() {
        this.name = getClass().getName();
        this.messageFactory = createDefaultMessageFactory();
    }

    /**
     * Creates a new named logger.
     *
     * @param name the logger name
     */
    public AbstractLoggerProvider(final String name) {
        this.name = name;
        this.messageFactory = createDefaultMessageFactory();
    }

    /**
     * Creates a new named logger.
     *
     * @param name the logger name
     * @param messageFactory the message factory, if null then use the default message factory.
     */
    public AbstractLoggerProvider(final String name, final MessageFactory messageFactory) {
        this.name = name;
        this.messageFactory = messageFactory == null ? createDefaultMessageFactory() : messageFactory;
    }

    /**
     * Logs a Throwable that has been caught.
     *
     * @param level The logging Level.
     * @param t The Throwable.
     */
    @Override
    public void catching(final Level level, final Throwable t) {
        catching(FQCN, level, t);
    }

    /**
     * Logs a Throwable that has been caught with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param level The logging level.
     * @param t The Throwable.
     */
    protected void catching(final String fqcn, final Level level, final Throwable t) {
        if (isEnabled(level, CATCHING_MARKER, (Object) null, null)) {
            logMessage(fqcn, level, CATCHING_MARKER, catchingMsg(t), t);
        }
    }

    /**
     * Logs a Throwable at the {@link Level#ERROR ERROR} level..
     *
     * @param t The Throwable.
     */
    @Override
    public void catching(final Throwable t) {
        if (isEnabled(Level.ERROR, CATCHING_MARKER, (Object) null, null)) {
            logMessage(FQCN, Level.ERROR, CATCHING_MARKER, catchingMsg(t), t);
        }
    }

    protected Message catchingMsg(final Throwable t) {
        return messageFactory.newMessage(CATCHING);
    }

    private MessageFactory createDefaultMessageFactory() {
        try {
            return DEFAULT_MESSAGE_FACTORY_CLASS.newInstance();
        } catch (final InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Logs a message with the specific Marker at the DEBUG level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    @Override
    public void debug(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.DEBUG, marker, msg, null);
    }

    /**
     * Logs a message with the specific Marker at the DEBUG level.
     *
     * @param marker the marker data specific to this log statement.
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void debug(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, marker, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void debug(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, null);
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void debug(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, t);
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void debug(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void debug(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, params);
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void debug(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, t);
    }

    /**
     * Logs the specified Message at the DEBUG level.
     *
     * @param msg the message to be logged
     */
    @Override
    public void debug(final Message msg) {
        logIfEnabled(FQCN, Level.DEBUG, null, msg, null);
    }

    /**
     * Logs the specified Message at the DEBUG level.
     *
     * @param msg the message to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void debug(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void debug(final Object message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, null);
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void debug(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, t);
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void debug(final String message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void debug(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, params);
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void debug(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, t);
    }

    /**
     * Logs entry to a method.
     */
    @Override
    public void entry() {
        entry(FQCN);
    }

    /**
     * Logs entry to a method.
     *
     * @param params The parameters to the method.
     */
    @Override
    public void entry(final Object... params) {
        entry(FQCN, params);
    }

    /**
     * Logs entry to a method with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param params The parameters to the method.
     */
    protected void entry(final String fqcn, final Object... params) {
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            logIfEnabled(fqcn, Level.TRACE, ENTRY_MARKER, entryMsg(params.length, params), null);
        }
    }

    protected Message entryMsg(final int count, final Object... params) {
        if (count == 0) {
            return messageFactory.newMessage("entry");
        }
        final StringBuilder sb = new StringBuilder("entry params(");
        int i = 0;
        for (final Object parm : params) {
            if (parm != null) {
                sb.append(parm.toString());
            } else {
                sb.append("null");
            }
            if (++i < params.length) {
                sb.append(", ");
            }
        }
        sb.append(')');
        return messageFactory.newMessage(sb.toString());
    }

    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    @Override
    public void error(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.ERROR, marker, msg, null);
    }

    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void error(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void error(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, null);
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void error(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, t);
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void error(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void error(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, params);
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void error(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, t);
    }

    /**
     * Logs the specified Message at the {@link Level#ERROR ERROR} level.
     *
     * @param msg the message to be logged
     */
    @Override
    public void error(final Message msg) {
        logIfEnabled(FQCN, Level.ERROR, null, msg, null);
    }

    /**
     * Logs the specified Message at the {@link Level#ERROR ERROR} level.
     *
     * @param msg the message to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void error(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void error(final Object message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, null);
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void error(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, message, t);
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void error(final String message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#ERROR ERROR} level.
     *
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void error(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.ERROR, null, message, params);
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void error(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, message, t);
    }

    /**
     * Logs exit from a method.
     */
    @Override
    public void exit() {
        exit(FQCN, null);
    }

    /**
     * Logs exiting from a method with the result.
     *
     * @param <R> The type of the parameter and object being returned.
     * @param result The result being returned from the method call.
     * @return the Throwable.
     */
    @Override
    public <R> R exit(final R result) {
        return exit(FQCN, result);
    }

    /**
     * Logs exiting from a method with the result and location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param <R> The type of the parameter and object being returned.
     * @param result The result being returned from the method call.
     */
    protected <R> R exit(final String fqcn, final R result) {
        if (isEnabled(Level.TRACE, EXIT_MARKER, (Object) null, null)) {
            logIfEnabled(fqcn, Level.TRACE, EXIT_MARKER, exitMsg(result), null);
        }
        return result;
    }

    protected Message exitMsg(final Object result) {
        if (result == null) {
            return messageFactory.newMessage("exit");
        }
        return messageFactory.newMessage("exit with(" + result + ')');
    }

    /**
     * Logs a message with the specific Marker at the FATAL level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    @Override
    public void fatal(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.FATAL, marker, msg, null);
    }

    /**
     * Logs a message with the specific Marker at the FATAL level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void fatal(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void fatal(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, null);
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void fatal(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, t);
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void fatal(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void fatal(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, params);
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void fatal(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, t);
    }

    /**
     * Logs the specified Message at the FATAL level.
     *
     * @param msg the message to be logged
     */
    @Override
    public void fatal(final Message msg) {
        logIfEnabled(FQCN, Level.FATAL, null, msg, null);
    }

    /**
     * Logs the specified Message at the FATAL level.
     *
     * @param msg the message to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void fatal(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void fatal(final Object message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, null);
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void fatal(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, message, t);
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void fatal(final String message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#FATAL FATAL} level.
     *
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void fatal(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.FATAL, null, message, params);
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void fatal(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, message, t);
    }

    /**
     * Gets the message factory.
     *
     * @return the message factory.
     */
    @Override
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.Logger#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Logs a message with the specific Marker at the INFO level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    @Override
    public void info(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.INFO, marker, msg, null);
    }

    /**
     * Logs a message with the specific Marker at the INFO level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void info(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void info(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, null);
    }

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void info(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, message, t);
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void info(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void info(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.INFO, marker, message, params);
    }

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void info(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, message, t);
    }

    /**
     * Logs the specified Message at the INFO level.
     *
     * @param msg the message to be logged
     */
    @Override
    public void info(final Message msg) {
        logIfEnabled(FQCN, Level.INFO, null, msg, null);
    }

    /**
     * Logs the specified Message at the INFO level.
     *
     * @param msg the message to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void info(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void info(final Object message) {
        logIfEnabled(FQCN, Level.INFO, null, message, null);
    }

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void info(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, message, t);
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void info(final String message) {
        logIfEnabled(FQCN, Level.INFO, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#INFO INFO} level.
     *
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void info(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.INFO, null, message, params);
    }

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void info(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, message, t);
    }

    /**
     * Checks whether this Logger is enabled for the DEBUG Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level DEBUG, {@code false} otherwise.
     */
    @Override
    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG, null, null);
    }

    /**
     * Checks whether this Logger is enabled for the DEBUG Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level DEBUG, {@code false} otherwise.
     */
    @Override
    public boolean isDebugEnabled(final Marker marker) {
        return isEnabled(Level.DEBUG, marker, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the the given Level.
     * <p>
     * Note that passing in {@link Level#OFF OFF} always returns {@code true}.
     * </p>
     *
     * @param level the level to check
     * @return boolean - {@code true} if this Logger is enabled for level, {@code false} otherwise.
     */
    @Override
    public boolean isEnabled(final Level level) {
        return isEnabled(level, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the the given Level.
     * <p>
     * Note that passing in {@link Level#OFF OFF} always returns {@code true}.
     * </p>
     *
     * @param level the level to check
     * @param marker A Marker or null.
     * @return boolean - {@code true} if this Logger is enabled for level, {@code false} otherwise.
     */
    @Override
    public boolean isEnabled(final Level level, final Marker marker) {
        return isEnabled(level, marker, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the {@link Level#ERROR ERROR} Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#ERROR ERROR}, {@code false}
     *         otherwise.
     */
    @Override
    public boolean isErrorEnabled() {
        return isEnabled(Level.ERROR, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the {@link Level#ERROR ERROR} Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level {@link Level#ERROR ERROR}, {@code false}
     *         otherwise.
     */
    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return isEnabled(Level.ERROR, marker, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the FATAL Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level FATAL, {@code false} otherwise.
     */
    @Override
    public boolean isFatalEnabled() {
        return isEnabled(Level.FATAL, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the FATAL Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level FATAL, {@code false} otherwise.
     */
    @Override
    public boolean isFatalEnabled(final Marker marker) {
        return isEnabled(Level.FATAL, marker, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the INFO Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level INFO, {@code false} otherwise.
     */
    @Override
    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the INFO Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level INFO, {@code false} otherwise.
     */
    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return isEnabled(Level.INFO, marker, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the TRACE Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level TRACE, {@code false} otherwise.
     */
    @Override
    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the TRACE Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level TRACE, {@code false} otherwise.
     */
    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return isEnabled(Level.TRACE, marker, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the WARN Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level WARN, {@code false} otherwise.
     */
    @Override
    public boolean isWarnEnabled() {
        return isEnabled(Level.WARN, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the WARN Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level WARN, {@code false} otherwise.
     */
    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return isEnabled(Level.WARN, marker, (Object) null, null);
    }

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    @Override
    public void log(final Level level, final Marker marker, final Message msg) {
        logIfEnabled(FQCN, level, marker, msg, (Throwable) null);
    }

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement.
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void log(final Level level, final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, level, marker, msg, t);
    }

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void log(final Level level, final Marker marker, final Object message) {
        logIfEnabled(FQCN, level, marker, message, (Throwable) null);
    }

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void log(final Level level, final Marker marker, final Object message, final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            logMessage(FQCN, level, marker, message, t);
        }
    }

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void log(final Level level, final Marker marker, final String message) {
        logIfEnabled(FQCN, level, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the given level.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void log(final Level level, final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, level, marker, message, params);
    }

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void log(final Level level, final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, level, marker, message, t);
    }

    /**
     * Logs the specified Message at the given level.
     *
     * @param level the logging level
     * @param msg the message to be logged
     */
    @Override
    public void log(final Level level, final Message msg) {
        logIfEnabled(FQCN, level, null, msg, null);
    }

    /**
     * Logs the specified Message at the given level.
     *
     * @param level the logging level
     * @param msg the message to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void log(final Level level, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, level, null, msg, t);
    }

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param message the message object to log.
     */
    @Override
    public void log(final Level level, final Object message) {
        logIfEnabled(FQCN, level, null, message, null);
    }

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable} <code>t</code> passed as
     * parameter.
     *
     * @param level the logging level
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void log(final Level level, final Object message, final Throwable t) {
        logIfEnabled(FQCN, level, null, message, t);
    }

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param message the message object to log.
     */
    @Override
    public void log(final Level level, final String message) {
        logIfEnabled(FQCN, level, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the given level.
     *
     * @param level the logging level
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void log(final Level level, final String message, final Object... params) {
        logIfEnabled(FQCN, level, null, message, params);
    }

    /**
     * Logs a message at the given level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param level the logging level
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void log(final Level level, final String message, final Throwable t) {
        logIfEnabled(FQCN, level, null, message, t);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.spi.LoggerProvider#logIfEnabled(java.lang.String, org.apache.logging.log4j.Level,
     * org.apache.logging.log4j.Marker, org.apache.logging.log4j.message.Message, java.lang.Throwable)
     */
    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final Message msg,
            final Throwable t) {
        if (isEnabled(level, marker, msg, t)) {
            logMessage(fqcn, level, marker, msg, t);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.spi.LoggerProvider#logIfEnabled(java.lang.String, org.apache.logging.log4j.Level,
     * org.apache.logging.log4j.Marker, java.lang.Object, java.lang.Throwable)
     */
    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final Object message,
            final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            logMessage(fqcn, level, marker, message, t);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.spi.LoggerProvider#logIfEnabled(java.lang.String, org.apache.logging.log4j.Level,
     * org.apache.logging.log4j.Marker, java.lang.String)
     */
    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message) {
        if (isEnabled(level, marker, message)) {
            logMessage(fqcn, level, marker, message);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.spi.LoggerProvider#logIfEnabled(java.lang.String, org.apache.logging.log4j.Level,
     * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Object[])
     */
    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object... params) {
        if (isEnabled(level, marker, message, params)) {
            logMessage(fqcn, level, marker, message, params);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.spi.LoggerProvider#logIfEnabled(java.lang.String, org.apache.logging.log4j.Level,
     * org.apache.logging.log4j.Marker, java.lang.String, java.lang.Throwable)
     */
    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            logMessage(fqcn, level, marker, message, t);
        }
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final Object message,
            final Throwable t) {
        logMessage(fqcn, level, marker, messageFactory.newMessage(message), t);
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Throwable t) {
        logMessage(fqcn, level, marker, messageFactory.newMessage(message), t);
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message) {
        final Message msg = messageFactory.newMessage(message);
        logMessage(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object... params) {
        final Message msg = messageFactory.newMessage(message, params);
        logMessage(fqcn, level, marker, msg, msg.getThrowable());
    }

    /**
     * Logs a formatted message using the specified format string and arguments.
     *
     * @param level The logging Level.
     * @param marker the marker data specific to this log statement.
     * @param format The format String.
     * @param params Arguments specified by the format.
     */
    @Override
    public void printf(Level level, Marker marker, String format, Object... params) {
        if (isEnabled(level, marker, format, params)) {
            Message msg = new StringFormattedMessage(format, params);
            logMessage(FQCN, level, marker, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a formatted message using the specified format string and arguments.
     *
     * @param level The logging Level.
     * @param format The format String.
     * @param params Arguments specified by the format.
     */
    @Override
    public void printf(Level level, String format, Object... params) {
        if (isEnabled(level, null, format, params)) {
            Message msg = new StringFormattedMessage(format, params);
            logMessage(FQCN, level, null, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a Throwable to be thrown.
     *
     * @param <T> the type of the Throwable.
     * @param t The Throwable.
     * @return the Throwable.
     */
    @Override
    public <T extends Throwable> T throwing(final T t) {
        return throwing(FQCN, Level.ERROR, t);
    }

    /**
     * Logs a Throwable to be thrown.
     *
     * @param <T> the type of the Throwable.
     * @param level The logging Level.
     * @param t The Throwable.
     * @return the Throwable.
     */
    @Override
    public <T extends Throwable> T throwing(final Level level, final T t) {
        return throwing(FQCN, level, t);
    }

    /**
     * Logs a Throwable to be thrown.
     *
     * @param <T> the type of the Throwable.
     * @param level The logging Level.
     * @param t The Throwable.
     * @return the Throwable.
     */
    protected <T extends Throwable> T throwing(final String fqcn, final Level level, final T t) {
        if (isEnabled(level, THROWING_MARKER, (Object) null, null)) {
            logMessage(fqcn, level, THROWING_MARKER, throwingMsg(t), t);
        }
        return t;
    }

    protected Message throwingMsg(final Throwable t) {
        return messageFactory.newMessage(THROWING);
    }

    /**
     * Logs a message with the specific Marker at the TRACE level.
     *
     * @param marker the marker data specific to this log statement.
     * @param msg the message string to be logged
     */
    @Override
    public void trace(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.TRACE, marker, msg, null);
    }

    /**
     * Logs a message with the specific Marker at the TRACE level.
     *
     * @param marker the marker data specific to this log statement.
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void trace(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void trace(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, null);
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void trace(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, t);
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void trace(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void trace(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, params);
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void trace(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, t);
    }

    /**
     * Logs the specified Message at the TRACE level.
     *
     * @param msg the message to be logged
     */
    @Override
    public void trace(final Message msg) {
        logIfEnabled(FQCN, Level.TRACE, null, msg, null);
    }

    /**
     * Logs the specified Message at the TRACE level.
     *
     * @param msg the message to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void trace(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void trace(final Object message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, null);
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void trace(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, message, t);
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void trace(final String message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#TRACE TRACE} level.
     *
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void trace(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.TRACE, null, message, params);
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void trace(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, message, t);
    }

    /**
     * Logs a message with the specific Marker at the WARN level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     */
    @Override
    public void warn(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.WARN, marker, msg, null);
    }

    /**
     * Logs a message with the specific Marker at the WARN level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void warn(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void warn(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, null);
    }

    /*
     * Instead of one single method with Object... declared the following methods explicitly specify parameters because
     * they perform dramatically better than having the JVM convert them to an array.
     */

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void warn(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, message, t);
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void warn(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void warn(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.WARN, marker, message, params);
    }

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void warn(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, message, t);
    }

    /**
     * Logs the specified Message at the WARN level.
     *
     * @param msg the message to be logged
     */
    @Override
    public void warn(final Message msg) {
        logIfEnabled(FQCN, Level.WARN, null, msg, null);
    }

    /**
     * Logs the specified Message at the WARN level.
     *
     * @param msg the message to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void warn(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, msg, t);
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void warn(final Object message) {
        logIfEnabled(FQCN, Level.WARN, null, message, null);
    }

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void warn(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, message, t);
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void warn(final String message) {
        logIfEnabled(FQCN, Level.WARN, null, message, (Throwable) null);
    }

    /**
     * Logs a message with parameters at the {@link Level#WARN WARN} level.
     *
     * @param message the message to log.
     * @param params parameters to the message.
     */
    @Override
    public void warn(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.WARN, null, message, params);
    }

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the stack trace of the {@link Throwable}
     * <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void warn(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, message, t);
    }

}
