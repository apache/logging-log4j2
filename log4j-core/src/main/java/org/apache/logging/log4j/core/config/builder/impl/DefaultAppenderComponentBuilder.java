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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.jspecify.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A default implementation of the {@link AppenderComponentBuilder} interface for building
 * an {@link Appender} component for a Log4j configuration.
 *
 * <p>
 *   Note: This builder is not thread-safe. Instances should not be shared between threads.
 * </p>
 *
 * @since 2.4
 */
@ProviderType
class DefaultAppenderComponentBuilder extends DefaultComponentAndConfigurationBuilder<AppenderComponentBuilder>
        implements AppenderComponentBuilder {

    /**
     * Constructs a new component builder instance.
     *
     * @param builder    the configuration builder
     * @param pluginType the plugin-type of the appender component to build
     * @param name       the appender name
     * @throws NullPointerException if tthe {@code builder} argument is {@code null}
     */
    public DefaultAppenderComponentBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder,
            final String pluginType,
            final @Nullable String name) {
        super(builder, pluginType, name);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public AppenderComponentBuilder add(final LayoutComponentBuilder builder) {
        return addComponent(builder);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public AppenderComponentBuilder add(final FilterComponentBuilder builder) {
        return addComponent(builder);
    }
}
