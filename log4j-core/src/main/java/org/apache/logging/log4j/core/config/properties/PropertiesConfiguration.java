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
package org.apache.logging.log4j.core.config.properties;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.assembler.api.Component;
import org.apache.logging.log4j.core.config.assembler.impl.AssembledConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Configuration created from a properties file.
 */
public class PropertiesConfiguration extends AssembledConfiguration implements Reconfigurable {

    private static final long serialVersionUID = 5198216024278070407L;

    public PropertiesConfiguration(ConfigurationSource source, Component root) {
        super(source, root);
    }

    @Override
    public Configuration reconfigure() {
        try {
            final ConfigurationSource source = getConfigurationSource().resetInputStream();
            if (source == null) {
                return null;
            }
            final PropertiesConfigurationFactory factory = new PropertiesConfigurationFactory();
            final PropertiesConfiguration config = factory.getConfiguration(source);
            return config.root.getComponents().size() == 0 ? null : config;
        } catch (final IOException ex) {
            LOGGER.error("Cannot locate file {}", getConfigurationSource(), ex);
        }
        return null;
    }


}
