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
package org.apache.logging.log4j.core.config.properties;

import java.io.IOException;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Configuration created from a properties file.
 * @since 2.4
 */
public class PropertiesConfiguration extends BuiltConfiguration implements Reconfigurable {

    // ctor is called through reflection.
    public PropertiesConfiguration(
            final LoggerContext loggerContext, final ConfigurationSource source, final Component root) {
        super(loggerContext, source, root);
    }

    @Override
    public Configuration reconfigure() {
        try {
            final ConfigurationSource source = getConfigurationSource().resetInputStream();
            if (source == null) {
                return null;
            }
            final PropertiesConfigurationFactory factory = new PropertiesConfigurationFactory();
            final PropertiesConfiguration config = factory.getConfiguration(getLoggerContext(), source);
            return config == null || config.getState() != State.INITIALIZING ? null : config;
        } catch (final IOException ex) {
            LOGGER.error("Cannot locate file {}: {}", getConfigurationSource(), ex);
        }
        return null;
    }
}
