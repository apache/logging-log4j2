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
package org.apache.logging.log4j.core.config.json;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Loader;

/**
 *
 */
@Plugin(name = "JSONConfigurationFactory", category = "ConfigurationFactory")
@Order(6)
public class JSONConfigurationFactory extends ConfigurationFactory {

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

    public JSONConfigurationFactory() {
        for (final String dependency : dependencies) {
            if (!Loader.isClassAvailable(dependency)) {
                LOGGER.debug("Missing dependencies for Json support");
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
    public Configuration getConfiguration(final ConfigurationSource source) {
        if (!isActive) {
            return null;
        }
        return new JSONConfiguration(source);
    }

    @Override
    public String[] getSupportedTypes() {
        return SUFFIXES;
    }
}
