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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    /**
     * Returns the file extensions supported by this Properties configuration factory.
     *
     * <p>This method returns Properties file extensions that this factory can process.
     * The factory supports standard Java properties files used for Log4j 2 configuration.
     * No external dependencies are required for properties file support.</p>
     *
     * @return a list containing "properties" extension if the factory is active,
     *         empty list if inactive
     */
    @Override
    public List<String> getSupportedFileExtensions() {
        if (!isActive()) {
            return Collections.emptyList();
        }
        return Arrays.asList("properties");
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
