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
package org.apache.logging.log4j.core.config.builder.impl;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.jspecify.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Extends {@code DefaultComponentBuilder} to specify {@code DefaultConfigurationBuilder<? extends Configuration>}
 * as the {@code ConfigurationBuilder} type.
 *
 * <p>
 *   Note: This builder is not thread-safe. Instances should not be shared between threads.
 * </p>
 *
 * @since 2.4
 */
@ProviderType
abstract class DefaultComponentAndConfigurationBuilder<T extends ComponentBuilder<T>>
        extends DefaultComponentBuilder<T, DefaultConfigurationBuilder<? extends Configuration>> {

    /**
     * Constructs a new instance with the given plugin-type and {@code null} name and value.
     * @param builder the configuration-builder
     * @param pluginType the plugin-type of the component being built
     * @throws NullPointerException if either the {@code builder} or {@code pluginType} arguments are {@code null}
     */
    protected DefaultComponentAndConfigurationBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder, final String pluginType) {

        this(builder, pluginType, null, null);
    }

    /**
     * Constructs a new instance with the given plugin-type, name and {@code null} value.
     * @param builder the configuration-builder
     * @param pluginType the plugin-type of the component being built
     * @param name the component name
     * @throws NullPointerException if either the {@code builder} or {@code pluginType} arguments are {@code null}
     */
    protected DefaultComponentAndConfigurationBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder,
            final String pluginType,
            final @Nullable String name) {

        this(builder, pluginType, name, null);
    }

    /**
     * Constructs a new instance with the given plugin-type, name and value.
     * @param builder the configuration-builder
     * @param pluginType the plugin-type of the component being built
     * @param name the component name
     * @param value the component value
     * @throws NullPointerException if either the {@code builder} or {@code pluginType} arguments are {@code null}
     */
    protected DefaultComponentAndConfigurationBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder,
            final String pluginType,
            final @Nullable String name,
            final @Nullable String value) {

        super(builder, pluginType, name, value);
    }
}
