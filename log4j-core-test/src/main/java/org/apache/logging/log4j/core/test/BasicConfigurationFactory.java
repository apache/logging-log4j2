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
package org.apache.logging.log4j.core.test;

import java.net.URI;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 *
 */
public class BasicConfigurationFactory extends ConfigurationFactory {

    @Override
    public Configuration getConfiguration(
            final LoggerContext loggerContext, final String name, final URI configLocation) {
        return new BasicConfiguration(loggerContext);
    }

    @Override
    protected String[] getSupportedTypes() {
        return null;
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return null;
    }

    public static class BasicConfiguration extends AbstractConfiguration {

        private static final String DEFAULT_LEVEL = "org.apache.logging.log4j.level";

        public BasicConfiguration() {
            this(null);
        }

        public BasicConfiguration(final LoggerContext loggerContext) {
            super(
                    loggerContext,
                    ConfigurationSource.NULL_SOURCE,
                    loggerContext != null ? loggerContext.getEnvironment() : PropertiesUtil.getProperties(),
                    loggerContext != null
                            ? (ConfigurableInstanceFactory) loggerContext.getInstanceFactory()
                            : DI.createInitializedFactory());

            final LoggerConfig root = getRootLogger();
            final String name = System.getProperty(DEFAULT_LEVEL);
            final Level level = (name != null && Level.getLevel(name) != null) ? Level.getLevel(name) : Level.ERROR;
            root.setLevel(level);
        }
    }
}
