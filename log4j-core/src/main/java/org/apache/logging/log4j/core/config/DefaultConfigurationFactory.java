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
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.spi.LoggingSystemProperties;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.Strings;

/**
 * Default factory for using a plugin selected based on the configuration source.
 */
public class DefaultConfigurationFactory extends ConfigurationFactory {

    private static final String ALL_TYPES = "*";
    private static final String OVERRIDE_PARAM = "override";

    private final Lazy<List<ConfigurationFactory>> configurationFactories;

    @Inject
    public DefaultConfigurationFactory(final Injector injector) {
        configurationFactories = Lazy.lazy(() -> loadConfigurationFactories(injector));
    }

    /**
     * Default Factory Constructor.
     *
     * @param name           The configuration name.
     * @param configLocation The configuration location.
     * @return The Configuration.
     */
    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {

        if (configLocation == null) {
            final PropertyEnvironment properties = PropertiesUtil.getProperties();
            final String configLocationStr = substitutor.replace(properties.getStringProperty(CONFIGURATION_FILE_PROPERTY));
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
            } else {
                final String log4j1ConfigStr =
                        substitutor.replace(properties.getStringProperty(LOG4J1_CONFIGURATION_FILE_PROPERTY));
                if (log4j1ConfigStr != null) {
                    System.setProperty(LOG4J1_EXPERIMENTAL, "true");
                    return getConfiguration(LOG4J1_VERSION, loggerContext, log4j1ConfigStr);
                }
            }
            for (final ConfigurationFactory factory : configurationFactories.value()) {
                final String[] types = factory.getSupportedTypes();
                if (types != null) {
                    for (final String type : types) {
                        if (type.equals(ALL_TYPES)) {
                            final Configuration config = factory.getConfiguration(loggerContext, name, null);
                            if (config != null) {
                                return config;
                            }
                        }
                    }
                }
            }
        } else {
            // configLocation != null
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
            final String configLocationStr = configLocation.toString();
            for (final ConfigurationFactory factory : configurationFactories.value()) {
                final String[] types = factory.getSupportedTypes();
                if (types != null) {
                    for (final String type : types) {
                        if (type.equals(ALL_TYPES) || configLocationStr.endsWith(type)) {
                            final Configuration config = factory.getConfiguration(loggerContext, name, configLocation);
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
        LOGGER.warn("No Log4j 2 configuration file found. " +
                "Using default configuration (logging only errors to the console), " +
                "or user programmatically provided configurations. " +
                "Set system property 'log4j2.*.{}' " +
                "to show Log4j 2 internal initialization logging. " +
                "See https://logging.apache.org/log4j/2.x/manual/configuration.html for instructions on how to configure Log4j 2",
                LoggingSystemProperties.SYSTEM_DEBUG);
        return new DefaultConfiguration();
    }

    private Configuration getConfiguration(final LoggerContext loggerContext, final String configLocationStr) {
        return getConfiguration(null, loggerContext, configLocationStr);
    }

    private Configuration getConfiguration(
            final String requiredVersion, final LoggerContext loggerContext,
            final String configLocationStr) {
        ConfigurationSource source = null;
        try {
            source = ConfigurationSource.fromUri(NetUtils.toURI(configLocationStr));
        } catch (final Exception ex) {
            // Ignore the error and try as a String.
            LOGGER.catching(Level.DEBUG, ex);
        }
        if (source != null) {
            for (final ConfigurationFactory factory : configurationFactories.value()) {
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

    private Configuration getConfiguration(final LoggerContext loggerContext, final boolean isTest, final String name) {
        final boolean named = Strings.isNotEmpty(name);
        final ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
        for (final ConfigurationFactory factory : configurationFactories.value()) {
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
                        LOGGER.warn("Found configuration file {} for inactive ConfigurationFactory {}", configName,
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
            for (final ConfigurationFactory factory : configurationFactories.value()) {
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
            for (final String pair : pairs) {
                final int idx = pair.indexOf("=");
                final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8) : pair;
                if (key.equalsIgnoreCase(OVERRIDE_PARAM)) {
                    locations.add(URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8));
                }
            }
            return locations.toArray(new String[0]);
        }
        return new String[] { uris[0] };
    }

    private String[] parseConfigLocations(final String configLocations) {
        final String[] uris = configLocations.split(",");
        if (uris.length > 1) {
            return uris;
        }
        try {
            return parseConfigLocations(new URI(configLocations));
        } catch (final URISyntaxException ex) {
            LOGGER.warn("Error parsing URI {}", configLocations);
        }
        return new String[] { configLocations };
    }

    private static List<ConfigurationFactory> loadConfigurationFactories(final Injector injector) {
        final List<ConfigurationFactory> factories = new ArrayList<>();

        Optional.ofNullable(PropertiesUtil.getProperties().getStringProperty(Log4jProperties.CONFIG_CONFIGURATION_FACTORY_CLASS_NAME))
                .flatMap(DefaultConfigurationFactory::tryLoadFactoryClass)
                .map(clazz -> {
                    try {
                        return injector.getInstance(clazz);
                    } catch (final Exception ex) {
                        LOGGER.error("Unable to create instance of {}", clazz, ex);
                        return null;
                    }
                })
                .ifPresent(factories::add);

        final List<Class<? extends ConfigurationFactory>> configurationFactoryPluginClasses = new ArrayList<>();
        injector.getInstance(PLUGIN_CATEGORY_KEY).forEach(type -> {
            try {
                configurationFactoryPluginClasses.add(type.getPluginClass().asSubclass(ConfigurationFactory.class));
            } catch (final Exception ex) {
                LOGGER.warn("Unable to add class {}", type.getPluginClass(), ex);
            }
        });
        configurationFactoryPluginClasses.sort(OrderComparator.getInstance());
        configurationFactoryPluginClasses.forEach(clazz -> {
            try {
                factories.add(injector.getInstance(clazz));
            } catch (final Exception ex) {
                LOGGER.error("Unable to create instance of {}", clazz, ex);
            }
        });

        return factories;
    }

    private static Optional<Class<? extends ConfigurationFactory>> tryLoadFactoryClass(final String factoryClass) {
        try {
            return Optional.of(Loader.loadClass(factoryClass).asSubclass(ConfigurationFactory.class));
        } catch (final Exception ex) {
            LOGGER.error("Unable to load ConfigurationFactory class {}", factoryClass, ex);
            return Optional.empty();
        }
    }
}
