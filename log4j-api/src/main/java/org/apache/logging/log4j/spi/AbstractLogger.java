/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.spi;

import java.io.Serializable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.internal.DefaultLogBuilder;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.MessageFactory2;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.LambdaUtil;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Supplier;

/**
 * Base implementation of a Logger. It is highly recommended that any Logger implementation extend this class.
 */
public abstract class AbstractLogger implements ExtendedLogger, LocationAwareLogger, Serializable {
    // Implementation note: many methods in this class are tuned for performance. MODIFY WITH CARE!
    // Specifically, try to keep the hot methods to 35 bytecodes or less:
    // this is within the MaxInlineSize threshold on Java 7 and Java 8 Hotspot and makes these methods
    // candidates for immediate inlining instead of waiting until they are designated "hot enough".

    /**
     * Marker for flow tracing.
     */
    public static final Marker FLOW_MARKER = MarkerManager.getMarker("FLOW");

    /**
     * Marker for method entry tracing.
     */
    public static final Marker ENTRY_MARKER = MarkerManager.getMarker("ENTER").setParents(FLOW_MARKER);

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
    public static final Marker THROWING_MARKER =
            MarkerManager.getMarker("THROWING").setParents(EXCEPTION_MARKER);

    /**
     * Marker for catching exceptions.
     */
    public static final Marker CATCHING_MARKER =
            MarkerManager.getMarker("CATCHING").setParents(EXCEPTION_MARKER);

    /**
     * The default MessageFactory class.
     */
    public static final Class<? extends MessageFactory> DEFAULT_MESSAGE_FACTORY_CLASS =
            ParameterizedMessageFactory.class;

    /**
     * The default FlowMessageFactory class.
     */
    public static final Class<? extends FlowMessageFactory> DEFAULT_FLOW_MESSAGE_FACTORY_CLASS =
            DefaultFlowMessageFactory.class;

    private static final long serialVersionUID = 2L;

    private static final String FQCN = AbstractLogger.class.getName();
    private static final String THROWING = "Throwing";
    private static final String CATCHING = "Catching";

    protected final String name;
    private final MessageFactory2 messageFactory;
    private final FlowMessageFactory flowMessageFactory;
    private static final ThreadLocal<int[]> recursionDepthHolder = new ThreadLocal<>(); // LOG4J2-1518, LOG4J2-2031
    private static final ThreadLocal<DefaultLogBuilder> logBuilder = ThreadLocal.withInitial(DefaultLogBuilder::new);

    /**
     * Constructs an instance named after this class.
     */
    public AbstractLogger() {
        this(null, null, null);
    }

    /**
     * Constructs an instance using the provided name.
     *
     * @param name the logger name (if null, will be derived from this class or subclass)
     */
    public AbstractLogger(final String name) {
        this(name, null, null);
    }

    /**
     * Constructs an instance using the provided name and {@link MessageFactory}.
     *
     * @param name the logger name (if null, will be derived from this class)
     * @param messageFactory the {@link Message} factory (if null, {@link ParameterizedMessageFactory} will be used)
     */
    public AbstractLogger(final String name, final MessageFactory messageFactory) {
        this(name, messageFactory, null);
    }

    /**
     * The canonical constructor.
     *
     * @param name the logger name (if null, will be derived from this class)
     * @param messageFactory the {@link Message} factory (if null, {@link ParameterizedMessageFactory} will be used)
     * @param flowMessageFactory the {@link org.apache.logging.log4j.message.FlowMessage} factory (if null, {@link DefaultFlowMessageFactory} will be used)
     */
    protected AbstractLogger(
            final String name, final MessageFactory messageFactory, final FlowMessageFactory flowMessageFactory) {
        if (name != null) {
            this.name = name;
        } else {
            final Class<? extends AbstractLogger> clazz = getClass();
            final String canonicalName = clazz.getCanonicalName();
            this.name = canonicalName != null ? canonicalName : clazz.getName();
        }
        this.messageFactory =
                messageFactory != null ? adaptMessageFactory(messageFactory) : ParameterizedMessageFactory.INSTANCE;
        this.flowMessageFactory = flowMessageFactory != null ? flowMessageFactory : DefaultFlowMessageFactory.INSTANCE;
    }

