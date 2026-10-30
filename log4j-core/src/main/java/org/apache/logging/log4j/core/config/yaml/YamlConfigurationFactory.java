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
package org.apache.logging.log4j.core.config.yaml;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Loader;

@Plugin(name = "YamlConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(7)
public class YamlConfigurationFactory extends ConfigurationFactory {

    /**
     * The file extensions supported by this factory.
     */
    private static final String[] SUFFIXES = new String[] {".yml", ".yaml"};

    /**
     * Classes required for Jackson 3.x support.
     */
    private static final String[] JACKSON3_DEPENDENCIES = new String[] {
        "tools.jackson.databind.ObjectMapper",
        "tools.jackson.databind.JsonNode",
        "tools.jackson.dataformat.yaml.YAMLMapper"
    };

    /**
     * Classes required for Jackson 2.x support.
     */
    private static final String[] JACKSON2_DEPENDENCIES = new String[] {
        "com.fasterxml.jackson.databind.ObjectMapper",
        "com.fasterxml.jackson.databind.JsonNode",
        "com.fasterxml.jackson.core.JsonParser",
        "com.fasterxml.jackson.dataformat.yaml.YAMLFactory"
    };

    private final boolean isActive;

    private final boolean useJackson3;

    public YamlConfigurationFactory() {
        if (areClassesAvailable(JACKSON3_DEPENDENCIES)) {
            LOGGER.debug("Using Jackson 3.x for Yaml support");
            useJackson3 = true;
            isActive = true;
        } else if (areClassesAvailable(JACKSON2_DEPENDENCIES)) {
            LOGGER.debug("Using Jackson 2.x for Yaml support");
            useJackson3 = false;
            isActive = true;
        } else {
            LOGGER.debug(
                    "Missing dependencies for Yaml support, ConfigurationFactory {} is inactive",
                    getClass().getName());
            useJackson3 = false;
            isActive = false;
        }
    }

    private static boolean areClassesAvailable(final String[] classNames) {
        for (final String className : classNames) {
            if (!Loader.isClassAvailable(className)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean isActive() {
        return isActive;
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        if (!isActive()) {
            return null;
        }
        return useJackson3
                ? new Jackson3YamlConfiguration(loggerContext, source)
                : new YamlConfiguration(loggerContext, source);
    }

    @Override
    public String[] getSupportedTypes() {
        return SUFFIXES;
    }
}
