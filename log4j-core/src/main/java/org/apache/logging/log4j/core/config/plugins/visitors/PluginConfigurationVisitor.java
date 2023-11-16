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
package org.apache.logging.log4j.core.config.plugins.visitors;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;

/**
 * PluginVisitor implementation for {@link PluginConfiguration}.
 */
public class PluginConfigurationVisitor extends AbstractPluginVisitor<PluginConfiguration> {
    public PluginConfigurationVisitor() {
        super(PluginConfiguration.class);
    }

    @Override
    public Object visit(
            final Configuration configuration, final Node node, final LogEvent event, final StringBuilder log) {
        if (this.conversionType.isInstance(configuration)) {
            log.append("Configuration");
            if (configuration.getName() != null) {
                log.append('(').append(configuration.getName()).append(')');
            }
            return configuration;
        }
        LOGGER.warn(
                "Variable annotated with @PluginConfiguration is not compatible with type {}.",
                configuration.getClass());
        return null;
    }
}
