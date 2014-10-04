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

import org.apache.logging.log4j.message.MessageFactory;

/**
 * Anchor point for logging implementations.
 */
public interface LoggerContext {

    /**
     * An anchor for some other context, such as a ClassLoader or ServletContext.
     * @return The external context.
     */
    Object getExternalContext();

    /**
     * Returns a Logger.
     * @param name The name of the Logger to return.
     * @return The logger with the specified name.
     */
    ExtendedLogger getLogger(String name);

    /**
     * Returns a Logger.
     * @param name The name of the Logger to return.
     * @param messageFactory The message factory is used only when creating a logger, subsequent use does not change
     *                       the logger but will log a warning if mismatched.
     * @return The logger with the specified name.
     */
    ExtendedLogger getLogger(String name, MessageFactory messageFactory);

    /**
     * Detects if a Logger with the specified name exists.
     * @param name The Logger name to search for.
     * @return true if the Logger exists, false otherwise.
     */
    boolean hasLogger(String name);
}
