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
package org.apache.logging.log4j.core.config.builder.api;

import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.builder.impl.DefaultConfigurationBuilder;

/**
 * A {@link ConfigurationBuilder} factory which generates configuration-builders that build
 * a {@link BuiltConfiguration} or a derivative thereof.
 *
 * @since 2.4
 */
public abstract class ConfigurationBuilderFactory {

    /**
     * Returns a new default {@link ConfigurationBuilder} of type {@link BuiltConfiguration}
     * to construct Log4j configurations.
     *
     * @return the new configuration builder instance
     */
    public static ConfigurationBuilder<BuiltConfiguration> newConfigurationBuilder() {
        return new DefaultConfigurationBuilder<>();
    }

    /**
     * Returns a new default {@link ConfigurationBuilder} which builds a specific implementation of
     * {@link BuiltConfiguration}.
     *
     * @param <T> the {@code BuiltConfiguration} implementation type
     * @param clazz the class of the built-configuration implementation which should be built
     * @return the new configuration builder instance
     */
    public static <T extends BuiltConfiguration> ConfigurationBuilder<T> newConfigurationBuilder(final Class<T> clazz) {
        return new DefaultConfigurationBuilder<>(clazz);
    }
}
