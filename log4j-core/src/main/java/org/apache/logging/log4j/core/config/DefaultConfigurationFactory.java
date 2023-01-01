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
package org.apache.logging.log4j.core.config;

import java.net.URI;

import org.apache.logging.log4j.core.LoggerContext;

/**
 * Default implementation of {@link ConfigurationFactory} which dynamically selects a factory from installed plugins
 * that supports a given configuration source.
 */
public class DefaultConfigurationFactory extends ConfigurationFactory {

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
        return getProvider(loggerContext).getConfiguration(name, configLocation);
    }

    @Override
    public String[] getSupportedTypes() {
        return null;
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return getProvider(loggerContext).getConfiguration(source);
    }

    private static ConfigurationProvider getProvider(final LoggerContext loggerContext) {
        final LoggerContext context = loggerContext != null ? loggerContext : LoggerContext.getContext();
        return context.getInstance(ConfigurationProvider.class);
    }

}
