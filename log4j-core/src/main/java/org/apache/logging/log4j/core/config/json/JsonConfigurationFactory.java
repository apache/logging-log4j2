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
package org.apache.logging.log4j.core.config.json;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Loader;

@Plugin(name = "JsonConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(6)
public class JsonConfigurationFactory extends ConfigurationFactory {

    /**
     * The file extensions supported by this factory.
     */
    private static final String[] SUFFIXES = new String[] {".json", ".jsn"};

    private static final String[] dependencies = new String[] {
        "com.fasterxml.jackson.databind.ObjectMapper",
        "com.fasterxml.jackson.databind.JsonNode",
        "com.fasterxml.jackson.core.JsonParser"
    };

    private final boolean isActive;

    public JsonConfigurationFactory() {
        for (final String dependency : dependencies) {
            if (!Loader.isClassAvailable(dependency)) {
                LOGGER.debug(
                        "Missing dependencies for Json support, ConfigurationFactory {} is inactive",
                        getClass().getName());
                isActive = false;
                return;
            }
        }
        isActive = true;
    }

    @Override
    protected boolean isActive() {
        return isActive;
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        if (!isActive) {
            return null;
        }
        return new JsonConfiguration(loggerContext, source);
    }

    @Override
    public String[] getSupportedTypes() {
        return SUFFIXES;
    }
}
