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
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginType;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * ConfigurationFactory allows the configuration implementation to be
 * dynamically chosen in 1 of 3 ways:
 * <ol>
 * <li>A system property named "log4j.configurationFactory" can be set with the
 * name of the ConfigurationFactory to be used.</li>
 * <li>
 * {@linkplain #setConfigurationFactory(ConfigurationFactory)} can be called
 * with the instance of the ConfigurationFactory to be used. This must be called
 * before any other calls to Log4j.</li>
 * <li>
 * A ConfigurationFactory implementation can be added to the classpath and
 * configured as a plugin. The Order annotation should be used to configure the
 * factory to be the first one inspected. See
 * {@linkplain XMLConfigurationFactory} for an example.</li>
 * </ol>
 * 
 * If the ConfigurationFactory that was added returns null on a call to
 * getConfiguration the any other ConfigurationFactories found as plugins will
 * be called in their respective order. DefaultConfiguration is always called
 * last if no configuration has been returned.
 */
public abstract class ConfigurationFactory {
    /**
     * Allow the ConfigurationFactory class to be specified as a system property.
     */
    public static final String CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";

    /**
     * Allow the location of the configuration file to be specified as a system property.
     */
    public static final String CONFIGURATION_FILE_PROPERTY = "log4j.configurationFile";

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * File name prefix for test configurations.
     */
    protected static final String TEST_PREFIX = "log4j2-test";

    /**
     * File name prefix for standard configurations.
     */
    protected static final String DEFAULT_PREFIX = "log4j2";

    private static List<ConfigurationFactory> factories = new ArrayList<ConfigurationFactory>();

    private static ConfigurationFactory configFactory = new Factory();

    /**
     * Returns the ConfigurationFactory.
     * @return the ConfigurationFactory.
     */
    public static ConfigurationFactory getInstance() {
        String factoryClass = System.getProperty(CONFIGURATION_FACTORY_PROPERTY);
        if (factoryClass != null) {
            addFactory(factoryClass);
        }
        PluginManager manager = new PluginManager("ConfigurationFactory");
        manager.collectPlugins();
        Map<String, PluginType> plugins = manager.getPlugins();
        Set<WeightedFactory> ordered = new TreeSet<WeightedFactory>();
        for (PluginType type : plugins.values()) {
            try {
                Class<ConfigurationFactory> clazz = type.getPluginClass();
                Order o = clazz.getAnnotation(Order.class);
                Integer weight = o.value();
                if (o != null) {
                    ordered.add(new WeightedFactory(weight, clazz));
                }
            } catch (Exception ex) {
              LOGGER.warn("Unable to add class " + type.getPluginClass());
            }
        }
        for (WeightedFactory wf : ordered) {
            addFactory(wf.factoryClass);
        }
        return configFactory;
    }

    private static void addFactory(String factoryClass) {
        try {
            Class clazz = Class.forName(factoryClass);
            addFactory(clazz);
        } catch (ClassNotFoundException ex) {
            LOGGER.error("Unable to load class " + factoryClass, ex);
        } catch (Exception ex) {
            LOGGER.error("Unable to load class " + factoryClass, ex);
        }
    }

    private static void addFactory(Class factoryClass) {
        try {
            factories.add((ConfigurationFactory) factoryClass.newInstance());
        } catch (Exception ex) {
            LOGGER.error("Unable to create instance of " + factoryClass.getName(), ex);
        }
    }

    /**
     * Set the configuration factory.
     * @param factory the ConfigurationFactory.
     */
    public static void setConfigurationFactory(ConfigurationFactory factory) {
        configFactory = factory;
    }

    /**
     * Reset the ConfigurationFactory to the default.
     */
    public static void resetConfigurationFactory() {
        configFactory = new Factory();
    }

    /**
     * Remove the ConfigurationFactory.
     * @param factory The factory to remove.
     */
    public static void removeConfigurationFactory(ConfigurationFactory factory) {
        factories.remove(factory);
    }

    protected abstract String[] getSupportedTypes();

    protected boolean isActive() {
        return true;
    }

    public abstract Configuration getConfiguration(ConfigurationSource source);

    /**
     * Returns the Configuration.
     * @param name The configuration name.
     * @param configLocation The configuration location.
     * @return The Configuration.
     */
    public Configuration getConfiguration(String name, URI configLocation) {
        if (!isActive()) {
            return null;
        }
        if (configLocation != null) {
            ConfigurationSource source = getInputFromURI(configLocation);
            if (source != null) {
                return getConfiguration(source);
            }
        }
        return null;
    }

    /**
     * Load the configuration from a URI.
     * @param configLocation A URI representing the location of the configuration.
     * @return The ConfigurationSource for the configuration.
     */
    protected ConfigurationSource getInputFromURI(URI configLocation) {
        File configFile = FileUtils.fileFromURI(configLocation);
        if (configFile != null && configFile.exists() && configFile.canRead()) {
            try {
                return new ConfigurationSource(new FileInputStream(configFile), configFile);
            } catch (FileNotFoundException ex) {
                LOGGER.error("Cannot locate file " + configLocation.getPath(), ex);
            }
        }
        String scheme = configLocation.getScheme();
        if (scheme == null || scheme.equals("classloader")) {
            ClassLoader loader = this.getClass().getClassLoader();
            ConfigurationSource source = getInputFromResource(configLocation.getPath(), loader);
            if (source != null) {
                return source;
            }
        }
        try {
            return new ConfigurationSource(configLocation.toURL().openStream(), configLocation.getPath());
        } catch (MalformedURLException ex) {
            LOGGER.error("Invalid URL " + configLocation.toString(), ex);
        } catch (IOException ex) {
            LOGGER.error("Unable to access " + configLocation.toString(), ex);
        } catch (Exception ex) {
            LOGGER.error("Unable to access " + configLocation.toString(), ex);
        }
        return null;
    }

    /**
     * Load the configuration from the location represented by the String.
     * @param config The configuration location.
     * @param loader The default ClassLoader to use.
     * @return The InputSource to use to read the configuration.
     */
    protected ConfigurationSource getInputFromString(String config, ClassLoader loader) {
        try {
            URL url = new URL(config);
            return new ConfigurationSource(url.openStream(), FileUtils.fileFromURI(url.toURI()));
        } catch (Exception ex) {
            ConfigurationSource source = getInputFromResource(config, loader);
            if (source == null) {
                try {
                    File file = new File(config);
                    return new ConfigurationSource(new FileInputStream(file), file);
                } catch (FileNotFoundException fnfe) {
                    // Ignore the exception
                }
            }
            return source;
        }
    }

    /**
     * Retrieve the configuration via the ClassLoader.
     * @param resource The resource to load.
     * @param loader The default ClassLoader to use.
     * @return The ConfigurationSource for the configuration.
     */
    protected ConfigurationSource getInputFromResource(String resource, ClassLoader loader) {
        URL url = Loader.getResource(resource, loader);
        if (url == null) {
            return null;
        }
        InputStream is = null;
        try {
            is = url.openStream();
        } catch (IOException ioe) {
            return null;
        }
        if (is == null) {
            return null;
        }

        if (FileUtils.isFile(url)) {
            try {
                return new ConfigurationSource(is, FileUtils.fileFromURI((url.toURI())));
            } catch (URISyntaxException ex) {
                // Just ignore the exception.
            }
        }
        return new ConfigurationSource(is, resource);
    }

    /**
     * Factory that chooses a ConfigurationFactory based on weighting.
     */
    private static class WeightedFactory implements Comparable<WeightedFactory> {
        private int weight;
        private Class<ConfigurationFactory> factoryClass;

        /**
         * Constructor.
         * @param weight The weight.
         * @param clazz The class.
         */
        public WeightedFactory(int weight, Class<ConfigurationFactory> clazz) {
            this.weight = weight;
            this.factoryClass = clazz;
        }

        public int compareTo(WeightedFactory wf) {
            int w = wf.weight;
            if (weight == w) {
                return 0;
            } else if (weight > w) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    /**
     * Default Factory.
     */
    private static class Factory extends ConfigurationFactory {

        /**
         * Default Factory Constructor.
         * @param name The configuration name.
         * @param configLocation The configuration location.
         * @return The Configuration.
         */
        @Override
        public Configuration getConfiguration(String name, URI configLocation) {

            if (configLocation == null) {
                String config = System.getProperty(CONFIGURATION_FILE_PROPERTY);
                if (config != null) {
                    ClassLoader loader = this.getClass().getClassLoader();
                    ConfigurationSource source = getInputFromString(config, loader);
                    if (source != null) {
                        for (ConfigurationFactory factory : factories) {
                            String[] types = factory.getSupportedTypes();
                            if (types != null) {
                                for (String type : types) {
                                    if (type.equals("*") || config.endsWith(type)) {
                                        Configuration c = factory.getConfiguration(source);
                                        if (c != null) {
                                            return c;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                for (ConfigurationFactory factory : factories) {
                    String[] types = factory.getSupportedTypes();
                    if (types != null) {
                        for (String type : types) {
                            if (type.equals("*") || configLocation.getPath().endsWith(type)) {
                                Configuration config = factory.getConfiguration(name, configLocation);
                                if (config != null) {
                                    return config;
                                }
                            }
                        }
                    }
                }
            }

            Configuration config = getConfiguration(true, name);
            if (config == null) {
                config = getConfiguration(true, null);
                if (config == null) {
                    config = getConfiguration(false, name);
                    if (config == null) {
                        config = getConfiguration(false, null);
                    }
                }
            }
            return config != null ? config : new DefaultConfiguration();
        }

        private Configuration getConfiguration(boolean isTest, String name) {
            boolean named = (name != null && name.length() > 0);
            ClassLoader loader = this.getClass().getClassLoader();
            for (ConfigurationFactory factory : factories) {
                String configName;
                String prefix = isTest ? TEST_PREFIX : DEFAULT_PREFIX;
                String [] types = factory.getSupportedTypes();
                if (types == null) {
                    continue;
                }

                for (String suffix : types) {
                    if (suffix.equals("*")) {
                        continue;
                    }
                    configName = named ? prefix + name + suffix : prefix + suffix;

                    ConfigurationSource source = getInputFromResource(configName, loader);
                    if (source != null) {
                        return factory.getConfiguration(source);
                    }
                }
            }
            return null;
        }

        @Override
        public String[] getSupportedTypes() {
            return null;
        }

        @Override
        public Configuration getConfiguration(ConfigurationSource source) {
            if (source != null) {
                String config = source.getLocation();
                for (ConfigurationFactory factory : factories) {
                    String[] types = factory.getSupportedTypes();
                    if (types != null) {
                        for (String type : types) {
                            if (type.equals("*") || (config != null && config.endsWith(type))) {
                                Configuration c = factory.getConfiguration(source);
                                if (c != null) {
                                    return c;
                                } else {
                                    LOGGER.error("Cannot determine the ConfigurationFactory to use for {}", config);
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
            LOGGER.error("Cannot process configuration, input source is null");
            return null;
        }
    }

    public static class ConfigurationSource {

        private File file;

        private String location;

        private InputStream stream;

        public ConfigurationSource() {
        }

        public ConfigurationSource(InputStream stream) {
            this.stream = stream;
            this .file = null;
            this.location = null;
        }

        public ConfigurationSource(InputStream stream, File file) {
            this.stream = stream;
            this.file = file;
            this.location = file.getAbsolutePath();
        }

        public ConfigurationSource(InputStream stream, String location) {
            this.stream = stream;
            this.location = location;
            this.file = null;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public InputStream getInputStream() {
            return stream;
        }

        public void setInputStream(InputStream stream) {
            this.stream = stream;
        }
    }
}
