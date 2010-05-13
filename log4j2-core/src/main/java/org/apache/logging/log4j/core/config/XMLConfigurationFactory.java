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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

/**
 *
 */
@Plugin(name="XMLConfigurationFactory", type="ConfigurationFactory")
@Order(1)
public class XMLConfigurationFactory extends ConfigurationFactory {

    public static final String CONFIGURATION_FILE_PROPERTY = "log4j.configurationFile";

    public static final String DEFAULT_CONFIG_FILE = "log4j2.xml";

    public static final String TEST_CONFIG_FILE = "log4j2-test.xml";

    public Configuration getConfiguration() {
        ClassLoader loader = this.getClass().getClassLoader();
        InputSource source = getInputFromSystemProperty(loader);
        if (source == null) {
            source = getInputFromResource(TEST_CONFIG_FILE, loader);
            if (source == null) {
                source = getInputFromResource(DEFAULT_CONFIG_FILE, loader);
            }
            if (source == null) {
                return null;
            }
        }
        return new XMLConfiguration(source);
    }

    private InputSource getInputFromSystemProperty(ClassLoader loader) {
        String configFile = System.getProperty(CONFIGURATION_FILE_PROPERTY);
        if (configFile == null) {
            return null;
        }
        InputSource source;
        try {
            URL url = new URL(configFile);
            source = new InputSource(url.openStream());
            source.setSystemId(configFile);
            return source;
        } catch (Exception ex) {
            source = getInputFromResource(configFile, loader);
            if (source == null) {
                try {
                    InputStream is = new FileInputStream(configFile);
                    source = new InputSource(is);
                    source.setSystemId(configFile);
                } catch (FileNotFoundException fnfe) {
                    // Ignore the exception
                }
            }
        }
        return source;
    }

    private InputSource getInputFromResource(String resource, ClassLoader loader) {
        InputStream is = loader.getResourceAsStream(resource);
        if (is == null) {
            return null;
        }
        InputSource source = new InputSource(is);
        source.setSystemId(resource);
        return source;
    }
}
