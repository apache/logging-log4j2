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
package org.apache.logging.log4j.kit.logger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.kit.logger.internal.DefaultLogBuilder;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.recycler.Recycler;
import org.apache.logging.log4j.spi.recycler.RecyclerFactory;
import org.apache.logging.log4j.util.LambdaUtil;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Base implementation of a Logger.
 * <p>
 *     It is highly recommended that any Logger implementation extend this class.
 * </p>
 * <h2>Implementation details</h2>
 * <h3>Control flow</h3>
 * <p>The typical control flow of a log message inside this class is:</p>
 * <ol>
 *     <li>Methods from the {@link Logger} interface call location-aware methods.</li>
 *     <li>
 *         Location-aware methods (mostly in the {@link ExtendedLogger} interface) have a {@code fqcn} parameter to
 *         allow logging bridge developers to provide fully qualified class name of the logger entry point, used to
 *         determine the caller class and method when location information needs to be logged.
 *     </li>
 *     <li>Filters methods are evaluated to check if the message should be logged.</li>
 *     <li>If the user didn't supply a {@link Message} parameter, a message factory is called.</li>
 *     <li>The {@link #logMessageSafely} method is invoked, which handles recursion and exceptions.</li>
 *     <li>Control is passed to the abstract {@link #doLog} method.</li>
 * </ol>
 * <h3>Performance</h3>
 * <p>
 *     Most methods in this class are tuned for performance. Specifically, try to keep the hot methods to 35
 *     bytecodes or less: this is within the MaxInlineSize threshold on Java 7 and Java 8 Hotspot and makes these
 *     methods candidates for immediate inlining instead of waiting until they are designated "hot enough".
 * </p>
 */
@NullMarked
public abstract class AbstractLogger implements ExtendedLogger {

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

    private static final String FQCN = AbstractLogger.class.getName();
    private static final String THROWING = "Throwing";
    private static final String CATCHING = "Catching";
    private static final Object[] EMPTY_PARAMS = new Object[0];
    private static final ThreadLocal<int[]> recursionDepthHolder = new ThreadLocal<>(); // LOG4J2-1518, LOG4J2-2031

    private final String name;
    private final MessageFactory messageFactory;
    private final FlowMessageFactory flowMessageFactory;
    private final Recycler<DefaultLogBuilder> recycler;
    private final Logger statusLogger;

    /**
     * @param name The name of the logger.
     * @param messageFactory The message factory to use for logging methods.
     * @param flowMessageFactory The flow message factory to use for flow logging methods.
     * @param recyclerFactory The recycler to use for log builder instances.
     */
    protected AbstractLogger(
            final String name,
            final MessageFactory messageFactory,
            final FlowMessageFactory flowMessageFactory,
            final RecyclerFactory recyclerFactory,
            final Logger statusLogger) {
        this.name = name;
        this.messageFactory = messageFactory;
        this.flowMessageFactory = flowMessageFactory;
        this.recycler = recyclerFactory.create(DefaultLogBuilder::new);
        this.statusLogger = statusLogger;
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
     * @implNote This method is used by all the other filtering methods.
     */
    @Override
    public abstract boolean isEnabled(Level level, @Nullable Marker marker);

    /**
     * Performs the actual logging
     *
     * @param fqcn      The fully qualified class name of the entry point to the log system, which can be used to
     *                  determine the location of the caller.
     * @param location  The location of the caller, if provided by the user or {@code null} otherwise.
     * @param level     The level of the log message.
     * @param marker    The marker of the log message or {@code null}.
     * @param message   The message to be logged.
     * @param throwable The exception associated to the message or {@code null}.
     */
    protected abstract void doLog(
            String fqcn,
            @Nullable StackTraceElement location,
            Level level,
            @Nullable Marker marker,
            @Nullable Message message,
            @Nullable Throwable throwable);

    /**
     * Returns a log builder that logs at the specified level.
     *
     * @since 2.20.0
     */
    protected LogBuilder getLogBuilder(final Level level) {
        final DefaultLogBuilder builder = recycler.acquire();
        return builder.reset(this, level);
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

    private static int[] getRecursionDepthHolder() {
        int[] result = recursionDepthHolder.get();
        if (result == null) {
            result = new int[1];
            recursionDepthHolder.set(result);
        }
        return result;
    }

    @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 30 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private void logMessageSafely(
            final String fqcn,
            final @Nullable StackTraceElement location,
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        try {
            logMessageTrackRecursion(fqcn, location, level, marker, message, throwable);
        } finally {
            // LOG4J2-1583 prevent scrambled logs when logging calls are nested (logging in toString())
            recycle(message);
        }
    }

    @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 33 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private void logMessageTrackRecursion(
            final String fqcn,
            final @Nullable StackTraceElement location,
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        try {
            incrementRecursionDepth(); // LOG4J2-1518, LOG4J2-2031
            tryLogMessage(fqcn, location, level, marker, message, throwable);
        } finally {
            decrementRecursionDepth();
        }
    }

    @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 15 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private @Nullable StackTraceElement getLocation(final String fqcn, final @Nullable StackTraceElement location) {
        return location != null ? location : requiresLocation() ? StackLocatorUtil.calcLocation(fqcn) : null;
    }

    @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 33 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private void recycle(final @Nullable Message message) {
        messageFactory.recycle(message);
    }

    @PerformanceSensitive
    // NOTE: This is a hot method. Current implementation compiles to 26 bytes of byte code.
    // This is within the 35 byte MaxInlineSize threshold. Modify with care!
    private void tryLogMessage(
            final String fqcn,
            final @Nullable StackTraceElement location,
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        try {
            doLog(fqcn, getLocation(fqcn, location), level, marker, message, throwable);
        } catch (final Throwable t) {
            // LOG4J2-1990 Log4j2 suppresses all exceptions that occur once application called the logger
            handleLogMessageException(t, fqcn, message);
        }
    }

    // LOG4J2-1990 Log4j2 suppresses all exceptions that occur once application called the logger
    // TODO Configuration setting to propagate exceptions back to the caller *if requested*
    private void handleLogMessageException(
            final Throwable throwable, final String fqcn, final @Nullable Message message) {
        if (throwable instanceof LoggingException) {
            throw (LoggingException) throwable;
        }
        statusLogger.warn(
                "{} caught {} logging {}: {}",
                fqcn,
                throwable.getClass().getName(),
                message != null ? message.getClass().getSimpleName() : null,
                message != null ? message.getFormat() : null,
                throwable);
    }

    protected boolean requiresLocation() {
        return false;
    }

    // <editor-fold desc="Unconditional logging">
    // Methods that always log a message regardless of the current logger level.

    // <editor-fold desc="Logging methods">
    @PerformanceSensitive
    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final CharSequence message,
            final @Nullable Throwable throwable) {
        final Message msg = messageFactory.newMessage(message);
        logMessageSafely(fqcn, null, level, marker, msg, throwable);
    }

    @PerformanceSensitive
    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final Object message,
            final @Nullable Throwable throwable) {
        final Message msg = messageFactory.newMessage(message);
        logMessageSafely(fqcn, null, level, marker, msg, throwable);
    }

    @SuppressWarnings("deprecation")
    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final MessageSupplier messageSupplier,
            final @Nullable Throwable throwable) {
        final Message msg = LambdaUtil.get(messageSupplier);
        final Throwable effectiveThrowable = (throwable == null && msg != null) ? msg.getThrowable() : throwable;
        logMessageSafely(fqcn, null, level, marker, msg, effectiveThrowable);
    }

    @SuppressWarnings("deprecation")
    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final Supplier<?> messageSupplier,
            final @Nullable Throwable throwable) {
        final Message msg = LambdaUtil.getMessage(messageSupplier, messageFactory);
        final Throwable effectiveThrowable = (throwable == null && msg != null) ? msg.getThrowable() : throwable;
        logMessageSafely(fqcn, null, level, marker, msg, effectiveThrowable);
    }

    @PerformanceSensitive
    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final @Nullable Throwable throwable) {
        final Message msg = messageFactory.newMessage(message);
        logMessageSafely(fqcn, null, level, marker, msg, throwable);
    }

    @PerformanceSensitive
    protected void logMessage(
            final String fqcn, final Level level, final @Nullable Marker marker, final String message) {
        final Message msg = messageFactory.newMessage(message);
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    @PerformanceSensitive
    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0) {
        final Message msg = messageFactory.newMessage(message, p0);
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    @PerformanceSensitive
    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1) {
        final Message msg = messageFactory.newMessage(message, p0, p1);
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2);
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3);
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4);
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5);
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        final Message msg = messageFactory.newMessage(message, p0, p1, p2, p3, p4, p5, p6);
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
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
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
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
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
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
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    @SuppressWarnings("deprecation")
    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Supplier<?>... paramSuppliers) {
        final Message msg = messageFactory.newMessage(message, LambdaUtil.getAll(paramSuppliers));
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }

    @PerformanceSensitive
    protected void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object... params) {
        final Message msg = messageFactory.newMessage(message, params);
        logMessageSafely(fqcn, null, level, marker, msg, msg.getThrowable());
    }
    // </editor-fold>

    // <editor-fold desc="Flow messages">
    protected @Nullable EntryMessage logEnterMessage(
            final String fqcn, final @Nullable String format, final @Nullable Object... params) {
        final EntryMessage entryMessage = flowMessageFactory.newEntryMessage(format, params);
        logMessageSafely(fqcn, null, Level.TRACE, ENTRY_MARKER, entryMessage, null);
        return entryMessage;
    }

    protected @Nullable EntryMessage logEnterMessage(final String fqcn, final @Nullable Message message) {
        final EntryMessage entryMessage = flowMessageFactory.newEntryMessage(message);
        logMessageSafely(fqcn, null, Level.TRACE, ENTRY_MARKER, entryMessage, null);
        return entryMessage;
    }

    protected void logExitMessage(
            final String fqcn, final @Nullable EntryMessage message, final @Nullable Object result) {
        logMessageSafely(
                fqcn, null, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(result, message), null);
    }

    protected void logExitMessage(final String fqcn, final @Nullable Message message, final @Nullable Object result) {
        logMessageSafely(
                fqcn, null, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(result, message), null);
    }

    protected void logExitMessage(final String fqcn, final @Nullable String format, final @Nullable Object result) {
        logMessageSafely(fqcn, null, Level.TRACE, EXIT_MARKER, flowMessageFactory.newExitMessage(format, result), null);
    }
    // </editor-fold>

    // <editor-fold desc="Miscellaneous">
    protected void logCatchingMessage(final String fqcn, final Level level, final @Nullable Throwable throwable) {
        logMessageSafely(fqcn, null, level, CATCHING_MARKER, messageFactory.newMessage(CATCHING), throwable);
    }

    protected void logPrintfMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String format,
            final Object... params) {
        final Message message = new StringFormattedMessage(format, params);
        logMessageSafely(fqcn, null, level, marker, message, message.getThrowable());
    }

    protected void logThrowingMessage(final String fqcn, final Level level, final Throwable throwable) {
        logMessageSafely(fqcn, null, level, THROWING_MARKER, messageFactory.newMessage(THROWING), throwable);
    }
    // </editor-fold>

    // </editor-fold>

    // <editor-fold desc="Filter methods">
    @Override
    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    @Override
    public boolean isDebugEnabled(final @Nullable Marker marker) {
        return isEnabled(Level.DEBUG, marker);
    }

    @Override
    public boolean isEnabled(final Level level) {
        return isEnabled(level, null);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final CharSequence message,
            final @Nullable Throwable throwable) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final Object message,
            final @Nullable Throwable throwable) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final @Nullable Throwable throwable) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final @Nullable Marker marker, final String message) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level, final @Nullable Marker marker, final String message, final Object... params) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(final Level level, final @Nullable Marker marker, final String message, final Object p0) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level, final @Nullable Marker marker, final String message, final Object p0, final Object p1) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
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
        return isEnabled(level, marker);
    }

    @Override
    public boolean isEnabled(
            final Level level,
            final @Nullable Marker marker,
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
        return isEnabled(level, marker);
    }

    @Override
    public boolean isErrorEnabled() {
        return isEnabled(Level.ERROR);
    }

    @Override
    public boolean isErrorEnabled(final @Nullable Marker marker) {
        return isEnabled(Level.ERROR, marker);
    }

    @Override
    public boolean isFatalEnabled() {
        return isEnabled(Level.FATAL);
    }

    @Override
    public boolean isFatalEnabled(final @Nullable Marker marker) {
        return isEnabled(Level.FATAL, marker);
    }

    @Override
    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO);
    }

    @Override
    public boolean isInfoEnabled(final @Nullable Marker marker) {
        return isEnabled(Level.INFO, marker);
    }

    @Override
    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE);
    }

    @Override
    public boolean isTraceEnabled(final @Nullable Marker marker) {
        return isEnabled(Level.TRACE, marker);
    }

    @Override
    public boolean isWarnEnabled() {
        return isEnabled(Level.WARN);
    }

    @Override
    public boolean isWarnEnabled(final @Nullable Marker marker) {
        return isEnabled(Level.WARN, marker);
    }
    // </editor-fold>

    // <editor-fold desc="Location-aware methods">

    // <editor-fold desc="Logging methods">
    // These methods are part of `ExtendedLogger`.
    @Override
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessageSafely(fqcn, null, level, marker, message, throwable);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final MessageSupplier messageSupplier,
            final @Nullable Throwable throwable) {
        if (isEnabled(level, marker, messageSupplier, throwable)) {
            logMessage(fqcn, level, marker, messageSupplier, throwable);
        }
    }

    @Override
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final Object message,
            final @Nullable Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessage(fqcn, level, marker, message, throwable);
        }
    }

    @Override
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final CharSequence message,
            final @Nullable Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessage(fqcn, level, marker, message, throwable);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final Supplier<?> messageSupplier,
            final @Nullable Throwable throwable) {
        if (isEnabled(level, marker, messageSupplier, throwable)) {
            logMessage(fqcn, level, marker, messageSupplier, throwable);
        }
    }

    @Override
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn, final Level level, final @Nullable Marker marker, final String message) {
        if (isEnabled(level, marker, message)) {
            logMessage(fqcn, level, marker, message);
        }
    }

    @Override
    @PerformanceSensitive
    @SuppressWarnings("deprecation")
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Supplier<?>... paramSuppliers) {
        if (isEnabled(level, marker, message)) {
            logMessage(fqcn, level, marker, message, paramSuppliers);
        }
    }

    @Override
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object... params) {
        if (isEnabled(level, marker, message, params)) {
            logMessage(fqcn, level, marker, message, params);
        }
    }

    @Override
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0) {
        if (isEnabled(level, marker, message, p0)) {
            logMessage(fqcn, level, marker, message, p0);
        }
    }

    @Override
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1) {
        if (isEnabled(level, marker, message, p0, p1)) {
            logMessage(fqcn, level, marker, message, p0, p1);
        }
    }

    @Override
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
    @PerformanceSensitive
    public void logIfEnabled(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final @Nullable Throwable throwable) {
        if (isEnabled(level, marker, message, throwable)) {
            logMessage(fqcn, level, marker, message, throwable);
        }
    }

    @Override
    @PerformanceSensitive
    public void logMessage(
            final String fqcn,
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        // This method does NOT check the level
        logMessageSafely(fqcn, null, level, marker, message, throwable);
    }

    @Override
    @PerformanceSensitive
    public void logMessage(
            final Level level,
            final @Nullable Marker marker,
            final String fqcn,
            final StackTraceElement location,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        // This method does NOT check the level
        logMessageSafely(fqcn, location, level, marker, message, throwable);
    }
    // </editor-fold>

    // </editor-fold>

    // <editor-fold desc="Public API">

    // <editor-fold desc="Fluent API">
    @Override
    public LogBuilder atTrace() {
        return atLevel(Level.TRACE);
    }

    @Override
    public LogBuilder atDebug() {
        return atLevel(Level.DEBUG);
    }

    @Override
    public LogBuilder atInfo() {
        return atLevel(Level.INFO);
    }

    @Override
    public LogBuilder atWarn() {
        return atLevel(Level.WARN);
    }

    @Override
    public LogBuilder atError() {
        return atLevel(Level.ERROR);
    }

    @Override
    public LogBuilder atFatal() {
        return atLevel(Level.FATAL);
    }

    @Override
    public LogBuilder always() {
        return getLogBuilder(Level.OFF);
    }

    @Override
    public LogBuilder atLevel(final Level level) {
        return isEnabled(level) ? getLogBuilder(level) : LogBuilder.NOOP;
    }
    // </editor-fold>

    // <editor-fold desc="Logging methods">
    @Override
    public void debug(final @Nullable Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, null);
    }

    @Override
    public void debug(final @Nullable Marker marker, final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, throwable);
    }

    @Override
    public void debug(final Marker marker, final @Nullable Message message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void debug(
            final @Nullable Marker marker, final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, throwable);
    }

    @Override
    public void debug(final @Nullable Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, null);
    }

    @Override
    public void debug(final @Nullable Marker marker, final Object message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, throwable);
    }

    @Override
    public void debug(final @Nullable Marker marker, final String message) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, (Throwable) null);
    }

    @Override
    public void debug(final @Nullable Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, params);
    }

    @Override
    public void debug(final @Nullable Marker marker, final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, throwable);
    }

    @Override
    public void debug(final @Nullable Message message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void debug(final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, throwable);
    }

    @Override
    public void debug(final CharSequence message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, null);
    }

    @Override
    public void debug(final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, throwable);
    }

    @Override
    public void debug(final Object message) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, null);
    }

    @Override
    public void debug(final Object message, final @Nullable Throwable throwable) {
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
    public void debug(final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final @Nullable Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final @Nullable Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(
            final @Nullable Marker marker, final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.DEBUG, null, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final @Nullable Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(
            final @Nullable Marker marker, final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.DEBUG, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void debug(final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.DEBUG, null, messageSupplier, throwable);
    }

    @Override
    public void debug(final @Nullable Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0);
    }

    @Override
    public void debug(final @Nullable Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1);
    }

    @Override
    public void debug(
            final @Nullable Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2);
    }

    @Override
    public void debug(
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.DEBUG, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void debug(
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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

    @Override
    public void error(final @Nullable Marker marker, final @Nullable Message message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void error(
            final @Nullable Marker marker, final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, throwable);
    }

    @Override
    public void error(final @Nullable Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, null);
    }

    @Override
    public void error(final @Nullable Marker marker, final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, throwable);
    }

    @Override
    public void error(final @Nullable Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, null);
    }

    @Override
    public void error(final @Nullable Marker marker, final Object message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, throwable);
    }

    @Override
    public void error(final @Nullable Marker marker, final String message) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, (Throwable) null);
    }

    @Override
    public void error(final @Nullable Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, params);
    }

    @Override
    public void error(final @Nullable Marker marker, final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, throwable);
    }

    @Override
    public void error(final @Nullable Message message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void error(final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, message, throwable);
    }

    @Override
    public void error(final CharSequence message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, null);
    }

    @Override
    public void error(final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, message, throwable);
    }

    @Override
    public void error(final Object message) {
        logIfEnabled(FQCN, Level.ERROR, null, message, null);
    }

    @Override
    public void error(final Object message, final @Nullable Throwable throwable) {
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
    public void error(final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.ERROR, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final @Nullable Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.ERROR, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final @Nullable Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(
            final @Nullable Marker marker, final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.ERROR, null, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final @Nullable Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.ERROR, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(
            final @Nullable Marker marker, final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.ERROR, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void error(final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.ERROR, null, messageSupplier, throwable);
    }

    @Override
    public void error(final @Nullable Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0);
    }

    @Override
    public void error(final @Nullable Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1);
    }

    @Override
    public void error(
            final @Nullable Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2);
    }

    @Override
    public void error(
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.ERROR, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void error(
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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

    @Override
    public void fatal(final @Nullable Marker marker, final @Nullable Message message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void fatal(
            final @Nullable Marker marker, final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, throwable);
    }

    @Override
    public void fatal(final @Nullable Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, null);
    }

    @Override
    public void fatal(final @Nullable Marker marker, final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, throwable);
    }

    @Override
    public void fatal(final @Nullable Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, null);
    }

    @Override
    public void fatal(final @Nullable Marker marker, final Object message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, throwable);
    }

    @Override
    public void fatal(final @Nullable Marker marker, final String message) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, (Throwable) null);
    }

    @Override
    public void fatal(final @Nullable Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, params);
    }

    @Override
    public void fatal(final @Nullable Marker marker, final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, throwable);
    }

    @Override
    public void fatal(final @Nullable Message message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void fatal(final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, message, throwable);
    }

    @Override
    public void fatal(final CharSequence message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, null);
    }

    @Override
    public void fatal(final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, message, throwable);
    }

    @Override
    public void fatal(final Object message) {
        logIfEnabled(FQCN, Level.FATAL, null, message, null);
    }

    @Override
    public void fatal(final Object message, final @Nullable Throwable throwable) {
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
    public void fatal(final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.FATAL, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final @Nullable Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.FATAL, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final @Nullable Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(
            final @Nullable Marker marker, final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.FATAL, null, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final @Nullable Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.FATAL, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(
            final @Nullable Marker marker, final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.FATAL, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void fatal(final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.FATAL, null, messageSupplier, throwable);
    }

    @Override
    public void fatal(final @Nullable Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0);
    }

    @Override
    public void fatal(final @Nullable Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1);
    }

    @Override
    public void fatal(
            final @Nullable Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2);
    }

    @Override
    public void fatal(
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.FATAL, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void fatal(
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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

    @Override
    public void info(final @Nullable Marker marker, final @Nullable Message message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void info(
            final @Nullable Marker marker, final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, message, throwable);
    }

    @Override
    public void info(final @Nullable Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, null);
    }

    @Override
    public void info(final @Nullable Marker marker, final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, message, throwable);
    }

    @Override
    public void info(final @Nullable Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, null);
    }

    @Override
    public void info(final @Nullable Marker marker, final Object message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, message, throwable);
    }

    @Override
    public void info(final @Nullable Marker marker, final String message) {
        logIfEnabled(FQCN, Level.INFO, marker, message, (Throwable) null);
    }

    @Override
    public void info(final @Nullable Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.INFO, marker, message, params);
    }

    @Override
    public void info(final @Nullable Marker marker, final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, message, throwable);
    }

    @Override
    public void info(final @Nullable Message message) {
        logIfEnabled(FQCN, Level.INFO, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void info(final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, message, throwable);
    }

    @Override
    public void info(final CharSequence message) {
        logIfEnabled(FQCN, Level.INFO, null, message, null);
    }

    @Override
    public void info(final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, message, throwable);
    }

    @Override
    public void info(final Object message) {
        logIfEnabled(FQCN, Level.INFO, null, message, null);
    }

    @Override
    public void info(final Object message, final @Nullable Throwable throwable) {
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
    public void info(final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.INFO, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final @Nullable Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.INFO, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final @Nullable Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.INFO, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(
            final @Nullable Marker marker, final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.INFO, null, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final @Nullable Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.INFO, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(
            final @Nullable Marker marker, final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.INFO, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void info(final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.INFO, null, messageSupplier, throwable);
    }

    @Override
    public void info(final @Nullable Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0);
    }

    @Override
    public void info(final @Nullable Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1);
    }

    @Override
    public void info(
            final @Nullable Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2);
    }

    @Override
    public void info(
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.INFO, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void info(
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
    public void log(final Level level, final @Nullable Marker marker, final @Nullable Message message) {
        logIfEnabled(FQCN, level, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void log(
            final Level level,
            final @Nullable Marker marker,
            final @Nullable Message message,
            final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, marker, message, throwable);
    }

    @Override
    public void log(final Level level, final @Nullable Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, level, marker, message, null);
    }

    @Override
    public void log(
            final Level level,
            final @Nullable Marker marker,
            final CharSequence message,
            final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, marker, message, throwable);
    }

    @Override
    public void log(final Level level, final @Nullable Marker marker, final Object message) {
        logIfEnabled(FQCN, level, marker, message, null);
    }

    @Override
    public void log(
            final Level level,
            final @Nullable Marker marker,
            final Object message,
            final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, marker, message, throwable);
    }

    @Override
    public void log(final Level level, final @Nullable Marker marker, final String message) {
        logIfEnabled(FQCN, level, marker, message, (Throwable) null);
    }

    @Override
    public void log(final Level level, final @Nullable Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, level, marker, message, params);
    }

    @Override
    public void log(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, marker, message, throwable);
    }

    @Override
    public void log(final Level level, final @Nullable Message message) {
        logIfEnabled(FQCN, level, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void log(final Level level, final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, null, message, throwable);
    }

    @Override
    public void log(final Level level, final CharSequence message) {
        logIfEnabled(FQCN, level, null, message, null);
    }

    @Override
    public void log(final Level level, final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, null, message, throwable);
    }

    @Override
    public void log(final Level level, final Object message) {
        logIfEnabled(FQCN, level, null, message, null);
    }

    @Override
    public void log(final Level level, final Object message, final @Nullable Throwable throwable) {
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
    public void log(final Level level, final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, level, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final @Nullable Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, level, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, level, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(
            final Level level,
            final @Nullable Marker marker,
            final Supplier<?> messageSupplier,
            final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, level, null, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final @Nullable Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, level, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(
            final Level level,
            final @Nullable Marker marker,
            final MessageSupplier messageSupplier,
            final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, level, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void log(final Level level, final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, level, null, messageSupplier, throwable);
    }

    @Override
    public void log(final Level level, final @Nullable Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, level, marker, message, p0);
    }

    @Override
    public void log(
            final Level level, final @Nullable Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, level, marker, message, p0, p1);
    }

    @Override
    public void log(
            final Level level,
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2) {
        logIfEnabled(FQCN, level, marker, message, p0, p1, p2);
    }

    @Override
    public void log(
            final Level level,
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
    public void trace(final @Nullable Marker marker, final @Nullable Message message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void trace(
            final @Nullable Marker marker, final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, throwable);
    }

    @Override
    public void trace(final @Nullable Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, null);
    }

    @Override
    public void trace(final @Nullable Marker marker, final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, throwable);
    }

    @Override
    public void trace(final @Nullable Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, null);
    }

    @Override
    public void trace(final @Nullable Marker marker, final Object message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, throwable);
    }

    @Override
    public void trace(final @Nullable Marker marker, final String message) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, (Throwable) null);
    }

    @Override
    public void trace(final @Nullable Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, params);
    }

    @Override
    public void trace(final @Nullable Marker marker, final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, throwable);
    }

    @Override
    public void trace(final @Nullable Message message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void trace(final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, message, throwable);
    }

    @Override
    public void trace(final CharSequence message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, null);
    }

    @Override
    public void trace(final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, message, throwable);
    }

    @Override
    public void trace(final Object message) {
        logIfEnabled(FQCN, Level.TRACE, null, message, null);
    }

    @Override
    public void trace(final Object message, final @Nullable Throwable throwable) {
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
    public void trace(final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.TRACE, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final @Nullable Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.TRACE, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final @Nullable Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(
            final @Nullable Marker marker, final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.TRACE, null, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final @Nullable Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.TRACE, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(
            final @Nullable Marker marker, final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.TRACE, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void trace(final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.TRACE, null, messageSupplier, throwable);
    }

    @Override
    public void trace(final @Nullable Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0);
    }

    @Override
    public void trace(final @Nullable Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1);
    }

    @Override
    public void trace(
            final @Nullable Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2);
    }

    @Override
    public void trace(
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.TRACE, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void trace(
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
    public void warn(final @Nullable Marker marker, final @Nullable Message message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void warn(
            final @Nullable Marker marker, final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, message, throwable);
    }

    @Override
    public void warn(final @Nullable Marker marker, final CharSequence message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, null);
    }

    @Override
    public void warn(final @Nullable Marker marker, final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, message, throwable);
    }

    @Override
    public void warn(final @Nullable Marker marker, final Object message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, null);
    }

    @Override
    public void warn(final @Nullable Marker marker, final Object message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, message, throwable);
    }

    @Override
    public void warn(final @Nullable Marker marker, final String message) {
        logIfEnabled(FQCN, Level.WARN, marker, message, (Throwable) null);
    }

    @Override
    public void warn(final @Nullable Marker marker, final String message, final Object... params) {
        logIfEnabled(FQCN, Level.WARN, marker, message, params);
    }

    @Override
    public void warn(final @Nullable Marker marker, final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, message, throwable);
    }

    @Override
    public void warn(final @Nullable Message message) {
        logIfEnabled(FQCN, Level.WARN, null, message, message != null ? message.getThrowable() : null);
    }

    @Override
    public void warn(final @Nullable Message message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, message, throwable);
    }

    @Override
    public void warn(final CharSequence message) {
        logIfEnabled(FQCN, Level.WARN, null, message, null);
    }

    @Override
    public void warn(final CharSequence message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, message, throwable);
    }

    @Override
    public void warn(final Object message) {
        logIfEnabled(FQCN, Level.WARN, null, message, null);
    }

    @Override
    public void warn(final Object message, final @Nullable Throwable throwable) {
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
    public void warn(final String message, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, message, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.WARN, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final @Nullable Marker marker, final Supplier<?> messageSupplier) {
        logIfEnabled(FQCN, Level.WARN, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final @Nullable Marker marker, final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.WARN, marker, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(
            final @Nullable Marker marker, final Supplier<?> messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final String message, final Supplier<?>... paramSuppliers) {
        logIfEnabled(FQCN, Level.WARN, null, message, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final @Nullable Marker marker, final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.WARN, marker, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(
            final @Nullable Marker marker, final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, marker, messageSupplier, throwable);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final MessageSupplier messageSupplier) {
        logIfEnabled(FQCN, Level.WARN, null, messageSupplier, null);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void warn(final MessageSupplier messageSupplier, final @Nullable Throwable throwable) {
        logIfEnabled(FQCN, Level.WARN, null, messageSupplier, throwable);
    }

    @Override
    public void warn(final @Nullable Marker marker, final String message, final Object p0) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0);
    }

    @Override
    public void warn(final @Nullable Marker marker, final String message, final Object p0, final Object p1) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1);
    }

    @Override
    public void warn(
            final @Nullable Marker marker, final String message, final Object p0, final Object p1, final Object p2) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2);
    }

    @Override
    public void warn(
            final @Nullable Marker marker,
            final String message,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        logIfEnabled(FQCN, Level.WARN, marker, message, p0, p1, p2, p3);
    }

    @Override
    public void warn(
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
            final @Nullable Marker marker,
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
    // </editor-fold>

    // <editor-fold desc="Flow messages">
    private @Nullable EntryMessage enter(final @Nullable String format, final @Nullable Object... params) {
        EntryMessage entryMessage = null;
        if (isEnabled(Level.TRACE, ENTRY_MARKER)) {
            entryMessage = logEnterMessage(FQCN, format, params);
        }
        return entryMessage;
    }

    @Override
    public final @Nullable EntryMessage traceEntry() {
        return enter(null, EMPTY_PARAMS);
    }

    @Override
    public final @Nullable EntryMessage traceEntry(final String format, final Object... params) {
        return enter(format, params);
    }

    @SuppressWarnings("deprecation")
    private @Nullable EntryMessage enter(final @Nullable String format, final Supplier<?>... paramSuppliers) {
        EntryMessage entryMessage = null;
        if (isEnabled(Level.TRACE, ENTRY_MARKER)) {
            entryMessage = logEnterMessage(FQCN, format, LambdaUtil.getAll(paramSuppliers));
        }
        return entryMessage;
    }

    @Override
    @SuppressWarnings("deprecation")
    public final @Nullable EntryMessage traceEntry(final Supplier<?>... paramSuppliers) {
        return enter(null, paramSuppliers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public final @Nullable EntryMessage traceEntry(final String format, final Supplier<?>... paramSuppliers) {
        return enter(format, paramSuppliers);
    }

    @Override
    public final @Nullable EntryMessage traceEntry(final @Nullable Message message) {
        EntryMessage entryMessage = null;
        if (isEnabled(Level.TRACE, ENTRY_MARKER)) {
            entryMessage = logEnterMessage(FQCN, message);
        }
        return entryMessage;
    }

    private <R> @Nullable R exit(final @Nullable String format, final @Nullable R result) {
        if (isEnabled(Level.TRACE, EXIT_MARKER)) {
            logExitMessage(FQCN, format, result);
        }
        return result;
    }

    @Override
    public final void traceExit() {
        exit((String) null, null);
    }

    @Override
    public final <R> @Nullable R traceExit(final @Nullable R result) {
        return exit((String) null, result);
    }

    @Override
    public final <R> @Nullable R traceExit(final String format, final @Nullable R result) {
        return exit(format, result);
    }

    private <R> @Nullable R exit(final @Nullable EntryMessage message, final @Nullable R result) {
        // If the message is null, traceEnter returned null because flow logging was disabled, we can optimize out
        // calling isEnabled().
        if (message != null && isEnabled(Level.TRACE, EXIT_MARKER, message, null)) {
            logExitMessage(FQCN, message, result);
        }
        return result;
    }

    @Override
    public final void traceExit(final EntryMessage message) {
        exit(message, null);
    }

    @Override
    public final <R> @Nullable R traceExit(final EntryMessage message, final @Nullable R result) {
        return exit(message, result);
    }

    @Override
    public final <R> @Nullable R traceExit(final @Nullable Message message, final @Nullable R result) {
        // If the message is null, traceEnter returned null because flow logging was disabled, we can optimize out
        // calling isEnabled().
        if (message != null && isEnabled(Level.TRACE, EXIT_MARKER, message, null)) {
            logExitMessage(FQCN, message, result);
        }
        return result;
    }

    @Override
    public void entry() {
        enter(null, EMPTY_PARAMS);
    }

    @Override
    public void entry(final Object... params) {
        enter(null, params);
    }

    @Override
    public void exit() {
        exit((String) null, null);
    }

    @Override
    public <R> R exit(final R result) {
        return exit((String) null, result);
    }
    // </editor-fold>

    // <editor-fold desc="Miscellaneous">
    @Override
    public final void catching(final Level level, final @Nullable Throwable throwable) {
        if (isEnabled(level, CATCHING_MARKER)) {
            logCatchingMessage(FQCN, level, throwable);
        }
    }

    @Override
    public final void catching(final @Nullable Throwable throwable) {
        catching(Level.ERROR, throwable);
    }

    @Override
    public final void printf(
            final Level level, final @Nullable Marker marker, final String format, final Object... params) {
        if (isEnabled(level, marker, format, params)) {
            logPrintfMessage(FQCN, level, marker, format, params);
        }
    }

    @Override
    public final void printf(final Level level, final String format, final Object... params) {
        printf(level, null, format, params);
    }

    @Override
    public final <T extends Throwable> T throwing(final T throwable) {
        return throwing(Level.ERROR, throwable);
    }

    @Override
    public final <T extends Throwable> T throwing(final Level level, final T throwable) {
        if (isEnabled(level, THROWING_MARKER)) {
            logThrowingMessage(FQCN, level, throwable);
        }
        return throwable;
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
    // </editor-fold>

    // </editor-fold>
}
