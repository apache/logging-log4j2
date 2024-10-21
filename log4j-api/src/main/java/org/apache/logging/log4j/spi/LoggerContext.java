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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MessageFactory;

/**
 * Anchor point for logging implementations.
 */
public interface LoggerContext {

    /**
     * Empty array.
     */
    LoggerContext[] EMPTY_ARRAY = {};

    /**
     * Gets the anchor for some other context, such as a ClassLoader or ServletContext.
     * @return The external context.
     */
    Object getExternalContext();

    /**
     * Gets an ExtendedLogger using the fully qualified name of the Class as the Logger name.
     * @param cls The Class whose name should be used as the Logger name.
     * @return The logger.
     * @since 2.14.0
     */
    default ExtendedLogger getLogger(Class<?> cls) {
        final String canonicalName = cls.getCanonicalName();
        return getLogger(canonicalName != null ? canonicalName : cls.getName());
    }

    /**
     * Gets an ExtendedLogger using the fully qualified name of the Class as the Logger name.
     * @param cls The Class whose name should be used as the Logger name.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change the
     *                       logger but will log a warning if mismatched.
     * @return The logger.
     * @since 2.14.0
     */
    default ExtendedLogger getLogger(Class<?> cls, MessageFactory messageFactory) {
        final String canonicalName = cls.getCanonicalName();
        return getLogger(canonicalName != null ? canonicalName : cls.getName(), messageFactory);
    }

    /**
     * Gets an ExtendedLogger.
     * @param name The name of the Logger to return.
     * @return The logger with the specified name.
     */
    ExtendedLogger getLogger(String name);

    /**
     * Gets an ExtendedLogger.
     * @param name The name of the Logger to return.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change
     *                       the logger but will log a warning if mismatched.
     * @return The logger with the specified name.
     */
    ExtendedLogger getLogger(String name, MessageFactory messageFactory);

    /**
     * Gets the LoggerRegistry.
     *
     * @return the LoggerRegistry.
     * @since 2.17.2
     */
    default LoggerRegistry<? extends Logger> getLoggerRegistry() {
        return null;
    }

    /**
     * Gets an object by its name.
     * @param key The object's key.
     * @return The Object that is associated with the key, if any.
     * @since 2.13.0
     */
    default Object getObject(String key) {
        return null;
    }

    /**
     * Tests if a Logger with the specified name exists.
     * @param name The Logger name to search for.
     * @return true if the Logger exists, false otherwise.
     */
    boolean hasLogger(String name);

    /**
     * Tests if a Logger with the specified name and MessageFactory type exists.
     * @param name The Logger name to search for.
     * @param messageFactoryClass The message factory class to search for.
     * @return true if the Logger exists, false otherwise.
     * @since 2.5
     */
    boolean hasLogger(String name, Class<? extends MessageFactory> messageFactoryClass);

    /**
     * Tests if a Logger with the specified name and MessageFactory exists.
     * @param name The Logger name to search for.
     * @param messageFactory The message factory to search for.
     * @return true if the Logger exists, false otherwise.
     * @since 2.5
     */
    boolean hasLogger(String name, MessageFactory messageFactory);

    /**
     * Associates an object into the LoggerContext by name for later use.
     * @param key The object's key.
     * @param value The object.
     * @return The previous object or null.
     * @since 2.13.0
     */
    default Object putObject(String key, Object value) {
        return null;
    }

    /**
     * Associates an object into the LoggerContext by name for later use if an object is not already stored with that key.
     * @param key The object's key.
     * @param value The object.
     * @return The previous object or null.
     * @since 2.13.0
     */
    default Object putObjectIfAbsent(String key, Object value) {
        return null;
    }

    /**
     * Removes an object if it is present.
     * @param key The object's key.
     * @return The object if it was present, null if it was not.
     * @since 2.13.0
     */
    default Object removeObject(String key) {
        return null;
    }

    /**
     * Removes an object if it is present and the provided object is stored.
     * @param key The object's key.
     * @param value The object.
     * @return The object if it was present, null if it was not.
     * @since 2.13.0
     */
    default boolean removeObject(String key, Object value) {
        return false;
    }
}
