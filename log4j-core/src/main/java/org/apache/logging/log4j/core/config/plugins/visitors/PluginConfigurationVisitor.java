/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package org.apache.logging.log4j.core.config.plugins.visitors;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;

/**
 * PluginVisitor implementation for {@link PluginConfiguration}.
 */
public class PluginConfigurationVisitor extends AbstractPluginVisitor<PluginConfiguration, Configuration> {
    public PluginConfigurationVisitor() {
        super(PluginConfiguration.class);
    }

    @Override
    public Object build() {
        if (this.conversionType.isInstance(configuration)) {
            debugLog.append("Configuration");
            if (configuration.getName() != null) {
                debugLog.append('(').append(configuration.getName()).append(')');
            }
            return configuration;
        }
        LOGGER.warn("Variable annotated with @PluginConfiguration is not compatible with type {}.",
                configuration.getClass());
        return null;
    }
}