    private static MessageFactory2 adaptMessageFactory(final MessageFactory result) {
        if (result instanceof MessageFactory2) {
            return (MessageFactory2) result;
        }
        return new MessageFactory2Adapter(result);
    }

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
            StatusLogger.getLogger()
                    .warn(
                            "The Logger {} was created with the message factory {} and is now requested with the "
                                    + "message factory {}, which may create log events with unexpected formatting.",
                            name,
                            loggerMessageFactory,
                            messageFactory);
        } else if (messageFactory == null && !loggerMessageFactory.getClass().equals(DEFAULT_MESSAGE_FACTORY_CLASS)) {
            StatusLogger.getLogger()
                    .warn(
                            "The Logger {} was created with the message factory {} and is now requested with a null "
                                    + "message factory (defaults to {}), which may create log events with unexpected "
                                    + "formatting.",
                            name,
                            loggerMessageFactory,
                            DEFAULT_MESSAGE_FACTORY_CLASS.getName());
        }
    }

    @Override
    public void catching(final Level level, final Throwable throwable) {
        catching(FQCN, level, throwable);
    }

    /**
     * Logs a Throwable that has been caught with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param level The logging level.
     * @param throwable The Throwable.
     */
    protected void catching(final String fqcn, final Level level, final Throwable throwable) {
        if (isEnabled(level, CATCHING_MARKER, (Object) null, null)) {
            logMessageSafely(fqcn, level, CATCHING_MARKER, catchingMsg(throwable), throwable);
        }
    }

    @Override
    public void catching(final Throwable throwable) {
        if (isEnabled(Level.ERROR, CATCHING_MARKER, (Object) null, null)) {
            logMessageSafely(FQCN, Level.ERROR, CATCHING_MARKER, catchingMsg(throwable), throwable);
        }
    }

    protected Message catchingMsg(final Throwable throwable) {
        return messageFactory.newMessage(CATCHING);
    }

    @Override
    public void debug(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, null);
    }

    @Override
    public void debug(final Marker marker, final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, throwable);
    }

    @Override
    public void debug(final Marker marker, final Message message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void debug(final Marker marker, final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, throwable);
    }

    @Override
    public void debug(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, null);
    }

    @Override
    public void debug(final Marker marker, final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, throwable);
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
    public void debug(final Marker marker, final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, throwable);
    }

    @Override
    public void debug(final Message message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void debug(final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, throwable);
    }

    @Override
    public void debug(final CharSequence message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, null);
    }

    @Override
    public void debug(final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, throwable);
    }

    @Override
    public void debug(final Object message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, null);
    }

    @Override
    public void debug(final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, throwable);
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
    public void debug(final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, null, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, marker, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final Marker marker, final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, paramSuppliers);
    }

    @Override
    public void debug(final Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, marker, messageSupplier, (Throwable) null);
    }

    @Override
    public void debug(final Marker marker, final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, messageSupplier, throwable);
    }

    @Override
    public void debug(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, null, messageSupplier, (Throwable) null);
    }

    @Override
    public void debug(final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, messageSupplier, throwable);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2);
    }

    @Override
    public void debug(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void debug(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void debug(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void debug(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void debug(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void debug(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void debug(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void debug(final String message, final Object p0) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0);
    }

    @Override
    public void debug(final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1);
    }

    @Override
    public void debug(final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2);
    }

    @Override
    public void debug(final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3);
    }

    @Override
    public void debug(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void debug(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void debug(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void debug(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void debug(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void debug(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs entry to a method with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param format Format String for the parameters.
     * @param paramSuppliers The Suppliers of the parameters.
     */
    @SuppressWarnings("deprecation")
    protected EntryMessage enter(final String fqcn, final String format, final Supplier<?>... paramSuppliers) {
        EntryMessage entryMsg = null;
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            logMessageSafely(
                    fqcn,
                    Level.TRACE,
                    ENTRY_MARKER,
                    entryMsg = flowMessageFactory.newEntryMessage(format, LambdaUtil.getAll(paramSuppliers)),
                    null);
        }
        return entryMsg;
    }

    /**
     * Logs entry to a method with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param format The format String for the parameters.
     * @param paramSuppliers The parameters to the method.
     */
    @Deprecated
    protected EntryMessage enter(final String fqcn, final String format, final MessageSupplier... paramSuppliers) {
        EntryMessage entryMsg = null;
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            logMessageSafely(fqcn, Level.TRACE, ENTRY_MARKER, entryMsg = entryMsg(format, paramSuppliers), null);
        }
        return entryMsg;
    }

    /**
     * Logs entry to a method with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param format The format String for the parameters.
     * @param params The parameters to the method.
     */
    protected EntryMessage enter(final String fqcn, final String format, final Object... params) {
        EntryMessage entryMsg = null;
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            logMessageSafely(
                    fqcn,
                    Level.TRACE,
                    ENTRY_MARKER,
                    entryMsg = flowMessageFactory.newEntryMessage(format, params),
                    null);
        }
        return entryMsg;
    }

    /**
     * Logs entry to a method with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param messageSupplier The Supplier of the Message.
     */
    @Deprecated
    protected EntryMessage enter(final String fqcn, final MessageSupplier messageSupplier) {
        EntryMessage message = null;
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            logMessageSafely(
                    fqcn,
                    Level.TRACE,
                    ENTRY_MARKER,
                    message = flowMessageFactory.newEntryMessage(messageSupplier.get()),
                    null);
        }
        return message;
    }

    /**
     * Logs entry to a method with location information.
     *
     * @param fqcn
     *            The fully qualified class name of the <b>caller</b>.
     * @param message
     *            the Message.
     * @since 2.6
     */
    protected EntryMessage enter(final String fqcn, final Message message) {
        EntryMessage flowMessage = null;
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            logMessageSafely(
                    fqcn, Level.TRACE, ENTRY_MARKER, flowMessage = flowMessageFactory.newEntryMessage(message), null);
        }
        return flowMessage;
    }

    @Deprecated
    @Override
    public void entry() {
        entry(FQCN, (Object[]) null);
    }

    @Deprecated
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
    @SuppressWarnings("deprecation")
    protected void entry(final String fqcn, final Object... params) {
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            if (params == null) {
                logMessageSafely(fqcn, Level.TRACE, ENTRY_MARKER, entryMsg(null, (Supplier<?>[]) null), null);
            } else {
                logMessageSafely(fqcn, Level.TRACE, ENTRY_MARKER, entryMsg(null, params), null);
            }
        }
    }

    protected EntryMessage entryMsg(final String format, final Object... params) {
        return flowMessageFactory.newEntryMessage(format, params);
    }

    protected EntryMessage entryMsg(final String format, final MessageSupplier... paramSuppliers) {
        final int count = paramSuppliers == null ? 0 : paramSuppliers.length;
        final Object[] params = new Object[count];
        for (int i = 0; i < count; i++) {
            params[i] = paramSuppliers[i].get();
        }
        return entryMsg(format, params);
    }

    @SuppressWarnings("deprecation")
    protected EntryMessage entryMsg(final String format, final Supplier<?>... paramSuppliers) {
        return entryMsg(format, LambdaUtil.getAll(paramSuppliers));
    }

    @Override
    public void error(final Marker marker, final Message message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void error(final Marker marker, final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, throwable);
    }

    @Override
    public void error(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, null);
    }

    @Override
    public void error(final Marker marker, final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, throwable);
    }

    @Override
    public void error(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, null);
    }

    @Override
    public void error(final Marker marker, final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, throwable);
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
    public void error(final Marker marker, final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, throwable);
    }

    @Override
    public void error(final Message message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void error(final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, message, throwable);
    }

    @Override
    public void error(final CharSequence message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, null);
    }

    @Override
    public void error(final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, message, throwable);
    }

    @Override
    public void error(final Object message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, null);
    }

    @Override
    public void error(final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, message, throwable);
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
    public void error(final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.ERROR, null, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.ERROR, marker, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final Marker marker, final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.ERROR, null, message, paramSuppliers);
    }

    @Override
    public void error(final Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.ERROR, marker, messageSupplier, (Throwable) null);
    }

    @Override
    public void error(final Marker marker, final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, messageSupplier, throwable);
    }

    @Override
    public void error(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.ERROR, null, messageSupplier, (Throwable) null);
    }

    @Override
    public void error(final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, messageSupplier, throwable);
    }

    @Override
    public void error(final Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0);
    }

    @Override
    public void error(final Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1);
    }

    @Override
    public void error(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2);
    }

    @Override
    public void error(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void error(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void error(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void error(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void error(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void error(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void error(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void error(final String message, final Object p0) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0);
    }

    @Override
    public void error(final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1);
    }

    @Override
    public void error(final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2);
    }

    @Override
    public void error(final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3);
    }

    @Override
    public void error(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void error(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void error(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void error(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void error(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void error(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Deprecated
    @Override
    public void exit() {
        exit(FQCN, (Object) null);
    }

    @Deprecated
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
        if (isEnabled(Level.TRACE, EXIT_MARKER, (CharSequence) null, null)) {
            logMessageSafely(fqcn, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(null, result), null);
        }
        return result;
    }

    /**
     * Logs exiting from a method with the result and location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param <R> The type of the parameter and object being returned.
     * @param result The result being returned from the method call.
     * @return the return value passed to this method.
     */
    protected <R> R exit(final String fqcn, final String format, final R result) {
        if (isEnabled(Level.TRACE, EXIT_MARKER, (CharSequence) null, null)) {
            logMessageSafely(fqcn, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(format, result), null);
        }
        return result;
    }

    protected Message exitMsg(final String format, final Object result) {
        return flowMessageFactory.newExitMessage(format, result);
    }

    @Override
    public void fatal(final Marker marker, final Message message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void fatal(final Marker marker, final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, throwable);
    }

    @Override
    public void fatal(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, null);
    }

    @Override
    public void fatal(final Marker marker, final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, throwable);
    }

    @Override
    public void fatal(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, null);
    }

    @Override
    public void fatal(final Marker marker, final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, throwable);
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
    public void fatal(final Marker marker, final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, throwable);
    }

    @Override
    public void fatal(final Message message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void fatal(final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, message, throwable);
    }

    @Override
    public void fatal(final CharSequence message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, null);
    }

    @Override
    public void fatal(final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, message, throwable);
    }

    @Override
    public void fatal(final Object message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, null);
    }

    @Override
    public void fatal(final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, message, throwable);
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
    public void fatal(final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.FATAL, null, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.FATAL, marker, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final Marker marker, final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.FATAL, null, message, paramSuppliers);
    }

    @Override
    public void fatal(final Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.FATAL, marker, messageSupplier, (Throwable) null);
    }

    @Override
    public void fatal(final Marker marker, final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, messageSupplier, throwable);
    }

    @Override
    public void fatal(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.FATAL, null, messageSupplier, (Throwable) null);
    }

    @Override
    public void fatal(final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, messageSupplier, throwable);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2);
    }

    @Override
    public void fatal(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void fatal(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void fatal(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void fatal(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void fatal(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void fatal(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void fatal(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void fatal(final String message, final Object p0) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0);
    }

    @Override
    public void fatal(final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1);
    }

    @Override
    public void fatal(final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2);
    }

    @Override
    public void fatal(final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3);
    }

    @Override
    public void fatal(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void fatal(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void fatal(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void fatal(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void fatal(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void fatal(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <MF extends MessageFactory> MF getMessageFactory() {
        return (MF) messageFactory;
    }

    @Override
    public FlowMessageFactory getFlowMessageFactory() {
        return flowMessageFactory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void info(final Marker marker, final Message message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void info(final Marker marker, final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, message, throwable);
    }

    @Override
    public void info(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, null);
    }

    @Override
    public void info(final Marker marker, final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, message, throwable);
    }

    @Override
    public void info(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, null);
    }

    @Override
    public void info(final Marker marker, final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, message, throwable);
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
    public void info(final Marker marker, final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, message, throwable);
    }

    @Override
    public void info(final Message message) {
        logIfEnabled(FQCN, Level.INFO, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void info(final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, message, throwable);
    }

    @Override
    public void info(final CharSequence message) {
        logIfEnabled(FQCN, Level.INFO, null, message, null);
    }

    @Override
    public void info(final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, message, throwable);
    }

    @Override
    public void info(final Object message) {
        logIfEnabled(FQCN, Level.INFO, null, message, null);
    }

    @Override
    public void info(final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, message, throwable);
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
    public void info(final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.INFO, null, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.INFO, marker, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.INFO, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final Marker marker, final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.INFO, null, message, paramSuppliers);
    }

    @Override
    public void info(final Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.INFO, marker, messageSupplier, (Throwable) null);
    }

    @Override
    public void info(final Marker marker, final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, messageSupplier, throwable);
    }

    @Override
    public void info(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.INFO, null, messageSupplier, (Throwable) null);
    }

    @Override
    public void info(final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, messageSupplier, throwable);
    }

    @Override
    public void info(final Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0);
    }

    @Override
    public void info(final Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1);
    }

    @Override
    public void info(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2);
    }

    @Override
    public void info(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void info(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void info(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void info(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void info(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void info(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void info(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void info(final String message, final Object p0) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0);
    }

    @Override
    public void info(final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1);
    }

    @Override
    public void info(final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2);
    }

    @Override
    public void info(final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3);
    }

    @Override
    public void info(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void info(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void info(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void info(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void info(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void info(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
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
    public void log(final Level level, final Marker marker, final Message message) {
        logIfEnabled(FQCN, level, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void log(final Level level, final Marker marker, final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, level, marker, message, throwable);
    }

    @Override
    public void log(final Level level, final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, level, marker, message, (Throwable) null);
    }

    @Override
    public void log(final Level level, final Marker marker, final CharSequence message, final Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessage(FQCN, level, marker, message, throwable);
        }
    }

    @Override
    public void log(final Level level, final Marker marker, final Object message) {
        logIfEnabled(FQCN, level, marker, message, (Throwable) null);
    }

    @Override
    public void log(final Level level, final Marker marker, final Object message, final Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessage(FQCN, level, marker, message, throwable);
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
    public void log(final Level level, final Marker marker, final String message, final Throwable throwable) {
        logIfEnabled(FQCN, level, marker, message, throwable);
    }

    @Override
    public void log(final Level level, final Message message) {
        logIfEnabled(FQCN, level, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void log(final Level level, final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, level, null, message, throwable);
    }

    @Override
    public void log(final Level level, final CharSequence message) {
        logIfEnabled(FQCN, level, null, message, null);
    }

    @Override
    public void log(final Level level, final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, level, null, message, throwable);
    }

    @Override
    public void log(final Level level, final Object message) {
        logIfEnabled(FQCN, level, null, message, null);
    }

    @Override
    public void log(final Level level, final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, level, null, message, throwable);
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
    public void log(final Level level, final String message, final Throwable throwable) {
        logIfEnabled(FQCN, level, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, level, null, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, level, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, level, marker, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, level, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(
            final Level level, final Marker marker, final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, level, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, level, null, message, paramSuppliers);
    }

    @Override
    public void log(final Level level, final Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, level, marker, messageSupplier, (Throwable) null);
    }

    @Override
    public void log(
            final Level level, final Marker marker, final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, level, marker, messageSupplier, throwable);
    }

    @Override
    public void log(final Level level, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, level, null, messageSupplier, (Throwable) null);
    }

    @Override
    public void log(final Level level, final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, level, null, messageSupplier, throwable);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, level, marker, message, p0);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, level, marker, message, p0, p1);
    }

    @Override
    public void log(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2);
    }

    @Override
    public void log(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void log(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void log(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void log(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void log(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void log(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void log(
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void log(final Level level, final String message, final Object p0) {
        logIfEnabled(FQCN, level, null, message, p0);
    }

    @Override
    public void log(final Level level, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, level, null, message, p0, p1);
    }

    @Override
    public void log(final Level level, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2);
    }

    @Override
    public void log(
            final Level level,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3);
    }

    @Override
    public void log(
            final Level level,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void log(
            final Level level,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void log(
            final Level level,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void log(
            final Level level,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void log(
            final Level level,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void log(
            final Level level,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessageSafely(fqcn, level, marker, message, throwable);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final MessageSupplier messageSupplier,
            final Throwable throwable) {
        if (isEnabled(level, marker, messageSupplier, throwable)) {
            logMessage(fqcn, level, marker, messageSupplier, throwable);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final Object message,
            final Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessage(fqcn, level, marker, message, throwable);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final CharSequence message,
            final Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessage(fqcn, level, marker, message, throwable);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final Supplier<?> messageSupplier,
            final Throwable throwable) {
        if (isEnabled(level, marker, messageSupplier, throwable)) {
            logMessage(fqcn, level, marker, messageSupplier, throwable);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message) {
        if (isEnabled(level, marker, message)) {
            logMessage(fqcn, level, marker, message);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Supplier<?>... paramSuppliers) {
        if (isEnabled(level, marker, message)) {
            logMessage(fqcn, level, marker, message, paramSuppliers);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn, final Level level, final Marker marker, final String message, final Object... params) {
        if (isEnabled(level, marker, message, params)) {
            logMessage(fqcn, level, marker, message, params);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn, final Level level, final Marker marker, final String message, final Object p0) {
        if (isEnabled(level, marker, message, p0)) {
            logMessage(fqcn, level, marker, message, p0);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1) {
        if (isEnabled(level, marker, message, p0, p1)) {
            logMessage(fqcn, level, marker, message, p0, p1);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        if (isEnabled(level, marker, message, p0, p1, p2)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
        }
    }

    @Override
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessage(fqcn, level, marker, message, throwable);
        }
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final CharSequence message,
            final Throwable throwable) {
        logMessageSafely(fqcn, level, marker, messageFactory.newMessage(message), throwable);
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final Object message,
            final Throwable throwable) {
        logMessageSafely(fqcn, level, marker, messageFactory.newMessage(message), throwable);
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final MessageSupplier messageSupplier,
            final Throwable throwable) {
        final Message message = LambdaUtil.get(messageSupplier);
        final Throwable effectiveThrowable =
                (throwable == null && message != null) ? message.getThrowable() : throwable;
        logMessageSafely(fqcn, level, marker, message, effectiveThrowable);
    }

    @SuppressWarnings("deprecation")
    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final Supplier<?> messageSupplier,
            final Throwable throwable) {
        final Message message = LambdaUtil.getMessage(messageSupplier, messageFactory);
        final Throwable effectiveThrowable =
                (throwable == null && message != null) ? message.getThrowable() : throwable;
        logMessageSafely(fqcn, level, marker, message, effectiveThrowable);
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Throwable throwable) {
        logMessageSafely(fqcn, level, marker, messageFactory.newMessage(message), throwable);
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message) {
        final Message msg = messageFactory.newMessage(message);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn, final Level level, final Marker marker, final String message, final Object... params) {
        final Message msg = messageFactory.newMessage(message, params);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn, final Level level, final Marker marker, final String message, final Object p0) {
        final Message msg = messageFactory.newMessage(message, p0);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1) {
        final Message msg = messageFactory.newMessage(message, p0, p1);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5, p6);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    @SuppressWarnings("deprecation")
    protected void logMessage(
            final String fqcn,
            final Level level,
            final Marker marker,
            final String message,
            final Supplier<?>... paramSuppliers) {
        final Message msg = messageFactory.newMessage(message, LambdaUtil.getAll(paramSuppliers));
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    @Override
    public void logMessage(
            final Level level,
            final Marker marker,
            final String fqcn,
            final StackTraceElement location,
            final Message message,
            final Throwable throwable) {
        try {
            incrementRecursionDepth();
            log(level, marker, fqcn, location, message, throwable);
        } catch (Throwable ex) {
            handleLogMessageException(ex, fqcn, message);
        } finally {
            decrementRecursionDepth();
            ReusableMessageFactory.release(message);
        }
    }

    protected void log(
            final Level level,
            final Marker marker,
            final String fqcn,
            final StackTraceElement location,
            final Message message,
            final Throwable throwable) {
        logMessage(fqcn, level, marker, message, throwable);
    }

    @Override
    public void printf(final Level level, final Marker marker, final String format, final Object... params) {
        if (isEnabled(level, marker, format, params)) {
            final Message message = new StringFormattedMessage(format, params);
            logMessageSafely(FQCN, level, marker, message, message.getThrowable());
        }
    }

    @Override
    public void printf(final Level level, final String format, final Object... params) {
        if (isEnabled(level, null, format, params)) {
            final Message message = new StringFormattedMessage(format, params);
            logMessageSafely(FQCN, level, null, message, message.getThrowable());
        }
    }

    @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 30 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private void logMessageSafely(
            final String fqcn,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable throwable) {
        try {
            logMessageTrackRecursion(fqcn, level, marker, message, throwable);
        } finally {
            // LOG4J2-1583 prevent scrambled logs when logging calls are nested (logging in toString())
            ReusableMessageFactory.release(message);
        }
    }

    @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 33 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private void logMessageTrackRecursion(
            final String fqcn,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable throwable) {
        try {
            incrementRecursionDepth(); // LOG4J2-1518, LOG4J2-2031
            tryLogMessage(fqcn, getLocation(fqcn), level, marker, message, throwable);
        } finally {
            decrementRecursionDepth();
        }
    }

    private static int[] getRecursionDepthHolder() {
        int[] result = recursionDepthHolder.get();
        if (result == null) {
            result = new int[1];
            recursionDepthHolder.set(result);
        }
        return result;
    }

    private static void incrementRecursionDepth() {
        getRecursionDepthHolder()[0]++;
    }

    private static void decrementRecursionDepth() {
        final int newDepth = --getRecursionDepthHolder()[0];
        if (newDepth < 0) {
            throw new IllegalStateException("Recursion depth became negative: " + newDepth);
        }
    }

    /**
     * Returns the depth of nested logging calls in the current Thread: zero if no logging call has been made,
     * one if a single logging call without nested logging calls has been made, or more depending on the level of
     * nesting.
     * @return the depth of the nested logging calls in the current Thread
     */
    public static int getRecursionDepth() {
        return getRecursionDepthHolder()[0];
    }

    @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 26 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private void tryLogMessage(
            final String fqcn,
            final StackTraceElement location,
            final Level level,
            final Marker marker,
            final Message message,
            final Throwable throwable) {
        try {
            log(level, marker, fqcn, location, message, throwable);
        } catch (final Throwable t) {
            // LOG4J2-1990 Log4j2 suppresses all exceptions that occur once application called the logger
            handleLogMessageException(t, fqcn, message);
        }
    }

    @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 15 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private StackTraceElement getLocation(final String fqcn) {
        return requiresLocation() ? StackLocatorUtil.calcLocation(fqcn) : null;
    }

    // LOG4J2-1990 Log4j2 suppresses all exceptions that occur once application called the logger
    // TODO Configuration setting to propagate exceptions back to the caller *if requested*
    private void handleLogMessageException(final Throwable throwable, final String fqcn, final Message message) {
        if (throwable instanceof LoggingException) {
            throw (LoggingException) throwable;
        }
        StatusLogger.getLogger()
                .warn(
                        "{} caught {} logging {}: {}",
                        fqcn,
                        throwable.getClass().getName(),
                        message.getClass().getSimpleName(),
                        message.getFormat(),
                        throwable);
    }

    @Override
    public <T extends Throwable> T throwing(final T throwable) {
        return throwing(FQCN, Level.ERROR, throwable);
    }

    @Override
    public <T extends Throwable> T throwing(final Level level, final T throwable) {
        return throwing(FQCN, level, throwable);
    }

    /**
     * Logs a Throwable to be thrown.
     *
     * @param <T> the type of the Throwable.
     * @param fqcn the fully qualified class name of this Logger implementation.
     * @param level The logging Level.
     * @param throwable The Throwable.
     * @return the Throwable.
     */
    protected <T extends Throwable> T throwing(final String fqcn, final Level level, final T throwable) {
        if (isEnabled(level, THROWING_MARKER, (Object) null, null)) {
            logMessageSafely(fqcn, level, THROWING_MARKER, throwingMsg(throwable), throwable);
        }
        return throwable;
    }

    protected Message throwingMsg(final Throwable throwable) {
        return messageFactory.newMessage(THROWING);
    }

    @Override
    public void trace(final Marker marker, final Message message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void trace(final Marker marker, final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, throwable);
    }

    @Override
    public void trace(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, null);
    }

    @Override
    public void trace(final Marker marker, final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, throwable);
    }

    @Override
    public void trace(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, null);
    }

    @Override
    public void trace(final Marker marker, final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, throwable);
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
    public void trace(final Marker marker, final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, throwable);
    }

    @Override
    public void trace(final Message message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void trace(final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, message, throwable);
    }

    @Override
    public void trace(final CharSequence message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, null);
    }

    @Override
    public void trace(final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, message, throwable);
    }

    @Override
    public void trace(final Object message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, null);
    }

    @Override
    public void trace(final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, message, throwable);
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
    public void trace(final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.TRACE, null, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.TRACE, marker, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final Marker marker, final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.TRACE, null, message, paramSuppliers);
    }

    @Override
    public void trace(final Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.TRACE, marker, messageSupplier, (Throwable) null);
    }

    @Override
    public void trace(final Marker marker, final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, messageSupplier, throwable);
    }

    @Override
    public void trace(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.TRACE, null, messageSupplier, (Throwable) null);
    }

    @Override
    public void trace(final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, messageSupplier, throwable);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2);
    }

    @Override
    public void trace(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void trace(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void trace(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void trace(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void trace(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void trace(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void trace(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void trace(final String message, final Object p0) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0);
    }

    @Override
    public void trace(final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1);
    }

    @Override
    public void trace(final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2);
    }

    @Override
    public void trace(final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3);
    }

    @Override
    public void trace(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void trace(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void trace(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void trace(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void trace(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void trace(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public EntryMessage traceEntry() {
        return enter(FQCN, null, (Object[]) null);
    }

    @Override
    public EntryMessage traceEntry(final String format, final Object... params) {
        return enter(FQCN, format, params);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EntryMessage traceEntry(final Supplier<?>... paramSuppliers) {
        return enter(FQCN, null, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EntryMessage traceEntry(final String format, final Supplier<?>... paramSuppliers) {
        return enter(FQCN, format, paramSuppliers);
    }

    @Override
    public EntryMessage traceEntry(final Message message) {
        return enter(FQCN, message);
    }

    @Override
    public void traceExit() {
        exit(FQCN, null, null);
    }

    @Override
    public <R> R traceExit(final R result) {
        return exit(FQCN, null, result);
    }

    @Override
    public <R> R traceExit(final String format, final R result) {
        return exit(FQCN, format, result);
    }

    @Override
    public void traceExit(final EntryMessage message) {
        // If the message is null, traceEnter returned null because flow logging was disabled, we can optimize out
        // calling isEnabled().
        if (message != null && isEnabled(Level.TRACE, EXIT_MARKER, message, null)) {
            logMessageSafely(FQCN, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(message), null);
        }
    }

    @Override
    public <R> R traceExit(final EntryMessage message, final R result) {
        // If the message is null, traceEnter returned null because flow logging was disabled, we can optimize out
        // calling isEnabled().
        if (message != null && isEnabled(Level.TRACE, EXIT_MARKER, message, null)) {
            logMessageSafely(FQCN, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(result, message), null);
        }
        return result;
    }

    @Override
    public <R> R traceExit(final Message message, final R result) {
        // If the message is null, traceEnter returned null because flow logging was disabled, we can optimize out
        // calling isEnabled().
        if (message != null && isEnabled(Level.TRACE, EXIT_MARKER, message, null)) {
            logMessageSafely(FQCN, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(result, message), null);
        }
        return result;
    }

    @Override
    public void warn(final Marker marker, final Message message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void warn(final Marker marker, final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, message, throwable);
    }

    @Override
    public void warn(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, null);
    }

    @Override
    public void warn(final Marker marker, final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, message, throwable);
    }

    @Override
    public void warn(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, null);
    }

    @Override
    public void warn(final Marker marker, final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, message, throwable);
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
    public void warn(final Marker marker, final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, message, throwable);
    }

    @Override
    public void warn(final Message message) {
        logIfEnabled(FQCN, Level.WARN, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void warn(final Message message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, message, throwable);
    }

    @Override
    public void warn(final CharSequence message) {
        logIfEnabled(FQCN, Level.WARN, null, message, null);
    }

    @Override
    public void warn(final CharSequence message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, message, throwable);
    }

    @Override
    public void warn(final Object message) {
        logIfEnabled(FQCN, Level.WARN, null, message, null);
    }

    @Override
    public void warn(final Object message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, message, throwable);
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
    public void warn(final String message, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.WARN, null, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.WARN, marker, messageSupplier, (Throwable) null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.WARN, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final Marker marker, final Supplier<?> messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.WARN, null, message, paramSuppliers);
    }

    @Override
    public void warn(final Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.WARN, marker, messageSupplier, (Throwable) null);
    }

    @Override
    public void warn(final Marker marker, final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, messageSupplier, throwable);
    }

    @Override
    public void warn(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.WARN, null, messageSupplier, (Throwable) null);
    }

    @Override
    public void warn(final MessageSupplier messageSupplier, final Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, messageSupplier, throwable);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2);
    }

    @Override
    public void warn(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void warn(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void warn(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void warn(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void warn(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void warn(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void warn(
            final Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void warn(final String message, final Object p0) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0);
    }

    @Override
    public void warn(final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1);
    }

    @Override
    public void warn(final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2);
    }

    @Override
    public void warn(final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3);
    }

    @Override
    public void warn(
            final String message, final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void warn(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void warn(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void warn(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void warn(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void warn(
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    protected boolean requiresLocation() {
        return false;
    }

    /**
     * Construct a trace log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    @Override
    public LogBuilder atTrace() {
        return atLevel(Level.TRACE);
    }

    /**
     * Construct a debug log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    @Override
    public LogBuilder atDebug() {
        return atLevel(Level.DEBUG);
    }

    /**
     * Construct an informational log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    @Override
    public LogBuilder atInfo() {
        return atLevel(Level.INFO);
    }

    /**
     * Construct a warning log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    @Override
    public LogBuilder atWarn() {
        return atLevel(Level.WARN);
    }

    /**
     * Construct an error log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    @Override
    public LogBuilder atError() {
        return atLevel(Level.ERROR);
    }

    /**
     * Construct a fatal log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    @Override
    public LogBuilder atFatal() {
        return atLevel(Level.FATAL);
    }

    /**
     * Construct a log event that will always be logged.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    @Override
    public LogBuilder always() {
        return getLogBuilder(Level.OFF);
    }

    /**
     * Construct a log event.
     * @return a LogBuilder.
     * @since 2.13.0
     */
    @Override
    public LogBuilder atLevel(final Level level) {
        if (isEnabled(level)) {
            return getLogBuilder(level);
        }
        return LogBuilder.NOOP;
    }

    /**
     * Returns a log builder that logs at the specified level.
     *
     * @since 2.20.0
     */
    protected LogBuilder getLogBuilder(final Level level) {
        if (Constants.ENABLE_THREADLOCALS) {
            final DefaultLogBuilder builder = logBuilder.get();
            if (!builder.isInUse()) {
                return builder.reset(this, level);
            }
        }
        return new DefaultLogBuilder(this, level);
    }
}
