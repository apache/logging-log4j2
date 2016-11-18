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
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.MessageFactory2;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.LambdaUtil;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.Supplier;

/**
 * Base implementation of a Logger. It is highly recommended that any Logger implementation extend this class.
 */
public abstract class AbstractLogger implements ExtendedLogger, Serializable {

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
    public static final Marker THROWING_MARKER = MarkerManager.getMarker("THROWING").setParents(EXCEPTION_MARKER);

    /**
     * Marker for catching exceptions.
     */
    public static final Marker CATCHING_MARKER = MarkerManager.getMarker("CATCHING").setParents(EXCEPTION_MARKER);

    /**
     * The default MessageFactory class.
     */
    public static final Class<? extends MessageFactory> DEFAULT_MESSAGE_FACTORY_CLASS =
            createClassForProperty("log4j2.messageFactory", ReusableMessageFactory.class,
                    ParameterizedMessageFactory.class);

    /**
     * The default FlowMessageFactory class.
     */
    public static final Class<? extends FlowMessageFactory> DEFAULT_FLOW_MESSAGE_FACTORY_CLASS =
            createFlowClassForProperty("log4j2.flowMessageFactory", DefaultFlowMessageFactory.class);

    private static final long serialVersionUID = 2L;

    private static final String FQCN = AbstractLogger.class.getName();
    private static final String THROWING = "Throwing";
    private static final String CATCHING = "Catching";

    protected final String name;
    private final MessageFactory2 messageFactory;
    private final FlowMessageFactory flowMessageFactory;

    /**
     * Creates a new logger named after this class (or subclass).
     */
    public AbstractLogger() {
        this.name = getClass().getName();
        this.messageFactory = createDefaultMessageFactory();
        this.flowMessageFactory = createDefaultFlowMessageFactory();
    }

    /**
     * Creates a new named logger.
     *
     * @param name the logger name
     */
    public AbstractLogger(final String name) {
        this(name, createDefaultMessageFactory());
    }

