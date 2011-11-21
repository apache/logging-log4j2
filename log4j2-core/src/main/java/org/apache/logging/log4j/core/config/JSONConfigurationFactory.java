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

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.xml.sax.InputSource;

import java.io.File;
import java.net.URI;

/**
 *
 */
@Plugin(name="JSONConfigurationFactory", type="ConfigurationFactory")
@Order(6)
public class JSONConfigurationFactory extends XMLConfigurationFactory {

    public static final String DEFAULT_CONFIG_FILE = "log4j2.json";

    public static final String TEST_CONFIG_FILE = "log4j2-test.json";

    public static final String TEST_PREFIX = "log4j2-test";

    public static final String DEFAULT_PREFIX = "log4j2";

    public static final String SUFFIX = ".json";

    private File configFile = null;

    private String[] dependencies = new String[] {
        "org.codehaus.jackson.JsonNode",
        "org.codehaus.jackson.map.ObjectMapper"
    };

    private boolean isActive;

    public JSONConfigurationFactory() {
        try {
            for (String item : dependencies) {
                Class.forName(item);
            }
        } catch (ClassNotFoundException ex) {
            logger.debug("Missing dependencies for Json support");
            isActive = false;
            return;
        }
        isActive = true;
    }

    public Configuration getConfiguration(String name, URI configLocation) {
        InputSource source = null;
        if (configLocation != null) {
            source = getInputFromURI(configLocation);
        }
        if (source == null) {
            String testName;
            String defaultName;
            boolean named = (name != null && name.length() > 0);
            if (named) {
                testName = TEST_PREFIX + name + SUFFIX;
                defaultName = DEFAULT_PREFIX + name + SUFFIX;
            } else {
                testName = TEST_CONFIG_FILE;
                defaultName = DEFAULT_CONFIG_FILE;
            }
            ClassLoader loader = this.getClass().getClassLoader();
            source = getInputFromSystemProperty(loader, ".json");
            if (source == null) {
                source = getInputFromResource(testName, loader);
                if (source == null) {
                    source = getInputFromResource(defaultName, loader);
                }
                if (source == null) {
                    return named ? getConfiguration(null, null) : null;
                }
            }
        }
        return new JSONConfiguration(source, configFile);
    }
}
