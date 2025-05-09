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

import static org.apache.logging.log4j.core.config.LoggerConfig.RootLogger;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderRefComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A default implementation of the {@link RootLoggerComponentBuilder} interface for building
 * a {@link RootLogger} component for a Log4j configuration.
 *
 * <p>
 *   Note: This builder is not thread-safe. Instances should not be shared between threads.
 * </p>
 *
 * @since 2.4
 */
@ProviderType
class DefaultRootLoggerComponentBuilder extends DefaultComponentAndConfigurationBuilder<RootLoggerComponentBuilder>
        implements RootLoggerComponentBuilder {

    /**
     * Create a new root logger component builder instance with the default plugin-type "{@code Root}".
     * @param builder the configuration builder.
     */
    public DefaultRootLoggerComponentBuilder(final DefaultConfigurationBuilder<? extends Configuration> builder) {
        this(builder, "Root");
    }

    /**
     * Create a new root logger component builder instance with the given plugin-type.
     * @param builder the configuration builder
     * @param pluginType the target plugin-type of the logger component
     */
    public DefaultRootLoggerComponentBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder, final String pluginType) {
        super(builder, pluginType, "");
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    @Override
    public RootLoggerComponentBuilder add(final AppenderRefComponentBuilder builder) {
        return addComponent(builder);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the given {@code builder} is {@code null}
     */
    @Override
    public RootLoggerComponentBuilder add(final FilterComponentBuilder builder) {
        return addComponent(builder);
    }
}
