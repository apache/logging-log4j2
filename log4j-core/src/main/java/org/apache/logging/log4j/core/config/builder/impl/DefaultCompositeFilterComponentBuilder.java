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
import org.apache.logging.log4j.core.config.builder.api.CompositeFilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A default implementation of the {@link CompositeFilterComponentBuilder} interface for building
 * a {@link CompositeFilter} component for a Log4j configuration.
 *
 * <p>
 *   Note: This builder is not thread-safe. Instances should not be shared between threads.
 * </p>
 *
 * @since 2.4
 */
@ProviderType
class DefaultCompositeFilterComponentBuilder
        extends DefaultComponentAndConfigurationBuilder<CompositeFilterComponentBuilder>
        implements CompositeFilterComponentBuilder {

    /**
     * Constructs a new instance with the given configurration builder and default plugin-type "{@code Filters}".
     * @param builder the configuration builder
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    public DefaultCompositeFilterComponentBuilder(final DefaultConfigurationBuilder<? extends Configuration> builder) {
        super(builder, "Filters");
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    @Override
    public CompositeFilterComponentBuilder add(final FilterComponentBuilder builder) {
        return addComponent(builder);
    }
}