    /**
     * Creates a new named logger with a particular {@link MessageFactory}.
     *
     * @param name the logger name
     * @param messageFactory the message factory, if null then use the default message factory.
     */
    public AbstractLogger(final String name, final MessageFactory messageFactory) {
        this.name = name;
        this.messageFactory = messageFactory == null ? createDefaultMessageFactory() : narrow(messageFactory);
        this.flowMessageFactory = createDefaultFlowMessageFactory();
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
            StatusLogger.getLogger().warn(
                    "The Logger {} was created with the message factory {} and is now requested with the "
                            + "message factory {}, which may create log events with unexpected formatting.", name,
                    loggerMessageFactory, messageFactory);
        } else if (messageFactory == null && !loggerMessageFactory.getClass().equals(DEFAULT_MESSAGE_FACTORY_CLASS)) {
            StatusLogger
                    .getLogger()
                    .warn("The Logger {} was created with the message factory {} and is now requested with a null "
                            + "message factory (defaults to {}), which may create log events with unexpected "
                            + "formatting.",
                            name, loggerMessageFactory, DEFAULT_MESSAGE_FACTORY_CLASS.getName());
        }
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
            logMessageSafely(fqcn, level, CATCHING_MARKER, catchingMsg(t), t);
        }
    }

    @Override
    public void catching(final Throwable t) {
        if (isEnabled(Level.ERROR, CATCHING_MARKER, (Object) null, null)) {
            logMessageSafely(FQCN, Level.ERROR, CATCHING_MARKER, catchingMsg(t), t);
        }
    }

    protected Message catchingMsg(final Throwable t) {
        return messageFactory.newMessage(CATCHING);
    }

    private static Class<? extends MessageFactory> createClassForProperty(final String property,
            final Class<ReusableMessageFactory> reusableParameterizedMessageFactoryClass,
            final Class<ParameterizedMessageFactory> parameterizedMessageFactoryClass) {
        try {
            final String fallback = Constants.ENABLE_THREADLOCALS ? reusableParameterizedMessageFactoryClass.getName()
                    : parameterizedMessageFactoryClass.getName();
            final String clsName = PropertiesUtil.getProperties().getStringProperty(property, fallback);
            return LoaderUtil.loadClass(clsName).asSubclass(MessageFactory.class);
        } catch (final Throwable t) {
            return parameterizedMessageFactoryClass;
        }
    }

    private static Class<? extends FlowMessageFactory> createFlowClassForProperty(final String property,
            final Class<DefaultFlowMessageFactory> defaultFlowMessageFactoryClass) {
        try {
            final String clsName = PropertiesUtil.getProperties().getStringProperty(property, defaultFlowMessageFactoryClass.getName());
            return LoaderUtil.loadClass(clsName).asSubclass(FlowMessageFactory.class);
        } catch (final Throwable t) {
            return defaultFlowMessageFactoryClass;
        }
    }

    private static MessageFactory2 createDefaultMessageFactory() {
        try {
            final MessageFactory result = DEFAULT_MESSAGE_FACTORY_CLASS.newInstance();
            return narrow(result);
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static MessageFactory2 narrow(final MessageFactory result) {
        if (result instanceof MessageFactory2) {
            return (MessageFactory2) result;
        }
        return new MessageFactory2Adapter(result);
    }

    private static FlowMessageFactory createDefaultFlowMessageFactory() {
        try {
            return DEFAULT_FLOW_MESSAGE_FACTORY_CLASS.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void debug(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, null);
    }

    @Override
    public void debug(final Marker marker, final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, t);
    }

    @Override
    public void debug(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.DEBUG, marker, msg, msg != null ? msg.getThrowable() : null);
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
        logIfEnabled(FQCN, Level.DEBUG, null, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void debug(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, msg, t);
    }

    @Override
    public void debug(final CharSequence message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, null);
    }

    @Override
    public void debug(final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, t);
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
    public void debug(final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void debug(final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, msgSupplier, t);
    }

    @Override
    public void debug(final Marker marker, final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void debug(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, paramSuppliers);
    }

    @Override
    public void debug(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, marker, msgSupplier, t);
    }

    @Override
    public void debug(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, paramSuppliers);
    }

    @Override
    public void debug(final Marker marker, final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void debug(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, marker, msgSupplier, t);
    }

    @Override
    public void debug(final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void debug(final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.DEBUG, null, msgSupplier, t);
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
    public void debug(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void debug(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
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
    public void debug(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void debug(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void debug(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void debug(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void debug(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void debug(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8, final Object p9) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    /**
     * Logs entry to a method with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param format Format String for the parameters.
     * @param paramSuppliers The Suppliers of the parameters.
     */
    protected EntryMessage enter(final String fqcn, final String format, final Supplier<?>... paramSuppliers) {
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
            logMessageSafely(fqcn, Level.TRACE, ENTRY_MARKER, entryMsg = entryMsg(format, params), null);
        }
        return entryMsg;
    }

    /**
     * Logs entry to a method with location information.
     *
     * @param fqcn The fully qualified class name of the <b>caller</b>.
     * @param msgSupplier The Supplier of the Message.
     */
    @Deprecated
    protected EntryMessage enter(final String fqcn, final MessageSupplier msgSupplier) {
        EntryMessage message = null;
        if (isEnabled(Level.TRACE, ENTRY_MARKER, (Object) null, null)) {
            logMessageSafely(fqcn, Level.TRACE, ENTRY_MARKER, message = flowMessageFactory.newEntryMessage(
                    msgSupplier.get()), null);
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
            logMessageSafely(fqcn, Level.TRACE, ENTRY_MARKER, flowMessage = flowMessageFactory.newEntryMessage(message),
                    null);
        }
        return flowMessage;
    }

    @Override
    public void entry() {
        entry(FQCN, (Object[]) null);
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
            if (params == null) {
                logMessageSafely(fqcn, Level.TRACE, ENTRY_MARKER, entryMsg(null, (Supplier<?>[]) null), null);
            } else {
                logMessageSafely(fqcn, Level.TRACE, ENTRY_MARKER, entryMsg(null, params), null);
            }
        }
    }

    protected EntryMessage entryMsg(final String format, final Object... params) {
        final int count = params == null ? 0 : params.length;
        if (count == 0) {
            if (Strings.isEmpty(format)) {
                return flowMessageFactory.newEntryMessage(null);
            }
            return flowMessageFactory.newEntryMessage(new SimpleMessage(format));
        }
        if (format != null) {
            return flowMessageFactory.newEntryMessage(new ParameterizedMessage(format, params));
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("params(");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            final Object parm = params[i];
            sb.append(parm instanceof Message ? ((Message) parm).getFormattedMessage() : String.valueOf(parm));
        }
        sb.append(')');
        return flowMessageFactory.newEntryMessage(new SimpleMessage(sb));
    }

    protected EntryMessage entryMsg(final String format, final MessageSupplier... paramSuppliers) {
        final int count = paramSuppliers == null ? 0 : paramSuppliers.length;
        final Object[] params = new Object[count];
        for (int i = 0; i < count; i++) {
            params[i] = paramSuppliers[i].get();
            params[i] = params[i] != null ? ((Message) params[i]).getFormattedMessage() : null;
        }
        return entryMsg(format, params);
    }

    protected EntryMessage entryMsg(final String format, final Supplier<?>... paramSuppliers) {
        final int count = paramSuppliers == null ? 0 : paramSuppliers.length;
        final Object[] params = new Object[count];
        for (int i = 0; i < count; i++) {
            params[i] = paramSuppliers[i].get();
            if (params[i] instanceof Message) {
                params[i] = ((Message) params[i]).getFormattedMessage();
            }
        }
        return entryMsg(format, params);
    }

    @Override
    public void error(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.ERROR, marker, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void error(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, msg, t);
    }

    @Override
    public void error(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, null);
    }

    @Override
    public void error(final Marker marker, final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, t);
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
        logIfEnabled(FQCN, Level.ERROR, null, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void error(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, msg, t);
    }

    @Override
    public void error(final CharSequence message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, null);
    }

    @Override
    public void error(final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, message, t);
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
    public void error(final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.ERROR, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void error(final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, msgSupplier, t);
    }

    @Override
    public void error(final Marker marker, final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.ERROR, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void error(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, paramSuppliers);
    }

    @Override
    public void error(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, msgSupplier, t);
    }

    @Override
    public void error(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.ERROR, null, message, paramSuppliers);
    }

    @Override
    public void error(final Marker marker, final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.ERROR, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void error(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, marker, msgSupplier, t);
    }

    @Override
    public void error(final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.ERROR, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void error(final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.ERROR, null, msgSupplier, t);
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
    public void error(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void error(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void error(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void error(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void error(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void error(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void error(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
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
    public void error(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void error(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void error(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void error(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void error(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void error(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7, final Object p8, final Object p9) {
        logIfEnabled(FQCN, Level.ERROR, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void exit() {
        exit(FQCN, (Object) null);
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
        logIfEnabled(fqcn, Level.TRACE, EXIT_MARKER, exitMsg(null, result), null);
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
        logIfEnabled(fqcn, Level.TRACE, EXIT_MARKER, exitMsg(format, result), null);
        return result;
    }

    protected Message exitMsg(final String format, final Object result) {
        if (result == null) {
            if (format == null) {
                return messageFactory.newMessage("Exit");
            }
            return messageFactory.newMessage("Exit: " + format);
        }
        if (format == null) {
            return messageFactory.newMessage("Exit with(" + result + ')');
        }
        return messageFactory.newMessage("Exit: " + format, result);

    }

    @Override
    public void fatal(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.FATAL, marker, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void fatal(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, msg, t);
    }

    @Override
    public void fatal(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, null);
    }

    @Override
    public void fatal(final Marker marker, final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, t);
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
        logIfEnabled(FQCN, Level.FATAL, null, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void fatal(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, msg, t);
    }

    @Override
    public void fatal(final CharSequence message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, null);
    }

    @Override
    public void fatal(final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, message, t);
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
    public void fatal(final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.FATAL, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void fatal(final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, msgSupplier, t);
    }

    @Override
    public void fatal(final Marker marker, final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.FATAL, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, paramSuppliers);
    }

    @Override
    public void fatal(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, msgSupplier, t);
    }

    @Override
    public void fatal(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.FATAL, null, message, paramSuppliers);
    }

    @Override
    public void fatal(final Marker marker, final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.FATAL, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void fatal(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, marker, msgSupplier, t);
    }

    @Override
    public void fatal(final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.FATAL, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void fatal(final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.FATAL, null, msgSupplier, t);
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
    public void fatal(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5, final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void fatal(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
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
    public void fatal(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void fatal(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void fatal(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void fatal(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void fatal(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void fatal(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8, final Object p9) {
        logIfEnabled(FQCN, Level.FATAL, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <MF extends MessageFactory> MF getMessageFactory() {
        return (MF) messageFactory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void info(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.INFO, marker, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void info(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, msg, t);
    }

    @Override
    public void info(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, null);
    }

    @Override
    public void info(final Marker marker, final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, message, t);
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
        logIfEnabled(FQCN, Level.INFO, null, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void info(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, msg, t);
    }

    @Override
    public void info(final CharSequence message) {
        logIfEnabled(FQCN, Level.INFO, null, message, null);
    }

    @Override
    public void info(final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, message, t);
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
    public void info(final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.INFO, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void info(final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, msgSupplier, t);
    }

    @Override
    public void info(final Marker marker, final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.INFO, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void info(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.INFO, marker, message, paramSuppliers);
    }

    @Override
    public void info(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, msgSupplier, t);
    }

    @Override
    public void info(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.INFO, null, message, paramSuppliers);
    }

    @Override
    public void info(final Marker marker, final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.INFO, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void info(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, marker, msgSupplier, t);
    }

    @Override
    public void info(final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.INFO, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void info(final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.INFO, null, msgSupplier, t);
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
    public void info(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void info(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void info(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void info(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void info(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5, final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void info(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void info(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
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
    public void info(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void info(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void info(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void info(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void info(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.INFO, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void info(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8, final Object p9) {
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
    public void log(final Level level, final Marker marker, final Message msg) {
        logIfEnabled(FQCN, level, marker, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void log(final Level level, final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, level, marker, msg, t);
    }

    @Override
    public void log(final Level level, final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, level, marker, message, (Throwable) null);
    }

    @Override
    public void log(final Level level, final Marker marker, final CharSequence message, final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            logMessage(FQCN, level, marker, message, t);
        }
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
        logIfEnabled(FQCN, level, null, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void log(final Level level, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, level, null, msg, t);
    }

    @Override
    public void log(final Level level, final CharSequence message) {
        logIfEnabled(FQCN, level, null, message, null);
    }

    @Override
    public void log(final Level level, final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, level, null, message, t);
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
    public void log(final Level level, final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, level, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void log(final Level level, final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, level, null, msgSupplier, t);
    }

    @Override
    public void log(final Level level, final Marker marker, final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, level, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, level, marker, message, paramSuppliers);
    }

    @Override
    public void log(final Level level, final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, level, marker, msgSupplier, t);
    }

    @Override
    public void log(final Level level, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, level, null, message, paramSuppliers);
    }

    @Override
    public void log(final Level level, final Marker marker, final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, level, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void log(final Level level, final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, level, marker, msgSupplier, t);
    }

    @Override
    public void log(final Level level, final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, level, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void log(final Level level, final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, level, null, msgSupplier, t);
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
    public void log(final Level level, final Marker marker, final String message, final Object p0, final Object p1,
            final Object p2) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object p0, final Object p1,
            final Object p2, final Object p3) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object p0, final Object p1,
            final Object p2, final Object p3, final Object p4) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object p0, final Object p1,
            final Object p2, final Object p3, final Object p4, final Object p5) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object p0, final Object p1,
            final Object p2, final Object p3, final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object p0, final Object p1,
            final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object p0, final Object p1,
            final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void log(final Level level, final Marker marker, final String message, final Object p0, final Object p1,
            final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
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
    public void log(final Level level, final String message, final Object p0, final Object p1, final Object p2, final Object p3) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3);
    }

    @Override
    public void log(final Level level, final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void log(final Level level, final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void log(final Level level, final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void log(final Level level, final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void log(final Level level, final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void log(final Level level, final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7, final Object p8, final Object p9) {
        logIfEnabled(FQCN, level, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final Message msg,
            final Throwable t) {
        if (isEnabled(level, marker, msg, t)) {
            logMessageSafely(fqcn, level, marker, msg, t);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker,
            final MessageSupplier msgSupplier, final Throwable t) {
        if (isEnabled(level, marker, msgSupplier, t)) {
            logMessage(fqcn, level, marker, msgSupplier, t);
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
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final CharSequence message,
            final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            logMessage(fqcn, level, marker, message, t);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final Supplier<?> msgSupplier,
            final Throwable t) {
        if (isEnabled(level, marker, msgSupplier, t)) {
            logMessage(fqcn, level, marker, msgSupplier, t);
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
            final Supplier<?>... paramSuppliers) {
        if (isEnabled(level, marker, message)) {
            logMessage(fqcn, level, marker, message, paramSuppliers);
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
            final Object p0) {
        if (isEnabled(level, marker, message, p0)) {
            logMessage(fqcn, level, marker, message, p0);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1) {
        if (isEnabled(level, marker, message, p0, p1)) {
            logMessage(fqcn, level, marker, message, p0, p1);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2) {
        if (isEnabled(level, marker, message, p0, p1, p2)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
        if (isEnabled(level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9)) {
            logMessage(fqcn, level, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
        }
    }

    @Override
    public void logIfEnabled(final String fqcn, final Level level, final Marker marker, final String message,
            final Throwable t) {
        if (isEnabled(level, marker, message, t)) {
            logMessage(fqcn, level, marker, message, t);
        }
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final CharSequence message,
            final Throwable t) {
        logMessageSafely(fqcn, level, marker, messageFactory.newMessage(message), t);
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final Object message,
            final Throwable t) {
        logMessageSafely(fqcn, level, marker, messageFactory.newMessage(message), t);
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker,
            final MessageSupplier msgSupplier, final Throwable t) {
        final Message message = LambdaUtil.get(msgSupplier);
        logMessageSafely(fqcn, level, marker, message, (t == null && message != null) ? message.getThrowable() : t);
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final Supplier<?> msgSupplier,
            final Throwable t) {
        final Message message = LambdaUtil.getMessage(msgSupplier, messageFactory);
        logMessageSafely(fqcn, level, marker, message, (t == null && message != null) ? message.getThrowable() : t);
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Throwable t) {
        logMessageSafely(fqcn, level, marker, messageFactory.newMessage(message), t);
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message) {
        final Message msg = messageFactory.newMessage(message);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object... params) {
        final Message msg = messageFactory.newMessage(message, params);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0) {
        final Message msg = messageFactory.newMessage(message, p0);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1) {
        final Message msg = messageFactory.newMessage(message, p0, p1);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5, p6);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Object p0, final Object p1, final Object p2, final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(final String fqcn, final Level level, final Marker marker, final String message,
            final Supplier<?>... paramSuppliers) {
        final Message msg = messageFactory.newMessage(message, LambdaUtil.getAll(paramSuppliers));
        logMessageSafely(fqcn, level, marker, msg, msg.getThrowable());
    }

    @Override
    public void printf(final Level level, final Marker marker, final String format, final Object... params) {
        if (isEnabled(level, marker, format, params)) {
            final Message msg = new StringFormattedMessage(format, params);
            logMessageSafely(FQCN, level, marker, msg, msg.getThrowable());
        }
    }

    @Override
    public void printf(final Level level, final String format, final Object... params) {
        if (isEnabled(level, null, format, params)) {
            final Message msg = new StringFormattedMessage(format, params);
            logMessageSafely(FQCN, level, null, msg, msg.getThrowable());
        }
    }

    private void logMessageSafely(final String fqcn, final Level level, final Marker marker, final Message msg,
            final Throwable throwable) {
        try {
            logMessage(fqcn, level, marker, msg, throwable);
        } finally {
            // LOG4J2-1583 prevent scrambled logs when logging calls are nested (logging in toString())
            ReusableMessageFactory.release(msg);
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
            logMessageSafely(fqcn, level, THROWING_MARKER, throwingMsg(t), t);
        }
        return t;
    }

    protected Message throwingMsg(final Throwable t) {
        return messageFactory.newMessage(THROWING);
    }

    @Override
    public void trace(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.TRACE, marker, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void trace(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, msg, t);
    }

    @Override
    public void trace(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, null);
    }

    @Override
    public void trace(final Marker marker, final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, t);
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
        logIfEnabled(FQCN, Level.TRACE, null, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void trace(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, msg, t);
    }

    @Override
    public void trace(final CharSequence message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, null);
    }

    @Override
    public void trace(final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, message, t);
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
    public void trace(final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.TRACE, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void trace(final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, msgSupplier, t);
    }

    @Override
    public void trace(final Marker marker, final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.TRACE, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void trace(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, paramSuppliers);
    }

    @Override
    public void trace(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, msgSupplier, t);
    }

    @Override
    public void trace(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.TRACE, null, message, paramSuppliers);
    }

    @Override
    public void trace(final Marker marker, final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.TRACE, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void trace(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, marker, msgSupplier, t);
    }

    @Override
    public void trace(final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.TRACE, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void trace(final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.TRACE, null, msgSupplier, t);
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
    public void trace(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5, final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void trace(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
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
    public void trace(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void trace(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void trace(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void trace(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void trace(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.TRACE, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void trace(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7, final Object p8, final Object p9) {
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
    public EntryMessage traceEntry(final Supplier<?>... paramSuppliers) {
        return enter(FQCN, null, paramSuppliers);
    }

    @Override
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
        // If the message is null, traceEnter returned null because flow logging was disabled, we can optimize out calling isEnabled().
        if (message != null && isEnabled(Level.TRACE, EXIT_MARKER, message, null)) {
            logMessageSafely(FQCN, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(message), null);
        }
    }

    @Override
    public <R> R traceExit(final EntryMessage message, final R result) {
        // If the message is null, traceEnter returned null because flow logging was disabled, we can optimize out calling isEnabled().
        if (message != null && isEnabled(Level.TRACE, EXIT_MARKER, message, null)) {
            logMessageSafely(FQCN, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(result, message), null);
        }
        return result;
    }

    @Override
    public <R> R traceExit(final Message message, final R result) {
        // If the message is null, traceEnter returned null because flow logging was disabled, we can optimize out calling isEnabled().
        if (message != null && isEnabled(Level.TRACE, EXIT_MARKER, message, null)) {
            logMessageSafely(FQCN, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(result, message), null);
        }
        return result;
    }

    @Override
    public void warn(final Marker marker, final Message msg) {
        logIfEnabled(FQCN, Level.WARN, marker, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void warn(final Marker marker, final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, msg, t);
    }

    @Override
    public void warn(final Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, null);
    }

    @Override
    public void warn(final Marker marker, final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, message, t);
    }

    @Override
    public void warn(final Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, null);
    }

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
        logIfEnabled(FQCN, Level.WARN, null, msg, msg != null ? msg.getThrowable() : null);
    }

    @Override
    public void warn(final Message msg, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, msg, t);
    }

    @Override
    public void warn(final CharSequence message) {
        logIfEnabled(FQCN, Level.WARN, null, message, null);
    }

    @Override
    public void warn(final CharSequence message, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, message, t);
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

    @Override
    public void warn(final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.WARN, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void warn(final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, msgSupplier, t);
    }

    @Override
    public void warn(final Marker marker, final Supplier<?> msgSupplier) {
        logIfEnabled(FQCN, Level.WARN, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void warn(final Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.WARN, marker, message, paramSuppliers);
    }

    @Override
    public void warn(final Marker marker, final Supplier<?> msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, msgSupplier, t);
    }

    @Override
    public void warn(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.WARN, null, message, paramSuppliers);
    }

    @Override
    public void warn(final Marker marker, final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.WARN, marker, msgSupplier, (Throwable) null);
    }

    @Override
    public void warn(final Marker marker, final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, marker, msgSupplier, t);
    }

    @Override
    public void warn(final MessageSupplier msgSupplier) {
        logIfEnabled(FQCN, Level.WARN, null, msgSupplier, (Throwable) null);
    }

    @Override
    public void warn(final MessageSupplier msgSupplier, final Throwable t) {
        logIfEnabled(FQCN, Level.WARN, null, msgSupplier, t);
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
    public void warn(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5, final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5, final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void warn(final Marker marker, final String message, final Object p0, final Object p1, final Object p2,
            final Object p3, final Object p4, final Object p5,
            final Object p6, final Object p7, final Object p8, final Object p9) {
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
    public void warn(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4);
    }

    @Override
    public void warn(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public void warn(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public void warn(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public void warn(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6, final Object p7, final Object p8) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public void warn(final String message, final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8, final Object p9) {
        logIfEnabled(FQCN, Level.WARN, null, message, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }
}
