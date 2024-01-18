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
package org.apache.logging.log4j.v3;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.simple.SimpleLoggerContextFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggingSystem;
import org.apache.logging.log4j.spi.Terminable;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StackLocatorUtil;

public final class LogManager {

    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();
    private static final String FQCN = "org.apache.logging.log4j.v3.LogManager";

    /**
     * Shutdown using the LoggerContext appropriate for the caller of this method.
     * This is equivalent to calling {@code LogManager.shutdown(false)}.
     * <p>
     * This call is synchronous and will block until shut down is complete.
     * This may include flushing pending log events over network connections.
     *
     * @since 2.6
     */
    public static void shutdown() {
        if (getContext() instanceof final Terminable terminable) {
            terminable.terminate();
        }
    }

    /**
     * Returns a Logger with the name of the calling class.
     *
     * @return The Logger for the calling class.
     * @throws UnsupportedOperationException if the calling class cannot be determined.
     */
    public static Logger getLogger() {
        return getLogger(StackLocatorUtil.getCallerClass(2));
    }

    /**
     * Returns a Logger using the fully qualified name of the Class as the Logger name.
     *
     * @param clazz The Class whose name should be used as the Logger name. If null it will default to the calling
     *              class.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code clazz} is {@code null} and the calling class cannot be
     *                                       determined.
     */
    public static Logger getLogger(final Class<?> clazz) {
        return getContext().getLogger(clazz, LoggingSystem.getMessageFactory());
    }

    /**
     * Returns a Logger using the fully qualified name of the Class as the Logger name.
     *
     * @param clazz          The Class whose name should be used as the Logger name. If null it will default to the calling
     *                       class.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the
     *                       logger but will log a warning if mismatched.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code clazz} is {@code null} and the calling class cannot be
     *                                       determined.
     */
    public static Logger getLogger(final Class<?> clazz, final MessageFactory messageFactory) {
        return getContext().getLogger(clazz, messageFactory);
    }

    /**
     * Returns a Logger with the name of the calling class.
     *
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the
     *                       logger but will log a warning if mismatched.
     * @return The Logger for the calling class.
     * @throws UnsupportedOperationException if the calling class cannot be determined.
     */
    public static Logger getLogger(final MessageFactory messageFactory) {
        return getLogger(StackLocatorUtil.getCallerClass(2), messageFactory);
    }

    /**
     * Returns a Logger with the specified name.
     *
     * @param name The logger name. If null the name of the calling class will be used.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code name} is {@code null} and the calling class cannot be determined.
     */
    public static Logger getLogger(final String name) {
        return getContext().getLogger(name, LoggingSystem.getMessageFactory());
    }

    /**
     * Returns a Logger with the specified name.
     *
     * @param name           The logger name. If null the name of the calling class will be used.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the
     *                       logger but will log a warning if mismatched.
     * @return The Logger.
     * @throws UnsupportedOperationException if {@code name} is {@code null} and the calling class cannot be determined.
     */
    public static Logger getLogger(final String name, final MessageFactory messageFactory) {
        return getContext().getLogger(name, messageFactory);
    }

    /**
     * Returns the root logger.
     *
     * @return the root logger.
     */
    public static Logger getRootLogger() {
        return getLogger("", LoggingSystem.getMessageFactory());
    }

    private static LoggerContext getContext() {
        try {
            return LoggingSystem.getLoggerContextFactory().getContext(FQCN, null, null, false, null, null);
        } catch (final IllegalStateException e) {
            LOGGER.warn("{} Using SimpleLogger", e.getMessage(), e);
        }
        return SimpleLoggerContextFactory.INSTANCE.getContext(FQCN, null, null, false, null, null);
    }

    private LogManager() {}
}
