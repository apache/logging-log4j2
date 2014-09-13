/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.core.util;

/**
 * A type of Plugin builder that can be used to configure and create a plugin instance using a Java DSL instead of
 * through a configuration file. These builders are primarily useful for internal code and unit tests, but they can
 * technically be used as a verbose alternative to configuration files.
 *
 * <p>
 *     When creating plugin builders, it is customary to create the builder class as a public static inner class
 *     called {@code Builder}. For instance, the builder class for
 *     {@link org.apache.logging.log4j.core.layout.PatternLayout PatternLayout} would be
 *     {@code PatternLayout.Builder}.
 * </p>
 *
 * @param <T> the Plugin class this is a builder for.
 */
public interface Builder<T> {

    /**
     * Builds the plugin object after all configuration has been set. This will use default values for any
     * unspecified attributes for the plugin.
     *
     * @return the configured plugin instance.
     * @throws org.apache.logging.log4j.core.config.ConfigurationException if there was an error building the plugin
     * object.
     */
    T build();
}
