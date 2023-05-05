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
package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;

/**
 * A type of builder that can be used to configure and create a instances using a Java DSL instead of
 * through a configuration file. These builders are primarily useful for internal code and unit tests, but they can
 * technically be used as a verbose alternative to configuration files.
 *
 * <p>
 *     When creating <em>plugin</em> builders, it is customary to create the builder class as a public static inner class
 *     called {@code Builder}. For instance, the builder class for
 *     {@link org.apache.logging.log4j.core.layout.PatternLayout PatternLayout} would be
 *     {@code PatternLayout.Builder}.
 * </p>
 *
 * @param <T> This builder creates instances of this class.
 */
public interface Builder<T> {

    /**
     * Builds the object after all configuration has been set. This will use default values for any
     * unspecified attributes for the object.
     *
     * @return the configured instance.
     * @throws org.apache.logging.log4j.core.config.ConfigurationException if there was an error building the
     * object.
     */
    T build();

    default boolean isValid() {
        return PluginBuilder.validateFields(this, getErrorPrefix());
    }

    /**
     * Prefix to use to report errors from this builder.
     *
     * @return The prefix of all logged errors.
     */
    default String getErrorPrefix() {
        return "Component";
    }
}
