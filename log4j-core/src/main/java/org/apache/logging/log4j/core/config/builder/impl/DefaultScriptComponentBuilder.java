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
import org.apache.logging.log4j.core.config.builder.api.ScriptComponentBuilder;
import org.apache.logging.log4j.core.script.Script;
import org.jspecify.annotations.Nullable;
import org.osgi.annotation.versioning.ProviderType;

/**
 * A default implementation of the {@link ScriptComponentBuilder} interface for building a {@link Script} component
 * for a Log4j configuration.
 *
 * <p>
 *   Note: This builder is not thread-safe. Instances should not be shared between threads.
 * </p>
 *
 * @since 2.5
 */
@ProviderType
class DefaultScriptComponentBuilder extends DefaultComponentAndConfigurationBuilder<ScriptComponentBuilder>
        implements ScriptComponentBuilder {

    /**
     * Constructs a new component builder instance.
     * @param builder the configuration builder
     * @param name the script name
     * @throws NullPointerException if the {@code builder} argument is {@code null}
     */
    public DefaultScriptComponentBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder, final @Nullable String name) {
        super(builder, "Script", name);
    }
}
