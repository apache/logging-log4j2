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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Base implementation of a Logger. It is highly recommended that any Logger implementation extend this class.
 *
 */
public abstract class AbstractLogger implements Logger {

    /**
     * Marker for flow tracing.
     */
    public static final Marker FLOW_MARKER = MarkerManager.getMarker("FLOW");

    /**
     * Marker for method entry tracing.
     */
    public static final Marker ENTRY_MARKER = MarkerManager.getMarker("ENTRY", FLOW_MARKER);

    /**
     * Marker for method exit tracing.
     */
    public static final Marker EXIT_MARKER = MarkerManager.getMarker("EXIT", FLOW_MARKER);


    /**
     * Marker for exception tracing.
     */
    public static final Marker EXCEPTION_MARKER = MarkerManager.getMarker("EXCEPTION");

    /**
     * Marker for throwing exceptions.
     */
    public static final Marker THROWING_MARKER = MarkerManager.getMarker("THROWING", EXCEPTION_MARKER);

    /**
     * Marker for catching exceptions.
     */
    public static final Marker CATCHING_MARKER = MarkerManager.getMarker("CATCHING", EXCEPTION_MARKER);

    /**
     * The default MessageFactory class.
     */
    public static final Class<? extends MessageFactory> DEFAULT_MESSAGE_FACTORY_CLASS =
        ParameterizedMessageFactory.class;

    private static final String FQCN = AbstractLogger.class.getName();

    private static final String THROWING = "throwing";

    private static final String CATCHING = "catching";

    private final String name;

    private final MessageFactory messageFactory;

    /**
     * Creates a new logger named after the class (or subclass).
     */
    public AbstractLogger() {
        this.name = getClass().getName();
        this.messageFactory = createDefaultMessageFactory();
    }

    /**
     * Creates a new named logger.
     *
     * @param name the logger name
     */
    public AbstractLogger(final String name) {
        this.name = name;
        this.messageFactory = createDefaultMessageFactory();
    }

    /**
     * Creates a new named logger.
     *
     * @param name the logger name
     * @param messageFactory the message factory, if null then use the default message factory.
     */
    public AbstractLogger(final String name, final MessageFactory messageFactory) {
        this.name = name;
        this.messageFactory = messageFactory == null ? createDefaultMessageFactory() : messageFactory;
    }

    /**
     * Checks that the message factory a logger was created with is the same as the given messageFactory. If they are
     * different log a warning to the {@linkplain StatusLogger}. A null MessageFactory translates to the default
     * MessageFactory {@link #DEFAULT_MESSAGE_FACTORY_CLASS}.
     *
     * @param logger
     *            The logger to check
     * @param messageFactory
     *            The message factory to check.
     */
    public static void checkMessageFactory(final Logger logger, final MessageFactory messageFactory) {
        final String name = logger.getName();
        final MessageFactory loggerMessageFactory = logger.getMessageFactory();
        if (messageFactory != null && !loggerMessageFactory.equals(messageFactory)) {
            StatusLogger
                .getLogger()
                .warn("The Logger {} was created with the message factory {} and is now requested with the " +
                    "message factory {}, which may create log events with unexpected formatting.",
                    name, loggerMessageFactory, messageFactory);
        } else if (messageFactory == null
            && !loggerMessageFactory.getClass().equals(DEFAULT_MESSAGE_FACTORY_CLASS)) {
            StatusLogger
                .getLogger()
                .warn("The Logger {} was created with the message factory {} and is now requested with a null " +
                    "message factory (defaults to {}), which may create log events with unexpected formatting.",
                    name, loggerMessageFactory, DEFAULT_MESSAGE_FACTORY_CLASS.getName());
        }
    }

    /**
     * Logs a Throwable that has been caught.
     *
     * @param level The logging Level.
     * @param t     The Throwable.
     */
    @Override
    public void catching(final Level level, final Throwable t) {
        catching(FQCN, level, t);
    }

