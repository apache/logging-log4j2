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
public abstract class AbstractLogger implements ExtendedLogger, Serializable {

    private static final long serialVersionUID = 2L;

    /**
     * Marker for flow tracing.
     */
    public static final Marker FLOW_MARKER = MarkerManager.getMarker("FLOW");

    /**
     * Marker for method entry tracing.
     */
    public static final Marker ENTRY_MARKER = MarkerManager.getMarker("ENTRY").setParents(FLOW_MARKER);

    /**
     * Marker for method exit tracing.
     */
    public static final Marker EXIT_MARKER = MarkerManager.getMarker("EXIT").setParents(FLOW_MARKER);

    /**
     * Marker for exception tracing.
     */
    public static final Marker EXCEPTION_MARKER = MarkerManager.getMarker("EXCEPTION");

    /**
     * Marker for throwing exceptions.
     */
    public static final Marker THROWING_MARKER = MarkerManager.getMarker("THROWING").setParents(EXCEPTION_MARKER);

    /**
     * Marker for catching exceptions.
     */
    public static final Marker CATCHING_MARKER = MarkerManager.getMarker("CATCHING").setParents(EXCEPTION_MARKER);

    /**
     * The default MessageFactory class.
     */
    public static final Class<? extends MessageFactory> DEFAULT_MESSAGE_FACTORY_CLASS = ParameterizedMessageFactory.class;

    private static final String FQCN = AbstractLogger.class.getName();

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
    public static void checkMessageFactory(final ExtendedLogger logger, final MessageFactory messageFactory) {
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
     * Creates a new logger named after this class (or subclass).
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
     * Creates a new named logger with a particular {@link MessageFactory}.
     *
     * @param name the logger name
     * @param messageFactory the message factory, if null then use the default message factory.
     */
    public AbstractLogger(final String name, final MessageFactory messageFactory) {
        this.name = name;
        this.messageFactory = messageFactory == null ? createDefaultMessageFactory() : messageFactory;
    }

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

    @Override
    public void debug(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.DEBUG, marker, msg, null);
    }

