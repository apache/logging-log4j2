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
package org.apache.logging.log4j.core.config;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.lookup.ConfigurationStrSubstitutor;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.BasicAuthorizationProvider;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Factory class for parsed {@link Configuration} objects from a configuration file.
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
 * A ConfigurationFactory implementation can be added to the classpath and configured as a plugin in the
 * {@link #CATEGORY ConfigurationFactory} category. The {@link Order} annotation should be used to configure the
 * factory to be the first one inspected. See
 * {@linkplain org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory} for an example.</li>
 * </ol>
 *
 * If the ConfigurationFactory that was added returns null on a call to
 * getConfiguration then any other ConfigurationFactories found as plugins will
 * be called in their respective order. DefaultConfiguration is always called
 * last if no configuration has been returned.
 */
public abstract class ConfigurationFactory extends ConfigurationBuilderFactory {

    public ConfigurationFactory() {
        // TEMP For breakpoints
    }

    /**
     * Allows the ConfigurationFactory class to be specified as a system property.
     */
    public static final String CONFIGURATION_FACTORY_PROPERTY = "log4j.configurationFactory";

    /**
     * Allows the location of the configuration file to be specified as a system property.
     */
    public static final String CONFIGURATION_FILE_PROPERTY = "log4j.configurationFile";

    public static final String LOG4J1_CONFIGURATION_FILE_PROPERTY = "log4j.configuration";

    public static final String LOG4J1_EXPERIMENTAL = "log4j1.compatibility";

    public static final String AUTHORIZATION_PROVIDER = "authorizationProvider";

    /**
     * Plugin category used to inject a ConfigurationFactory {@link org.apache.logging.log4j.core.config.plugins.Plugin}
     * class.
     *
     * @since 2.1
     */
    public static final String CATEGORY = "ConfigurationFactory";

    /**
     * Allows subclasses access to the status logger without creating another instance.
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

    protected static final String LOG4J1_VERSION = "1";
    protected static final String LOG4J2_VERSION = "2";

    /**
     * The name of the classloader URI scheme.
     */
    private static final String CLASS_LOADER_SCHEME = "classloader";

    /**
     * The name of the classpath URI scheme, synonymous with the classloader URI scheme.
     */
    private static final String CLASS_PATH_SCHEME = "classpath";

    private static final String OVERRIDE_PARAM = "override";

    private static volatile List<ConfigurationFactory> factories;

    private static ConfigurationFactory configFactory = new Factory();

    protected final StrSubstitutor substitutor = new ConfigurationStrSubstitutor(new Interpolator());

    private static final Lock LOCK = new ReentrantLock();

    private static final String HTTPS = "https";
    private static final String HTTP = "http";

    private static final String[] PREFIXES = {"log4j2.", "log4j2.Configuration."};

    private static volatile AuthorizationProvider authorizationProvider;

    /**
     * Returns the ConfigurationFactory.
     * @return the ConfigurationFactory.
     */
    public static ConfigurationFactory getInstance() {
        // volatile works in Java 1.6+, so double-checked locking also works properly
        //noinspection DoubleCheckedLocking
        if (factories == null) {
            LOCK.lock();
            try {
                if (factories == null) {
                    final List<ConfigurationFactory> list = new ArrayList<>();
                    final PropertiesUtil props = PropertiesUtil.getProperties();
                    final String factoryClass = props.getStringProperty(CONFIGURATION_FACTORY_PROPERTY);
                    if (factoryClass != null) {
                        addFactory(list, factoryClass);
                    }
                    final PluginManager manager = new PluginManager(CATEGORY);
                    manager.collectPlugins();
                    final Map<String, PluginType<?>> plugins = manager.getPlugins();
                    final List<Class<? extends ConfigurationFactory>> ordered = new ArrayList<>(plugins.size());
                    for (final PluginType<?> type : plugins.values()) {
                        try {
                            ordered.add(type.getPluginClass().asSubclass(ConfigurationFactory.class));
                        } catch (final Exception ex) {
                            LOGGER.warn("Unable to add class {}", type.getPluginClass(), ex);
                        }
                    }
                    Collections.sort(ordered, OrderComparator.getInstance());
                    for (final Class<? extends ConfigurationFactory> clazz : ordered) {
                        addFactory(list, clazz);
                    }
                    // see above comments about double-checked locking
                    //noinspection NonThreadSafeLazyInitialization
                    factories = Collections.unmodifiableList(list);
                    authorizationProvider = authorizationProvider(props);
                }
            } finally {
                LOCK.unlock();
            }
        }

        LOGGER.debug("Using configurationFactory {}", configFactory);
        return configFactory;
    }

    public static AuthorizationProvider authorizationProvider(final PropertiesUtil props) {
        final String authClass = props.getStringProperty(PREFIXES, AUTHORIZATION_PROVIDER, null);
        AuthorizationProvider provider = null;
        if (authClass != null) {
            try {
                final Object obj = LoaderUtil.newInstanceOf(authClass);
                if (obj instanceof AuthorizationProvider) {
                    provider = (AuthorizationProvider) obj;
                } else {
                    LOGGER.warn(
                            "{} is not an AuthorizationProvider, using default",
                            obj.getClass().getName());
                }
            } catch (Exception ex) {
                LOGGER.warn("Unable to create {}, using default: {}", authClass, ex.getMessage());
            }
        }
        if (provider == null) {
            provider = new BasicAuthorizationProvider(props);
        }
        return provider;
    }

    public static AuthorizationProvider getAuthorizationProvider() {
        return authorizationProvider;
    }

    private static void addFactory(final Collection<ConfigurationFactory> list, final String factoryClass) {
        try {
            addFactory(list, Loader.loadClass(factoryClass).asSubclass(ConfigurationFactory.class));
        } catch (final Exception ex) {
            LOGGER.error("Unable to load class {}", factoryClass, ex);
        }
    }

    private static void addFactory(
            final Collection<ConfigurationFactory> list, final Class<? extends ConfigurationFactory> factoryClass) {
        try {
            list.add(ReflectionUtil.instantiate(factoryClass));
        } catch (final Exception ex) {
            LOGGER.error("Unable to create instance of {}", factoryClass.getName(), ex);
        }
    }

    /**
     * Sets the configuration factory. This method is not intended for general use and may not be thread safe.
     * @param factory the ConfigurationFactory.
     */
    public static void setConfigurationFactory(final ConfigurationFactory factory) {
        configFactory = factory;
    }

    /**
     * Resets the ConfigurationFactory to the default. This method is not intended for general use and may
     * not be thread safe.
     */
    public static void resetConfigurationFactory() {
        configFactory = new Factory();
    }

    /**
     * Removes the ConfigurationFactory. This method is not intended for general use and may not be thread safe.
     * @param factory The factory to remove.
     */
    public static void removeConfigurationFactory(final ConfigurationFactory factory) {
        if (configFactory == factory) {
            configFactory = new Factory();
        }
    }

    protected abstract String[] getSupportedTypes();

    protected String getTestPrefix() {
        return TEST_PREFIX;
    }

    protected String getDefaultPrefix() {
        return DEFAULT_PREFIX;
    }

    protected String getVersion() {
        return LOG4J2_VERSION;
    }

    protected boolean isActive() {
        return true;
    }

    public abstract Configuration getConfiguration(final LoggerContext loggerContext, ConfigurationSource source);

    /**
     * Returns the Configuration.
     * @param loggerContext The logger context
     * @param name The configuration name.
     * @param configLocation The configuration location.
     * @return The Configuration.
     */
    public Configuration getConfiguration(
            final LoggerContext loggerContext, final String name, final URI configLocation) {
        if (!isActive()) {
            return null;
        }
        if (configLocation != null) {
            final ConfigurationSource source = ConfigurationSource.fromUri(configLocation);
            if (source != null) {
                return getConfiguration(loggerContext, source);
            }
        }
        return null;
    }

    /**
     * Returns the Configuration obtained using a given ClassLoader.
     * @param loggerContext The logger context
     * @param name The configuration name.
     * @param configLocation A URI representing the location of the configuration.
     * @param loader The default ClassLoader to use. If this is {@code null}, then the
     *               {@linkplain LoaderUtil#getThreadContextClassLoader() default ClassLoader} will be used.
     *
     * @return The Configuration.
     */
    public Configuration getConfiguration(
            final LoggerContext loggerContext, final String name, final URI configLocation, final ClassLoader loader) {
        if (!isActive()) {
            return null;
        }
        if (loader == null) {
            return getConfiguration(loggerContext, name, configLocation);
        }
        if (isClassLoaderUri(configLocation)) {
            final String path = extractClassLoaderUriPath(configLocation);
            final ConfigurationSource source = ConfigurationSource.fromResource(path, loader);
            if (source != null) {
                final Configuration configuration = getConfiguration(loggerContext, source);
                if (configuration != null) {
                    return configuration;
                }
            }
        }
        return getConfiguration(loggerContext, name, configLocation);
    }

    static boolean isClassLoaderUri(final URI uri) {
        if (uri == null) {
            return false;
        }
        final String scheme = uri.getScheme();
        return scheme == null || scheme.equals(CLASS_LOADER_SCHEME) || scheme.equals(CLASS_PATH_SCHEME);
    }

    static String extractClassLoaderUriPath(final URI uri) {
        return uri.getScheme() == null ? uri.getPath() : uri.getSchemeSpecificPart();
    }

    /**
     * Loads the configuration from the location represented by the String.
     * @param config The configuration location.
     * @param loader The default ClassLoader to use.
     * @return The InputSource to use to read the configuration.
     */
    @Deprecated
    protected ConfigurationSource getInputFromString(final String config, final ClassLoader loader) {
        return ConfigurationSource.fromUri(NetUtils.toURI(config));
    }

    /**
     * Default Factory.
     */
    private static class Factory extends ConfigurationFactory {

        private static final String ALL_TYPES = "*";

        /**
         * Default Factory Constructor.
         * @param name The configuration name.
         * @param configLocation The configuration location.
         * @return The Configuration.
         */
        @Override
        public Configuration getConfiguration(
                final LoggerContext loggerContext, final String name, final URI configLocation) {

            if (configLocation == null) {
                final String configLocationStr = this.substitutor.replace(
                        PropertiesUtil.getProperties().getStringProperty(CONFIGURATION_FILE_PROPERTY));
                if (configLocationStr != null) {
                    final String[] sources = parseConfigLocations(configLocationStr);
                    if (sources.length > 1) {
                        final List<AbstractConfiguration> configs = new ArrayList<>();
                        for (final String sourceLocation : sources) {
                            final Configuration config = getConfiguration(loggerContext, sourceLocation.trim());
                            if (config != null) {
                                if (config instanceof AbstractConfiguration) {
                                    configs.add((AbstractConfiguration) config);
                                } else {
                                    LOGGER.error("Failed to created configuration at {}", sourceLocation);
                                    return null;
                                }
                            } else {
                                LOGGER.warn("Unable to create configuration for {}, ignoring", sourceLocation);
                            }
                        }
                        if (configs.size() > 1) {
                            return new CompositeConfiguration(configs);
                        } else if (configs.size() == 1) {
                            return configs.get(0);
                        }
                    }
                    return getConfiguration(loggerContext, configLocationStr);
                }
                final String log4j1ConfigStr = this.substitutor.replace(
                        PropertiesUtil.getProperties().getStringProperty(LOG4J1_CONFIGURATION_FILE_PROPERTY));
                if (log4j1ConfigStr != null) {
                    System.setProperty(LOG4J1_EXPERIMENTAL, "true");
                    return getConfiguration(LOG4J1_VERSION, loggerContext, log4j1ConfigStr);
                }
                for (final ConfigurationFactory factory : getFactories()) {
                    final String[] types = factory.getSupportedTypes();
                    if (types != null) {
                        for (final String type : types) {
                            if (type.equals(ALL_TYPES)) {
                                final Configuration config =
                                        factory.getConfiguration(loggerContext, name, configLocation);
                                if (config != null) {
                                    return config;
                                }
                            }
                        }
                    }
                }
            } else {
                final String[] sources = parseConfigLocations(configLocation);
                if (sources.length > 1) {
                    final List<AbstractConfiguration> configs = new ArrayList<>();
                    for (final String sourceLocation : sources) {
                        final Configuration config = getConfiguration(loggerContext, sourceLocation.trim());
                        if (config instanceof AbstractConfiguration) {
                            configs.add((AbstractConfiguration) config);
                        } else {
                            LOGGER.error("Failed to created configuration at {}", sourceLocation);
                            return null;
                        }
                    }
                    return new CompositeConfiguration(configs);
                }
                // configLocation != null
                final String configLocationStr = configLocation.toString();
                for (final ConfigurationFactory factory : getFactories()) {
                    final String[] types = factory.getSupportedTypes();
                    if (types != null) {
                        for (final String type : types) {
                            if (type.equals(ALL_TYPES) || configLocationStr.endsWith(type)) {
                                final Configuration config =
                                        factory.getConfiguration(loggerContext, name, configLocation);
                                if (config != null) {
                                    return config;
                                }
                            }
                        }
                    }
                }
            }

            Configuration config = getConfiguration(loggerContext, true, name);
            if (config == null) {
                config = getConfiguration(loggerContext, true, null);
                if (config == null) {
                    config = getConfiguration(loggerContext, false, name);
                    if (config == null) {
                        config = getConfiguration(loggerContext, false, null);
                    }
                }
            }
            if (config != null) {
                return config;
            }
            LOGGER.warn(
                    "No Log4j 2 configuration file found. "
                            + "Using default configuration (logging only errors to the console), "
                            + "or user programmatically provided configurations. "
                            + "Set system property 'log4j2.debug' "
                            + "to show Log4j 2 internal initialization logging. "
                            + "See https://logging.apache.org/log4j/2.x/manual/configuration.html for instructions on how to configure Log4j 2");
            return new DefaultConfiguration();
        }

        private Configuration getConfiguration(final LoggerContext loggerContext, final String configLocationStr) {
            return getConfiguration(null, loggerContext, configLocationStr);
        }

        private Configuration getConfiguration(
                final String requiredVersion, final LoggerContext loggerContext, final String configLocationStr) {
            ConfigurationSource source = null;
            try {
                source = ConfigurationSource.fromUri(NetUtils.toURI(configLocationStr));
            } catch (final Exception ex) {
                // Ignore the error and try as a String.
                LOGGER.catching(Level.DEBUG, ex);
            }
            if (source == null) {
                final ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
                source = getInputFromString(configLocationStr, loader);
            }
            if (source != null) {
                for (final ConfigurationFactory factory : getFactories()) {
                    if (requiredVersion != null && !factory.getVersion().equals(requiredVersion)) {
                        continue;
                    }
                    final String[] types = factory.getSupportedTypes();
                    if (types != null) {
                        for (final String type : types) {
                            if (type.equals(ALL_TYPES) || configLocationStr.endsWith(type)) {
                                final Configuration config = factory.getConfiguration(loggerContext, source);
                                if (config != null) {
                                    return config;
                                }
                            }
                        }
                    }
                }
            }
            return null;
        }

        private Configuration getConfiguration(
                final LoggerContext loggerContext, final boolean isTest, final String name) {
            final boolean named = Strings.isNotEmpty(name);
            final ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
            for (final ConfigurationFactory factory : getFactories()) {
                String configName;
                final String prefix = isTest ? factory.getTestPrefix() : factory.getDefaultPrefix();
                final String[] types = factory.getSupportedTypes();
                if (types == null) {
                    continue;
                }

                for (final String suffix : types) {
                    if (suffix.equals(ALL_TYPES)) {
                        continue;
                    }
                    configName = named ? prefix + name + suffix : prefix + suffix;

                    final ConfigurationSource source = ConfigurationSource.fromResource(configName, loader);
                    if (source != null) {
                        if (!factory.isActive()) {
                            LOGGER.error(
                                    "Found configuration file `{}` for the inactive `{}`. This `ConfigurationFactory` implementation might be inactive due to a missing dependency.",
                                    configName,
                                    factory.getClass().getName());
                        }
                        return factory.getConfiguration(loggerContext, source);
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
        public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
            if (source != null) {
                final String config = source.getLocation();
                for (final ConfigurationFactory factory : getFactories()) {
                    final String[] types = factory.getSupportedTypes();
                    if (types != null) {
                        for (final String type : types) {
                            if (type.equals(ALL_TYPES) || config != null && config.endsWith(type)) {
                                final Configuration c = factory.getConfiguration(loggerContext, source);
                                if (c != null) {
                                    LOGGER.debug("Loaded configuration from {}", source);
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

        private String[] parseConfigLocations(final URI configLocations) {
            final String[] uris = configLocations.toString().split("\\?");
            final List<String> locations = new ArrayList<>();
            if (uris.length > 1) {
                locations.add(uris[0]);
                final String[] pairs = configLocations.getQuery().split("&");
                for (String pair : pairs) {
                    final int idx = pair.indexOf("=");
                    try {
                        final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                        if (key.equalsIgnoreCase(OVERRIDE_PARAM)) {
                            locations.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                        }
                    } catch (UnsupportedEncodingException ex) {
                        LOGGER.warn("Invalid query parameter in {}", configLocations);
                    }
                }
                return locations.toArray(Strings.EMPTY_ARRAY);
            }
            return new String[] {uris[0]};
        }

        private String[] parseConfigLocations(final String configLocations) {
            final String[] uris = configLocations.split(",");
            if (uris.length > 1) {
                return uris;
            }
            try {
                return parseConfigLocations(new URI(configLocations));
            } catch (URISyntaxException ex) {
                LOGGER.warn("Error parsing URI {}", configLocations);
            }
            return new String[] {configLocations};
        }
    }

    static List<ConfigurationFactory> getFactories() {
        return factories;
    }
}
