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
import java.io.InputStream;
import java.util.Properties;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * Creates a PropertiesConfiguration from a properties file.
 *
 * @since 2.4
 */
@Plugin(name = "PropertiesConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(8)
public class PropertiesConfigurationFactory extends ConfigurationFactory {

    @Override
    protected String[] getSupportedTypes() {
        return new String[] {".properties"};
    }

    @Override
    public PropertiesConfiguration getConfiguration(
            final LoggerContext loggerContext, final ConfigurationSource source) {
        final Properties properties = new Properties();
        try (final InputStream configStream = source.getInputStream()) {
            properties.load(configStream);
        } catch (final IOException ioe) {
            throw new ConfigurationException("Unable to load " + source.toString(), ioe);
        }
        return new PropertiesConfigurationBuilder()
                .setConfigurationSource(source)
                .setRootProperties(properties)
                .setLoggerContext(loggerContext)
                .build();
    }
}