    @Override
    public void debug(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, marker, msg, t);
    }

    @Override
    public void debug(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, null);
    }

    @Override
    public void debug(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, t);
    }

    @Override
    public void debug(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, (Throwable) null);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, params);
    }

    @Override
    public void debug(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, t);
    }

    @Override
    public void debug(final Message msg) {
        logIfEnabled(FQCN, Level.DEBUG, null, msg, null);
    }

    @Override
    public void debug(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, msg, t);
    }

    @Override
    public void debug(final Object message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, null);
    }

    @Override
    public void debug(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, t);
    }

    @Override
    public void debug(final String message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, (Throwable) null);
    }

    @Override
    public void debug(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, params);
    }

    @Override
    public void debug(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, t);
    }

    @Override
    public void entry() {
        entry(FQCN);
    }

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

    @Override
    public void error(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.ERROR, marker, msg, null);
    }

    @Override
    public void error(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, msg, t);
    }

    @Override
    public void error(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, null);
    }

    @Override
    public void error(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, t);
    }

    @Override
    public void error(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, (Throwable) null);
    }

    @Override
    public void error(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, params);
    }

    @Override
    public void error(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, t);
    }

    @Override
    public void error(final Message msg) {
        logIfEnabled(FQCN, Level.ERROR, null, msg, null);
    }

    @Override
    public void error(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, msg, t);
    }

    @Override
    public void error(final Object message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, null);
    }

    @Override
    public void error(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, message, t);
    }

    @Override
    public void error(final String message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, (Throwable) null);
    }

    @Override
    public void error(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.ERROR, null, message, params);
    }

    @Override
    public void error(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, message, t);
    }

    @Override
    public void exit() {
        exit(FQCN, null);
    }

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
     * @return the return value passed to this method.
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

    @Override
    public void fatal(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.FATAL, marker, msg, null);
    }

    @Override
    public void fatal(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, msg, t);
    }

    @Override
    public void fatal(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, null);
    }

    @Override
    public void fatal(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, t);
    }

    @Override
    public void fatal(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, (Throwable) null);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, params);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, t);
    }

    @Override
    public void fatal(final Message msg) {
        logIfEnabled(FQCN, Level.FATAL, null, msg, null);
    }

    @Override
    public void fatal(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, msg, t);
    }

    @Override
    public void fatal(final Object message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, null);
    }

    @Override
    public void fatal(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, message, t);
    }

    @Override
    public void fatal(final String message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, (Throwable) null);
    }

    @Override
    public void fatal(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.FATAL, null, message, params);
    }

    @Override
    public void fatal(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, message, t);
    }

    @Override
    public MessageFactory getMessageFactory() {
        return messageFactory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void info(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.INFO, marker, msg, null);
    }

    @Override
    public void info(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, msg, t);
    }

    @Override
    public void info(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, null);
    }

    @Override
    public void info(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, message, t);
    }

    @Override
    public void info(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, (Throwable) null);
    }

    @Override
    public void info(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.INFO, marker, message, params);
    }

    @Override
    public void info(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, message, t);
    }

    @Override
    public void info(final Message msg) {
        logIfEnabled(FQCN, Level.INFO, null, msg, null);
    }

    @Override
    public void info(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, msg, t);
    }

    @Override
    public void info(final Object message) {
        logIfEnabled(FQCN, Level.INFO, null, message, null);
    }

    @Override
    public void info(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, message, t);
    }

    @Override
    public void info(final String message) {
        logIfEnabled(FQCN, Level.INFO, null, message, (Throwable) null);
    }

    @Override
    public void info(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.INFO, null, message, params);
    }

    @Override
    public void info(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, message, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG, null, null);
    }

    @Override
    public boolean isDebugEnabled(final Marker marker) {
        return isEnabled(Level.DEBUG, marker, (Object) null, null);
    }

    @Override
    public boolean isEnabled(final Level level) {
        return isEnabled(level, null, (Object) null, null);
    }

    @Override
    public boolean isEnabled(final Level level, final Marker marker) {
        return isEnabled(level, marker, (Object) null, null);
    }

    @Override
    public boolean isErrorEnabled() {
        return isEnabled(Level.ERROR, null, (Object) null, null);
    }

    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return isEnabled(Level.ERROR, marker, (Object) null, null);
    }

    @Override
    public boolean isFatalEnabled() {
        return isEnabled(Level.FATAL, null, (Object) null, null);
    }

    @Override
    public boolean isFatalEnabled(final Marker marker) {
        return isEnabled(Level.FATAL, marker, (Object) null, null);
    }

    @Override
    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO, null, (Object) null, null);
    }

    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return isEnabled(Level.INFO, marker, (Object) null, null);
    }

    @Override
    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE, null, (Object) null, null);
    }

    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return isEnabled(Level.TRACE, marker, (Object) null, null);
    }

    @Override
    public boolean isWarnEnabled() {
        return isEnabled(Level.WARN, null, (Object) null, null);
    }

    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return isEnabled(Level.WARN, marker, (Object) null, null);
    }

    @Override
    public void log(final Level level, final Marker marker, final Message msg) {
        logIfEnabled(FQCN, level, marker, msg, (Throwable) null);
    }

    @Override
    public void log(final Level level, final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, level, marker, msg, t);
    }

    @Override
    public void log(final Level level, final Marker marker, final Object message) {
        logIfEnabled(FQCN, level, marker, message, (Throwable) null);
    }

    @Override
    public void log(final Level level, final Marker marker, final Object message, final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            logMessage(FQCN, level, marker, message, t);
        }
    }

    @Override
    public void log(final Level level, final Marker marker, final String message) {
        logIfEnabled(FQCN, level, marker, message, (Throwable) null);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, level, marker, message, params);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, level, marker, message, t);
    }

    @Override
    public void log(final Level level, final Message msg) {
        logIfEnabled(FQCN, level, null, msg, null);
    }

    @Override
    public void log(final Level level, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, level, null, msg, t);
    }

    @Override
    public void log(final Level level, final Object message) {
        logIfEnabled(FQCN, level, null, message, null);
    }

    @Override
    public void log(final Level level, final Object message, final Throwable t) {
        logIfEnabled(FQCN, level, null, message, t);
    }

    @Override
    public void log(final Level level, final String message) {
        logIfEnabled(FQCN, level, null, message, (Throwable) null);
    }

    @Override
    public void log(final Level level, final String message, final Object... params) {
        logIfEnabled(FQCN, level, null, message, params);
    }

    @Override
    public void log(final Level level, final String message, final Throwable t) {
        logIfEnabled(FQCN, level, null, message, t);
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final Message msg,
            final Throwable t) {
        if (isEnabled(level, marker, msg, t)) {
            logMessage(fqcn, level, marker, msg, t);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final Object message,
            final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            logMessage(fqcn, level, marker, message, t);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message) {
        if (isEnabled(level, marker, message)) {
            logMessage(fqcn, level, marker, message);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object... params) {
        if (isEnabled(level, marker, message, params)) {
            logMessage(fqcn, level, marker, message, params);
        }
    }

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

    @Override
    public void printf(final Level level, final Marker marker, final String format, final Object... params) {
        if (isEnabled(level, marker, format, params)) {
            final Message msg = new StringFormattedMessage(format, params);
            logMessage(FQCN, level, marker, msg, msg.getThrowable());
        }
    }

    @Override
    public void printf(final Level level, final String format, final Object... params) {
        if (isEnabled(level, null, format, params)) {
            final Message msg = new StringFormattedMessage(format, params);
            logMessage(FQCN, level, null, msg, msg.getThrowable());
        }
    }

    @Override
    public <T extends Throwable> T throwing(final T t) {
        return throwing(FQCN, Level.ERROR, t);
    }

    @Override
    public <T extends Throwable> T throwing(final Level level, final T t) {
        return throwing(FQCN, level, t);
    }

    /**
     * Logs a Throwable to be thrown.
     *
     * @param <T> the type of the Throwable.
     * @param fqcn the fully qualified class name of this Logger implementation.
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

    @Override
    public void trace(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.TRACE, marker, msg, null);
    }

    @Override
    public void trace(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, msg, t);
    }

    @Override
    public void trace(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, null);
    }

    @Override
    public void trace(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, t);
    }

    @Override
    public void trace(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, (Throwable) null);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, params);
    }

    @Override
    public void trace(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, t);
    }

    @Override
    public void trace(final Message msg) {
        logIfEnabled(FQCN, Level.TRACE, null, msg, null);
    }

    @Override
    public void trace(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, msg, t);
    }

    @Override
    public void trace(final Object message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, null);
    }

    @Override
    public void trace(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, message, t);
    }

    @Override
    public void trace(final String message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, (Throwable) null);
    }

    @Override
    public void trace(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.TRACE, null, message, params);
    }

    @Override
    public void trace(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, message, t);
    }

    @Override
    public void warn(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.WARN, marker, msg, null);
    }

    @Override
    public void warn(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, msg, t);
    }

    @Override
    public void warn(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, null);
    }

    /* -- FIXME: this comment looks lost
     * Instead of one single method with Object... declared the following methods explicitly specify parameters because
     * they perform dramatically better than having the JVM convert them to an array.
     */

    @Override
    public void warn(final Marker marker, final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, message, t);
    }

    @Override
    public void warn(final Marker marker, final String message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, (Throwable) null);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.WARN, marker, message, params);
    }

    @Override
    public void warn(final Marker marker, final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, message, t);
    }

    @Override
    public void warn(final Message msg) {
        logIfEnabled(FQCN, Level.WARN, null, msg, null);
    }

    @Override
    public void warn(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, msg, t);
    }

    @Override
    public void warn(final Object message) {
        logIfEnabled(FQCN, Level.WARN, null, message, null);
    }

    @Override
    public void warn(final Object message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, message, t);
    }

    @Override
    public void warn(final String message) {
        logIfEnabled(FQCN, Level.WARN, null, message, (Throwable) null);
    }

    @Override
    public void warn(final String message, final Object... params) {
        logIfEnabled(FQCN, Level.WARN, null, message, params);
    }

    @Override
    public void warn(final String message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, message, t);
    }

}
