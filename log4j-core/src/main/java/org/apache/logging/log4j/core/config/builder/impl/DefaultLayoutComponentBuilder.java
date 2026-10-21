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

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A default implementation of the {@link LayoutComponentBuilder} interface for building
 * a {@link Layout} component in a Log4j configuration.
 *
 * <p>
 *   Note: This builder is not thread-safe. Instances should not be shared between threads.
 * </p>
 *
 * @since 2.4
 */
@ProviderType
class DefaultLayoutComponentBuilder extends DefaultComponentAndConfigurationBuilder<LayoutComponentBuilder>
        implements LayoutComponentBuilder {

    /**
     * Create a new layout component builder instance with the given plugin-type.
     *
     * @param builder the configuration builder
     * @param pluginType the target plugin-type of the logger component
     * @throws NullPointerException if either the {@code builder} or {@code pluginType} argument is {@code null}
     */
    public DefaultLayoutComponentBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder, final String pluginType) {
        super(builder, pluginType);
    }
}
