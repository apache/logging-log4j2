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
package org.apache.logging.log4j.config.spi;

import java.util.Map;

/**
 * Service provider interface for a logging configuration.
 * <p>
 * Implementations are provided by configuration backends (for example, {@code log4j-core}).
 * This interface uses generic types to avoid coupling SPI consumers to core implementation classes.
 * </p>
 *
 * @since 3.0.0
 */
public interface ConfigurationSPI {

    /** Key for storing the Context properties. */
    String CONTEXT_PROPERTIES = "ContextProperties";

    /**
     * Returns the configuration name.
     *
     * @return the name of the configuration
     */
    String getName();

    /**
     * Returns a map containing all appenders keyed by name.
     *
     * @return a map of appender names to appender instances
     */
    Map<String, ?> getAppenders();

    /**
     * Returns the appender with the specified name.
     *
     * @param <T>  the expected appender type
     * @param name the name of the appender
     * @return the appender with the specified name, or {@code null} if not found
     */
    <T> T getAppender(String name);

    /**
     * Locates the logger configuration for a logger name.
     *
     * @param <T>  the expected logger configuration type
     * @param name the logger name
     * @return the located logger configuration
     */
    <T> T getLoggerConfig(String name);

    /**
     * Returns the configuration properties.
     *
     * @return a map of property names to values
     */
    Map<String, String> getProperties();

    /**
     * Returns the root logger configuration.
     *
     * @param <T> the expected root logger configuration type
     * @return the root logger configuration
     */
    <T> T getRootLogger();
}
