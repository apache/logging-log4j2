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
package org.apache.logging.log4j.core;

import java.io.Serializable;

/**
 * Appends {@link LogEvent}s. An Appender can contain a {@link Layout} if applicable as well
 * as an {@link ErrorHandler}. Typical Appender implementations coordinate with an
 * implementation of {@link org.apache.logging.log4j.core.appender.AbstractManager} to handle external resources
 * such as streams, connections, and other shared state. As Appenders are plugins, concrete implementations need to
 * be annotated with {@link org.apache.logging.log4j.core.config.plugins.Plugin} and need to provide a static
 * factory method annotated with {@link org.apache.logging.log4j.core.config.plugins.PluginFactory}.
 *
 * <p>Most core plugins are written using a related Manager class that handle the actual task of serializing a
 * {@link LogEvent} to some output location. For instance, many Appenders can take
 * advantage of the {@link org.apache.logging.log4j.core.appender.OutputStreamManager} class.</p>
 *
 * <p>It is recommended that Appenders don't do any heavy lifting since there can be many instances of the class
 * being used at any given time. When resources require locking (e.g., through {@link java.nio.channels.FileLock}),
 * it is important to isolate synchronized code to prevent concurrency issues.</p>
 */
public interface Appender extends LifeCycle {

    /**
     * Main {@linkplain org.apache.logging.log4j.core.config.plugins.Plugin#elementType() plugin element type} for
     * Appender plugins.
     *
     * @since 2.6
     */
    String ELEMENT_TYPE = "appender";

    /**
     * Empty array.
     */
    Appender[] EMPTY_ARRAY = {};

    /**
     * Logs a LogEvent using whatever logic this Appender wishes to use. It is typically recommended to use a
     * bridge pattern not only for the benefits from decoupling an Appender from its implementation, but it is also
     * handy for sharing resources which may require some form of locking.
     *
     * @param event The LogEvent.
     */
    void append(LogEvent event);

    /**
     * Gets the name of this Appender.
     *
     * @return name, may be null.
     */
    String getName();

    /**
     * Returns the Layout used by this Appender if applicable.
     *
     * @return the Layout for this Appender or {@code null} if none is configured.
     */
    Layout<? extends Serializable> getLayout();

    /**
     * Some Appenders need to propagate exceptions back to the application. When {@code ignoreExceptions} is
     * {@code false} the AppenderControl will allow the exception to percolate.
     *
     * @return {@code true} if exceptions will be logged but not thrown, {@code false} otherwise.
     */
    boolean ignoreExceptions();

    /**
     * Gets the {@link ErrorHandler} used for handling exceptions.
     *
     * @return the ErrorHandler for handling exceptions.
     */
    ErrorHandler getHandler();

    /**
     * Sets the {@link ErrorHandler} used for handling exceptions.
     *
     * @param handler the ErrorHandler to use for handling exceptions.
     */
    void setHandler(ErrorHandler handler);
}
