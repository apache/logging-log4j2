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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.helpers.FileUtils;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 *
 */
@Plugin(name="XMLConfigurationFactory", type="ConfigurationFactory")
@Order(5)
public class XMLConfigurationFactory extends ConfigurationFactory {

    public static final String CONFIGURATION_FILE_PROPERTY = "log4j.configurationFile";

    public static final String DEFAULT_CONFIG_FILE = "log4j2.xml";

    public static final String TEST_CONFIG_FILE = "log4j2-test.xml";

    public static final String TEST_PREFIX = "log4j2-test";

    public static final String DEFAULT_PREFIX = "log4j2";

    public static final String SUFFIX = ".xml";

    protected static Logger logger = StatusLogger.getLogger();

    private File configFile = null;

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
            source = getInputFromSystemProperty(loader, null);
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
        return new XMLConfiguration(source, configFile);
    }

    protected InputSource getInputFromURI(URI configLocation) {
        configFile = FileUtils.fileFromURI(configLocation);
        if (configFile != null && configFile.exists() && configFile.canRead()) {
            try {
                InputSource source = new InputSource(new FileInputStream(configFile));
                source.setSystemId(configLocation.getPath());
                return source;
            } catch (FileNotFoundException ex) {
                logger.error("Cannot locate file " + configLocation.getPath(), ex);
            }
        }
        try {
            InputSource source = new InputSource(configLocation.toURL().openStream());
            source.setSystemId(configLocation.getPath());
            return source;
        } catch (MalformedURLException ex) {
            logger.error("Invalid URL " + configLocation.toString(), ex);
        } catch (IOException ex) {
            logger.error("Unabled to access " + configLocation.toString(), ex);
        }
        return null;
    }

    protected InputSource getInputFromSystemProperty(ClassLoader loader, String suffix) {
        String configFile = System.getProperty(CONFIGURATION_FILE_PROPERTY);
        if (configFile == null || (suffix != null && !configFile.toLowerCase().endsWith(suffix.toLowerCase()))) {
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

    protected InputSource getInputFromResource(String resource, ClassLoader loader) {
        InputStream is = Loader.getResourceAsStream(resource, loader);
        if (is == null) {
            return null;
        }
        InputSource source = new InputSource(is);
        source.setSystemId(resource);
        return source;
    }
}
