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
package org.apache.log4j.config;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

/**
 * Experimental ConfigurationFactory for Log4j 1.2 properties configuration files.
 */
// TODO
// @Plugin(name = "Log4j1ConfigurationFactory", category = ConfigurationFactory.CATEGORY)
//
// Best Value?
// @Order(50)
public class Log4j1ConfigurationFactory extends ConfigurationFactory {

    private static final String[] SUFFIXES = {".properties"};

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        final ConfigurationBuilder<BuiltConfiguration> builder;
        try (final InputStream configStream = source.getInputStream()) {
            builder = new Log4j1ConfigurationParser().buildConfigurationBuilder(configStream);
        } catch (final IOException e) {
            throw new ConfigurationException("Unable to load " + source, e);
        }
        return builder.build();
    }

    @Override
    protected String[] getSupportedTypes() {
        return SUFFIXES;
    }

}
