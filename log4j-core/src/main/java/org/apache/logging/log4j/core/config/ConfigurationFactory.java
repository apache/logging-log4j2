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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.PluginManager;
import org.apache.logging.log4j.core.config.plugins.PluginType;
import org.apache.logging.log4j.core.helpers.FileUtils;
import org.apache.logging.log4j.core.helpers.Loader;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

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

    /**
     * The name of the classloader URI scheme.
     */
    private static final String CLASS_LOADER_SCHEME = "classloader";
    private static final int CLASS_LOADER_SCHEME_LENGTH = CLASS_LOADER_SCHEME.length() + 1;

    /**
     * The name of the classpath URI scheme, synonymous with the classloader URI scheme.
     */
    private static final String CLASS_PATH_SCHEME = "classpath";
    private static final int CLASS_PATH_SCHEME_LENGTH = CLASS_PATH_SCHEME.length() + 1;

    private static volatile List<ConfigurationFactory> factories = null;

    private static ConfigurationFactory configFactory = new Factory();

    /**
     * Returns the ConfigurationFactory.
     * @return the ConfigurationFactory.
     */
    public static ConfigurationFactory getInstance() {
        if (factories == null) {
            synchronized(TEST_PREFIX) {
                if (factories == null) {
                    final List<ConfigurationFactory> list = new ArrayList<ConfigurationFactory>();
                    final String factoryClass = PropertiesUtil.getProperties().getStringProperty(CONFIGURATION_FACTORY_PROPERTY);
                    if (factoryClass != null) {
                        addFactory(list, factoryClass);
                    }
                    final PluginManager manager = new PluginManager("ConfigurationFactory");
                    manager.collectPlugins();
                    final Map<String, PluginType<?>> plugins = manager.getPlugins();
                    final Set<WeightedFactory> ordered = new TreeSet<WeightedFactory>();
                    for (final PluginType<?> type : plugins.values()) {
                        try {
                            @SuppressWarnings("unchecked")
                            final Class<ConfigurationFactory> clazz = (Class<ConfigurationFactory>)type.getPluginClass();
                            final Order order = clazz.getAnnotation(Order.class);
                            if (order != null) {
                                final int weight = order.value();
                                ordered.add(new WeightedFactory(weight, clazz));
                            }
                        } catch (final Exception ex) {
                            LOGGER.warn("Unable to add class " + type.getPluginClass());
                        }
                    }
                    for (final WeightedFactory wf : ordered) {
                        addFactory(list, wf.factoryClass);
                    }
                    factories = Collections.unmodifiableList(list);
                }
            }
        }

        return configFactory;
    }

    @SuppressWarnings("unchecked")
    private static void addFactory(final List<ConfigurationFactory> list, final String factoryClass) {
        try {
            addFactory(list, (Class<ConfigurationFactory>) Class.forName(factoryClass));
        } catch (final ClassNotFoundException ex) {
            LOGGER.error("Unable to load class " + factoryClass, ex);
        } catch (final Exception ex) {
            LOGGER.error("Unable to load class " + factoryClass, ex);
        }
    }

    private static void addFactory(final List<ConfigurationFactory> list,
                                   final Class<ConfigurationFactory> factoryClass) {
        try {
            list.add(factoryClass.newInstance());
        } catch (final Exception ex) {
            LOGGER.error("Unable to create instance of " + factoryClass.getName(), ex);
        }
    }

    /**
     * Set the configuration factory. This method is not intended for general use and may not be thread safe.
     * @param factory the ConfigurationFactory.
     */
    public static void setConfigurationFactory(final ConfigurationFactory factory) {
        configFactory = factory;
    }

    /**
     * Reset the ConfigurationFactory to the default. This method is not intended for general use and may
     * not be thread safe.
     */
    public static void resetConfigurationFactory() {
        configFactory = new Factory();
    }

    /**
     * Remove the ConfigurationFactory. This method is not intended for general use and may not be thread safe.
     * @param factory The factory to remove.
     */
    public static void removeConfigurationFactory(final ConfigurationFactory factory) {
        if (configFactory == factory) {
            configFactory = new Factory();
        }
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
    public Configuration getConfiguration(final String name, final URI configLocation) {
        if (!isActive()) {
            return null;
        }
        if (configLocation != null) {
            final ConfigurationSource source = getInputFromURI(configLocation);
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
    protected ConfigurationSource getInputFromURI(final URI configLocation) {
        final File configFile = FileUtils.fileFromURI(configLocation);
        if (configFile != null && configFile.exists() && configFile.canRead()) {
            try {
                return new ConfigurationSource(new FileInputStream(configFile), configFile);
            } catch (final FileNotFoundException ex) {
                LOGGER.error("Cannot locate file " + configLocation.getPath(), ex);
            }
        }
        final String scheme = configLocation.getScheme();
        final boolean isClassLoaderScheme = scheme != null && scheme.equals(CLASS_LOADER_SCHEME);
        final boolean isClassPathScheme = scheme != null && !isClassLoaderScheme && scheme.equals(CLASS_PATH_SCHEME);
        if (scheme == null || isClassLoaderScheme || isClassPathScheme) {
            final ClassLoader loader = this.getClass().getClassLoader();
            String path;
            if (isClassLoaderScheme) {
                path = configLocation.toString().substring(CLASS_LOADER_SCHEME_LENGTH);
            } else if (isClassPathScheme) {
                path = configLocation.toString().substring(CLASS_PATH_SCHEME_LENGTH);
            } else {
                path = configLocation.getPath();
            }
            final ConfigurationSource source = getInputFromResource(path, loader);
            if (source != null) {
                return source;
            }
        }
        try {
            return new ConfigurationSource(configLocation.toURL().openStream(), configLocation.getPath());
        } catch (final MalformedURLException ex) {
            LOGGER.error("Invalid URL " + configLocation.toString(), ex);
        } catch (final IOException ex) {
            LOGGER.error("Unable to access " + configLocation.toString(), ex);
        } catch (final Exception ex) {
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
    protected ConfigurationSource getInputFromString(final String config, final ClassLoader loader) {
        try {
            final URL url = new URL(config);
            return new ConfigurationSource(url.openStream(), FileUtils.fileFromURI(url.toURI()));
        } catch (final Exception ex) {
            final ConfigurationSource source = getInputFromResource(config, loader);
            if (source == null) {
                try {
                    final File file = new File(config);
                    return new ConfigurationSource(new FileInputStream(file), file);
                } catch (final FileNotFoundException fnfe) {
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
    protected ConfigurationSource getInputFromResource(final String resource, final ClassLoader loader) {
        final URL url = Loader.getResource(resource, loader);
        if (url == null) {
            return null;
        }
        InputStream is = null;
        try {
            is = url.openStream();
        } catch (final IOException ioe) {
            return null;
        }
        if (is == null) {
            return null;
        }

        if (FileUtils.isFile(url)) {
            try {
                return new ConfigurationSource(is, FileUtils.fileFromURI((url.toURI())));
            } catch (final URISyntaxException ex) {
                // Just ignore the exception.
            }
        }
        return new ConfigurationSource(is, resource);
    }

    /**
     * Factory that chooses a ConfigurationFactory based on weighting.
     */
    private static class WeightedFactory implements Comparable<WeightedFactory> {
        private final int weight;
        private final Class<ConfigurationFactory> factoryClass;

        /**
         * Constructor.
         * @param weight The weight.
         * @param clazz The class.
         */
        public WeightedFactory(final int weight, final Class<ConfigurationFactory> clazz) {
            this.weight = weight;
            this.factoryClass = clazz;
        }

        @Override
        public int compareTo(final WeightedFactory wf) {
            final int w = wf.weight;
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
        public Configuration getConfiguration(final String name, final URI configLocation) {

            if (configLocation == null) {
                final String config = PropertiesUtil.getProperties().getStringProperty(CONFIGURATION_FILE_PROPERTY);
                if (config != null) {
                    final ClassLoader loader = this.getClass().getClassLoader();
                    final ConfigurationSource source = getInputFromString(config, loader);
                    if (source != null) {
                        for (final ConfigurationFactory factory : factories) {
                            final String[] types = factory.getSupportedTypes();
                            if (types != null) {
                                for (final String type : types) {
                                    if (type.equals("*") || config.endsWith(type)) {
                                        final Configuration c = factory.getConfiguration(source);
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
                for (final ConfigurationFactory factory : factories) {
                    final String[] types = factory.getSupportedTypes();
                    if (types != null) {
                        for (final String type : types) {
                            if (type.equals("*") || configLocation.toString().endsWith(type)) {
                                final Configuration config = factory.getConfiguration(name, configLocation);
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

        private Configuration getConfiguration(final boolean isTest, final String name) {
            final boolean named = name != null && name.length() > 0;
            final ClassLoader loader = this.getClass().getClassLoader();
            for (final ConfigurationFactory factory : factories) {
                String configName;
                final String prefix = isTest ? TEST_PREFIX : DEFAULT_PREFIX;
                final String [] types = factory.getSupportedTypes();
                if (types == null) {
                    continue;
                }

                for (final String suffix : types) {
                    if (suffix.equals("*")) {
                        continue;
                    }
                    configName = named ? prefix + name + suffix : prefix + suffix;

                    final ConfigurationSource source = getInputFromResource(configName, loader);
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
        public Configuration getConfiguration(final ConfigurationSource source) {
            if (source != null) {
                final String config = source.getLocation();
                for (final ConfigurationFactory factory : factories) {
                    final String[] types = factory.getSupportedTypes();
                    if (types != null) {
                        for (final String type : types) {
                            if (type.equals("*") || (config != null && config.endsWith(type))) {
                                final Configuration c = factory.getConfiguration(source);
                                if (c != null) {
                                    return c;
                                }
                                LOGGER.error("Cannot determine the ConfigurationFactory to use for {}", config);
                                return null;
                            }
                        }
                    }
                }
            }
            LOGGER.error("Cannot process configuration, input source is null");
            return null;
        }
    }

    /**
     * Represents the source for the logging configuration.
     */
    public static class ConfigurationSource {

        private File file;

        private String location;

        private InputStream stream;

        public ConfigurationSource() {
        }

        public ConfigurationSource(final InputStream stream) {
            this.stream = stream;
            this.file = null;
            this.location = null;
        }

        public ConfigurationSource(final InputStream stream, final File file) {
            this.stream = stream;
            this.file = file;
            this.location = file.getAbsolutePath();
        }

        public ConfigurationSource(final InputStream stream, final String location) {
            this.stream = stream;
            this.location = location;
            this.file = null;
        }

        public File getFile() {
            return file;
        }

        public void setFile(final File file) {
            this.file = file;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(final String location) {
            this.location = location;
        }

        public InputStream getInputStream() {
            return stream;
        }

        public void setInputStream(final InputStream stream) {
            this.stream = stream;
        }
    }
}