    /**
     * Logs a Throwable at the {@link Level#ERROR ERROR} level..
     *
     * @param t The Throwable.
     */
    @Override
    public void catching(final Throwable t) {
        catching(FQCN, Level.ERROR, t);
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
            log(CATCHING_MARKER, fqcn, level, messageFactory.newMessage(CATCHING), t);
        }
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
     * @param msg    the message string to be logged
     */
    @Override
    public void debug(final Marker marker, final Message msg) {
        if (isEnabled(Level.DEBUG, marker, msg, null)) {
            log(marker, FQCN, Level.DEBUG, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the DEBUG level.
     *
     * @param marker the marker data specific to this log statement.
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    @Override
    public void debug(final Marker marker, final Message msg, final Throwable t) {
        if (isEnabled(Level.DEBUG, marker, msg, t)) {
            log(marker, FQCN, Level.DEBUG, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void debug(final Marker marker, final Object message) {
        if (isEnabled(Level.DEBUG, marker, message, null)) {
            log(marker, FQCN, Level.DEBUG, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void debug(final Marker marker, final Object message, final Throwable t) {
        if (isEnabled(Level.DEBUG, marker, message, t)) {
            log(marker, FQCN, Level.DEBUG, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void debug(final Marker marker, final String message) {
        if (isEnabled(Level.DEBUG, marker, message)) {
            log(marker, FQCN, Level.DEBUG, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void debug(final Marker marker, final String message, final Object... params) {
        if (isEnabled(Level.DEBUG, marker, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(marker, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void debug(final Marker marker, final String message, final Throwable t) {
        if (isEnabled(Level.DEBUG, marker, message, t)) {
            log(marker, FQCN, Level.DEBUG, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message with the specific Marker at the DEBUG level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void debug(final Message msg) {
        if (isEnabled(Level.DEBUG, null, msg, null)) {
            log(null, FQCN, Level.DEBUG, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the DEBUG level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    @Override
    public void debug(final Message msg, final Throwable t) {
        if (isEnabled(Level.DEBUG, null, msg, t)) {
            log(null, FQCN, Level.DEBUG, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void debug(final Object message) {
        if (isEnabled(Level.DEBUG, null, message, null)) {
            log(null, FQCN, Level.DEBUG, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void debug(final Object message, final Throwable t) {
        if (isEnabled(Level.DEBUG, null, message, t)) {
            log(null, FQCN, Level.DEBUG, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void debug(final String message) {
        if (isEnabled(Level.DEBUG, null, message)) {
            log(null, FQCN, Level.DEBUG, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void debug(final String message, final Object... params) {
        if (isEnabled(Level.DEBUG, null, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(null, FQCN, Level.DEBUG, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void debug(final String message, final Throwable t) {
        if (isEnabled(Level.DEBUG, null, message, t)) {
            log(null, FQCN, Level.DEBUG, messageFactory.newMessage(message), t);
        }
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
            log(ENTRY_MARKER, fqcn, Level.TRACE, entryMsg(params.length, params), null);
        }
    }

    private Message entryMsg(final int count, final Object... params) {
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
        sb.append(")");
        return messageFactory.newMessage(sb.toString());
    }


    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    @Override
    public void error(final Marker marker, final Message msg) {
        if (isEnabled(Level.ERROR, marker, msg, null)) {
            log(marker, FQCN, Level.ERROR, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    @Override
    public void error(final Marker marker, final Message msg, final Throwable t) {
        if (isEnabled(Level.ERROR, marker, msg, t)) {
            log(marker, FQCN, Level.ERROR, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void error(final Marker marker, final Object message) {
        if (isEnabled(Level.ERROR, marker, message, null)) {
            log(marker, FQCN, Level.ERROR, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void error(final Marker marker, final Object message, final Throwable t) {
        if (isEnabled(Level.ERROR, marker, message, t)) {
            log(marker, FQCN, Level.ERROR, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void error(final Marker marker, final String message) {
        if (isEnabled(Level.ERROR, marker, message)) {
            log(marker, FQCN, Level.ERROR, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#ERROR ERROR} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void error(final Marker marker, final String message, final Object... params) {
        if (isEnabled(Level.ERROR, marker, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(marker, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void error(final Marker marker, final String message, final Throwable t) {
        if (isEnabled(Level.ERROR, marker, message, t)) {
            log(marker, FQCN, Level.ERROR, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void error(final Message msg) {
        if (isEnabled(Level.ERROR, null, msg, null)) {
            log(null, FQCN, Level.ERROR, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the {@link Level#ERROR ERROR} level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    @Override
    public void error(final Message msg, final Throwable t) {
        if (isEnabled(Level.ERROR, null, msg, t)) {
            log(null, FQCN, Level.ERROR, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void error(final Object message) {
        if (isEnabled(Level.ERROR, null, message, null)) {
            log(null, FQCN, Level.ERROR, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void error(final Object message, final Throwable t) {
        if (isEnabled(Level.ERROR, null, message, t)) {
            log(null, FQCN, Level.ERROR, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#ERROR ERROR} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void error(final String message) {
        if (isEnabled(Level.ERROR, null, message)) {
            log(null, FQCN, Level.ERROR, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#ERROR ERROR} level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void error(final String message, final Object... params) {
        if (isEnabled(Level.ERROR, null, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(null, FQCN, Level.ERROR, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void error(final String message, final Throwable t) {
        if (isEnabled(Level.ERROR, null, message, t)) {
            log(null, FQCN, Level.ERROR, messageFactory.newMessage(message), t);
        }
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
            log(EXIT_MARKER, fqcn, Level.TRACE, toExitMsg(result), null);
        }
        return result;
    }

    /**
     * Logs a message with the specific Marker at the FATAL level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    @Override
    public void fatal(final Marker marker, final Message msg) {
        if (isEnabled(Level.FATAL, marker, msg, null)) {
            log(marker, FQCN, Level.FATAL, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the FATAL level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    @Override
    public void fatal(final Marker marker, final Message msg, final Throwable t) {
        if (isEnabled(Level.FATAL, marker, msg, t)) {
            log(marker, FQCN, Level.FATAL, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void fatal(final Marker marker, final Object message) {
        if (isEnabled(Level.FATAL, marker, message, null)) {
            log(marker, FQCN, Level.FATAL, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void fatal(final Marker marker, final Object message, final Throwable t) {
        if (isEnabled(Level.FATAL, marker, message, t)) {
            log(marker, FQCN, Level.FATAL, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void fatal(final Marker marker, final String message) {
        if (isEnabled(Level.FATAL, marker, message)) {
            log(marker, FQCN, Level.FATAL, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#FATAL FATAL} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void fatal(final Marker marker, final String message, final Object... params) {
        if (isEnabled(Level.FATAL, marker, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(marker, FQCN, Level.FATAL, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void fatal(final Marker marker, final String message, final Throwable t) {
        if (isEnabled(Level.FATAL, marker, message, t)) {
            log(marker, FQCN, Level.FATAL, messageFactory.newMessage(message), t);
        }
    }
    /**
     * Logs a message with the specific Marker at the FATAL level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void fatal(final Message msg) {
        if (isEnabled(Level.FATAL, null, msg, null)) {
            log(null, FQCN, Level.FATAL, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the FATAL level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    @Override
    public void fatal(final Message msg, final Throwable t) {
        if (isEnabled(Level.FATAL, null, msg, t)) {
            log(null, FQCN, Level.FATAL, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void fatal(final Object message) {
        if (isEnabled(Level.FATAL, null, message, null)) {
            log(null, FQCN, Level.FATAL, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void fatal(final Object message, final Throwable t) {
        if (isEnabled(Level.FATAL, null, message, t)) {
            log(null, FQCN, Level.FATAL, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#FATAL FATAL} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void fatal(final String message) {
        if (isEnabled(Level.FATAL, null, message)) {
            log(null, FQCN, Level.FATAL, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#FATAL FATAL} level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void fatal(final String message, final Object... params) {
        if (isEnabled(Level.FATAL, null, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(null, FQCN, Level.FATAL, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void fatal(final String message, final Throwable t) {
        if (isEnabled(Level.FATAL, null, message, t)) {
            log(null, FQCN, Level.FATAL, messageFactory.newMessage(message), t);
        }
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

    /* (non-Javadoc)
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
     * @param msg    the message string to be logged
     */
    @Override
    public void info(final Marker marker, final Message msg) {
        if (isEnabled(Level.INFO, marker, msg, null)) {
            log(marker, FQCN, Level.INFO, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the INFO level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    @Override
    public void info(final Marker marker, final Message msg, final Throwable t) {
        if (isEnabled(Level.INFO, marker, msg, t)) {
            log(marker, FQCN, Level.INFO, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void info(final Marker marker, final Object message) {
        if (isEnabled(Level.INFO, marker, message, null)) {
            log(marker, FQCN, Level.INFO, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void info(final Marker marker, final Object message, final Throwable t) {
        if (isEnabled(Level.INFO, marker, message, t)) {
            log(marker, FQCN, Level.INFO, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void info(final Marker marker, final String message) {
        if (isEnabled(Level.INFO, marker, message)) {
            log(marker, FQCN, Level.INFO, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#INFO INFO} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void info(final Marker marker, final String message, final Object... params) {
        if (isEnabled(Level.INFO, marker, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(marker, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void info(final Marker marker, final String message, final Throwable t) {
        if (isEnabled(Level.INFO, marker, message, t)) {
            log(marker, FQCN, Level.INFO, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message with the specific Marker at the TRACE level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void info(final Message msg) {
        if (isEnabled(Level.INFO, null, msg, null)) {
            log(null, FQCN, Level.INFO, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the INFO level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    @Override
    public void info(final Message msg, final Throwable t) {
        if (isEnabled(Level.INFO, null, msg, t)) {
            log(null, FQCN, Level.INFO, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void info(final Object message) {
        if (isEnabled(Level.INFO, null, message, null)) {
            log(null, FQCN, Level.INFO, messageFactory.newMessage(message), null);
        }
    }


    /**
     * Logs a message at the {@link Level#INFO INFO} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void info(final Object message, final Throwable t) {
        if (isEnabled(Level.INFO, null, message, t)) {
            log(null, FQCN, Level.INFO, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#INFO INFO} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void info(final String message) {
        if (isEnabled(Level.INFO, null, message)) {
            log(null, FQCN, Level.INFO, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#INFO INFO} level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void info(final String message, final Object... params) {
        if (isEnabled(Level.INFO, null, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(null, FQCN, Level.INFO, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void info(final String message, final Throwable t) {
        if (isEnabled(Level.INFO, null, message, t)) {
            log(null, FQCN, Level.INFO, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Checks whether this Logger is enabled for the DEBUG Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level
     *         DEBUG, {@code false} otherwise.
     */
    @Override
    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG, null, null);
    }

    /**
     * Checks whether this Logger is enabled for the DEBUG Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level
     *         DEBUG, {@code false} otherwise.
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
     * @param level the level to check
     * @return boolean - {@code true} if this Logger is enabled for level, {@code false} otherwise.
     */
    @Override
    public boolean isEnabled(final Level level) {
        return isEnabled(level, null, (Object) null, null);
    }

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The Message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, Message data, Throwable t);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, Object data, Throwable t);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, String data);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param p1 The parameters.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, String data, Object... p1);

    /**
     * Determine if logging is enabled.
     * @param level The logging Level to check.
     * @param marker A Marker or null.
     * @param data The message.
     * @param t A Throwable.
     * @return True if logging is enabled, false otherwise.
     */
    protected abstract boolean isEnabled(Level level, Marker marker, String data, Throwable t);

    /**
     * Checks whether this Logger is enabled for the {@link Level#ERROR ERROR} Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level
     *         {@link Level#ERROR ERROR}, {@code false} otherwise.
     */
    @Override
    public boolean isErrorEnabled() {
        return isEnabled(Level.ERROR, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the {@link Level#ERROR ERROR} Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level
     *         {@link Level#ERROR ERROR}, {@code false} otherwise.
     */
    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return isEnabled(Level.ERROR, marker, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the FATAL Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level
     *         FATAL, {@code false} otherwise.
     */
    @Override
    public boolean isFatalEnabled() {
        return isEnabled(Level.FATAL, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the FATAL Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level
     *         FATAL, {@code false} otherwise.
     */
    @Override
    public boolean isFatalEnabled(final Marker marker) {
        return isEnabled(Level.FATAL, marker, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the INFO Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level
     *         INFO, {@code false} otherwise.
     */
    @Override
    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the INFO Level.
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level
     *         INFO, {@code false} otherwise.
     */
    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return isEnabled(Level.INFO, marker, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the TRACE  Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level
     *         TRACE, {@code false} otherwise.
     */
    @Override
    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the TRACE  Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level
     *         TRACE, {@code false} otherwise.
     */
    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return isEnabled(Level.TRACE, marker, (Object) null, null);
    }


    /**
     * Checks whether this Logger is enabled for the WARN Level.
     *
     * @return boolean - {@code true} if this Logger is enabled for level
     *         WARN, {@code false} otherwise.
     */
    @Override
    public boolean isWarnEnabled() {
        return isEnabled(Level.WARN, null, (Object) null, null);
    }

    /**
     * Checks whether this Logger is enabled for the WARN Level.
     *
     * @param marker The marker data.
     * @return boolean - {@code true} if this Logger is enabled for level
     *         WARN, {@code false} otherwise.
     */
    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return isEnabled(Level.WARN, marker, (Object) null, null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker) {
        return isEnabled(level, marker, (Object) null, null);
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
        if (isEnabled(level, marker, msg, null)) {
            log(marker, FQCN, level, msg, null);
        }
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
        if (isEnabled(level, marker, msg, t)) {
            log(marker, FQCN, level, msg, t);
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
    public void log(final Level level, final Marker marker, final Object message) {
        if (isEnabled(level, marker, message, null)) {
            log(marker, FQCN, level, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the given level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void log(final Level level, final Marker marker, final Object message, final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            log(marker, FQCN, level, messageFactory.newMessage(message), t);
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
        if (isEnabled(level, marker, message)) {
            log(marker, FQCN, level, messageFactory.newMessage(message), null);
        }
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
        if (isEnabled(level, marker, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(marker, FQCN, level, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the given level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param level the logging level
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void log(final Level level, final Marker marker, final String message, final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            log(marker, FQCN, level, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param level the logging level
     * @param msg the message string to be logged
     */
    @Override
    public void log(final Level level, final Message msg) {
        if (isEnabled(level, null, msg, null)) {
            log(null, FQCN, level, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the given level.
     *
     * @param level the logging level
     * @param msg the message string to be logged
     * @param t A Throwable or null.
     */
    @Override
    public void log(final Level level, final Message msg, final Throwable t) {
        if (isEnabled(level, null, msg, t)) {
            log(null, FQCN, level, msg, t);
        }
    }

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param message the message object to log.
     */
    @Override
    public void log(final Level level, final Object message) {
        if (isEnabled(level, null, message, null)) {
            log(null, FQCN, level, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the given level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param level the logging level
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void log(final Level level, final Object message, final Throwable t) {
        if (isEnabled(level, null, message, t)) {
            log(null, FQCN, level, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the given level.
     *
     * @param level the logging level
     * @param message the message object to log.
     */
    @Override
    public void log(final Level level, final String message) {
        if (isEnabled(level, null, message)) {
            log(null, FQCN, level, messageFactory.newMessage(message), null);
        }
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
        if (isEnabled(level, null, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(null, FQCN, level, msg, msg.getThrowable());
        }
    }


    /**
     * Logs a message at the given level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param level the logging level
     * @param message the message to log.
     * @param t the exception to log, including its stack trace.
     */
    @Override
    public void log(final Level level, final String message, final Throwable t) {
        if (isEnabled(level, null, message, t)) {
            log(null, FQCN, level, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a formatted message using the specified format string and arguments.
     * @param level The logging Level.
     * @param format The format String.
     * @param params Arguments specified by the format.
     */
    @Override
    public void printf(Level level, String format, Object... params) {
        if (isEnabled(level, null, format, params)) {
            Message msg = new StringFormattedMessage(format, params);
            log(null, FQCN, level, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a formatted message using the specified format string and arguments.
     * @param level The logging Level.
     * @param marker the marker data specific to this log statement.
     * @param format The format String.
     * @param params Arguments specified by the format.
     */
    @Override
    public void printf(Level level, Marker marker, String format, Object... params) {
        if (isEnabled(level, marker, format, params)) {
            Message msg = new StringFormattedMessage(format, params);
            log(marker, FQCN, level, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message with location information.
     *
     * @param marker The Marker
     * @param fqcn   The fully qualified class name of the <b>caller</b>
     * @param level  The logging level
     * @param data   The Message.
     * @param t      A Throwable or null.
     */
    public abstract void log(Marker marker, String fqcn, Level level, Message data, Throwable t);

    /**
     * Logs a Throwable to be thrown.
     *
     * @param <T> the type of the Throwable.
     * @param level The logging Level.
     * @param t     The Throwable.
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
     * @param t The Throwable.
     * @return the Throwable.
     */
    @Override
    public <T extends Throwable> T throwing(final T t) {
        return throwing(FQCN, Level.ERROR, t);
    }

    /**
     * Logs a Throwable to be thrown with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param <T> the type of the Throwable.
     * @param level The logging Level.
     * @param t The Throwable.
     * @return the Throwable.
     */
    protected <T extends Throwable> T throwing(final String fqcn, final Level level, final T t) {
        if (isEnabled(level, THROWING_MARKER, (Object) null, null)) {
            log(THROWING_MARKER, fqcn, level, messageFactory.newMessage(THROWING), t);
        }
        return t;
    }

    private Message toExitMsg(final Object result) {
        if (result == null) {
            return messageFactory.newMessage("exit");
        }
        return messageFactory.newMessage("exit with(" + result + ")");
    }

    /**
     * Returns a String representation of this instance in the form {@code "name"}.
     * @return A String describing this Logger instance.
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Logs a message with the specific Marker at the TRACE level.
     *
     * @param marker the marker data specific to this log statement.
     * @param msg    the message string to be logged
     */
    @Override
    public void trace(final Marker marker, final Message msg) {
        if (isEnabled(Level.TRACE, marker, msg, null)) {
            log(marker, FQCN, Level.TRACE, msg, null);
        }
    }


    /**
     * Logs a message with the specific Marker at the TRACE level.
     *
     * @param marker the marker data specific to this log statement.
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    @Override
    public void trace(final Marker marker, final Message msg, final Throwable t) {
        if (isEnabled(Level.TRACE, marker, msg, t)) {
            log(marker, FQCN, Level.TRACE, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void trace(final Marker marker, final Object message) {
        if (isEnabled(Level.TRACE, marker, message, null)) {
            log(marker, FQCN, Level.TRACE, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void trace(final Marker marker, final Object message, final Throwable t) {
        if (isEnabled(Level.TRACE, marker, message, t)) {
            log(marker, FQCN, Level.TRACE, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void trace(final Marker marker, final String message) {
        if (isEnabled(Level.TRACE, marker, message)) {
            log(marker, FQCN, Level.TRACE, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#TRACE TRACE} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void trace(final Marker marker, final String message, final Object... params) {
        if (isEnabled(Level.TRACE, marker, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(marker, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void trace(final Marker marker, final String message, final Throwable t) {
        if (isEnabled(Level.TRACE, marker, message, t)) {
            log(marker, FQCN, Level.TRACE, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message with the specific Marker at the TRACE level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void trace(final Message msg) {
        if (isEnabled(Level.TRACE, null, msg, null)) {
            log(null, FQCN, Level.TRACE, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the TRACE level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    @Override
    public void trace(final Message msg, final Throwable t) {
        if (isEnabled(Level.TRACE, null, msg, t)) {
            log(null, FQCN, Level.TRACE, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void trace(final Object message) {
        if (isEnabled(Level.TRACE, null, message, null)) {
            log(null, FQCN, Level.TRACE, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void trace(final Object message, final Throwable t) {
        if (isEnabled(Level.TRACE, null, message, t)) {
            log(null, FQCN, Level.TRACE, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#TRACE TRACE} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void trace(final String message) {
        if (isEnabled(Level.TRACE, null, message)) {
            log(null, FQCN, Level.TRACE, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#TRACE TRACE} level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void trace(final String message, final Object... params) {
        if (isEnabled(Level.TRACE, null, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(null, FQCN, Level.TRACE, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     * <p/>
     * <p>
     * See {@link #debug(String)} form for more detailed information.
     * </p>
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void trace(final String message, final Throwable t) {
        if (isEnabled(Level.TRACE, null, message, t)) {
            log(null, FQCN, Level.TRACE, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message with the specific Marker at the WARN level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     */
    @Override
    public void warn(final Marker marker, final Message msg) {
        if (isEnabled(Level.WARN, marker, msg, null)) {
            log(marker, FQCN, Level.WARN, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the WARN level.
     *
     * @param marker the marker data specific to this log statement
     * @param msg    the message string to be logged
     * @param t      A Throwable or null.
     */
    @Override
    public void warn(final Marker marker, final Message msg, final Throwable t) {
        if (isEnabled(Level.WARN, marker, msg, t)) {
            log(marker, FQCN, Level.WARN, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void warn(final Marker marker, final Object message) {
        if (isEnabled(Level.WARN, marker, message, null)) {
            log(marker, FQCN, Level.WARN, messageFactory.newMessage(message), null);
        }
    }

    /*
     * Instead of one single method with Object... declared the following methods explicitly specify
     * parameters because they perform dramatically better than having the JVM convert them to an
     * array.
     */

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void warn(final Marker marker, final Object message, final Throwable t) {
        if (isEnabled(Level.WARN, marker, message, t)) {
            log(marker, FQCN, Level.WARN, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     */
    @Override
    public void warn(final Marker marker, final String message) {
        if (isEnabled(Level.WARN, marker, message)) {
            log(marker, FQCN, Level.WARN, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#WARN WARN} level.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void warn(final Marker marker, final String message, final Object... params) {
        if (isEnabled(Level.WARN, marker, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(marker, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param marker the marker data specific to this log statement.
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void warn(final Marker marker, final String message, final Throwable t) {
        if (isEnabled(Level.WARN, marker, message, t)) {
            log(marker, FQCN, Level.WARN, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message with the specific Marker at the WARN level.
     *
     * @param msg the message string to be logged
     */
    @Override
    public void warn(final Message msg) {
        if (isEnabled(Level.WARN, null, msg, null)) {
            log(null, FQCN, Level.WARN, msg, null);
        }
    }

    /**
     * Logs a message with the specific Marker at the WARN level.
     *
     * @param msg the message string to be logged
     * @param t   A Throwable or null.
     */
    @Override
    public void warn(final Message msg, final Throwable t) {
        if (isEnabled(Level.WARN, null, msg, t)) {
            log(null, FQCN, Level.WARN, msg, t);
        }
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void warn(final Object message) {
        if (isEnabled(Level.WARN, null, message, null)) {
            log(null, FQCN, Level.WARN, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void warn(final Object message, final Throwable t) {
        if (isEnabled(Level.WARN, null, message, t)) {
            log(null, FQCN, Level.WARN, messageFactory.newMessage(message), t);
        }
    }

    /**
     * Logs a message object with the {@link Level#WARN WARN} level.
     *
     * @param message the message object to log.
     */
    @Override
    public void warn(final String message) {
        if (isEnabled(Level.WARN, null, message)) {
            log(null, FQCN, Level.WARN, messageFactory.newMessage(message), null);
        }
    }

    /**
     * Logs a message with parameters at the {@link Level#WARN WARN} level.
     *
     * @param message the message to log.
     * @param params  parameters to the message.
     */
    @Override
    public void warn(final String message, final Object... params) {
        if (isEnabled(Level.WARN, null, message, params)) {
            final Message msg = messageFactory.newMessage(message, params);
            log(null, FQCN, Level.WARN, msg, msg.getThrowable());
        }
    }

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log.
     * @param t       the exception to log, including its stack trace.
     */
    @Override
    public void warn(final String message, final Throwable t) {
        if (isEnabled(Level.WARN, null, message, t)) {
            log(null, FQCN, Level.WARN, messageFactory.newMessage(message), t);
        }
    }

}
