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
package org.apache.logging.log4j.core.config;

import java.net.URI;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.plugins.di.Key;
import org.jspecify.annotations.Nullable;

/**
 * Creates configuration from an {@link URI}.
 */
public interface URIConfigurationFactory {

    Key<URIConfigurationFactory> KEY = Key.forClass(URIConfigurationFactory.class);

    String LOG4J2_VERSION = "2";

    /**
     * The prefix to use for automatic discovery of configuration files.
     */
    String getDefaultPrefix();

    /**
     * The prefix to use for automatic discovery of test configuration files.
     */
    String getTestPrefix();

    /**
     * A list of supported file extensions.
     * <p>
     *     Can contain {@code *}, if an attempt should be made to parse files with any file extension.
     * </p>
     */
    String[] getSupportedExtensions();

    /**
     * The version of the configuration format.
     * <p>
     *     Should return {@value LOG4J2_VERSION} for the Log4j 2.x configuration format.
     * </p>
     */
    String getVersion();

    /**
     * @param loggerContext The logger context to associate with the configuration.
     * @param source The source of the configuration.
     * @return A Configuration or {@code null}.
     */
    @Nullable
    Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source);

    /**
     * @param loggerContext The logger context to associate with the configuration.
     * @param name           The name of the logger context.
     * @param configLocation The configuration location or {@code null}
     * @return A Configuration or {@code null}.
     */
    @Nullable
    Configuration getConfiguration(LoggerContext loggerContext, String name, @Nullable URI configLocation);

    /**
     * @param loggerContext The logger context to associate with the configuration.
     * @param name           The name of the logger context.
     * @param configLocation The configuration location or {@code null}
     * @param loader The classloader to use to load resources.
     * @return A Configuration or {@code null}.
     */
    @Nullable
    Configuration getConfiguration(
            LoggerContext loggerContext, String name, @Nullable URI configLocation, ClassLoader loader);
}
