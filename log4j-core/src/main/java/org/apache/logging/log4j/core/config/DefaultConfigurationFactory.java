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
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.lookup.ConfigurationStrSubstitutor;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.spi.LoggingSystemProperty;
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

    /**
     * Default Factory Constructor.
     *
     * @param name           The configuration name.
     * @param configLocation The configuration location.
     * @return The Configuration.
     */
    @Override
    public Configuration getConfiguration(
            final LoggerContext loggerContext, final String name, final URI configLocation) {
        final InstanceFactory instanceFactory = loggerContext.getInstanceFactory();
        final List<URIConfigurationFactory> configurationFactories = loadConfigurationFactories(instanceFactory);
        final StrSubstitutor substitutor = instanceFactory.getInstance(ConfigurationStrSubstitutor.class);
        if (configLocation == null) {
            PropertyEnvironment properties = loggerContext.getEnvironment();
            if (properties == null) {
                properties = PropertiesUtil.getProperties();
            }
            final String configLocationStr =
                    substitutor.replace(properties.getStringProperty(Log4jPropertyKey.CONFIG_LOCATION));
            if (configLocationStr != null) {
                final String[] sources = parseConfigLocations(configLocationStr);
                if (sources.length > 1) {
                    final List<AbstractConfiguration> configs = new ArrayList<>();
                    for (final String sourceLocation : sources) {
                        final Configuration config =
                                getConfiguration(null, loggerContext, sourceLocation.trim(), configurationFactories);
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
                        return new CompositeConfiguration(loggerContext, configs);
                    } else if (configs.size() == 1) {
                        return configs.get(0);
                    }
                }
                return getConfiguration(null, loggerContext, configLocationStr, configurationFactories);
            } else {
                final String log4j1ConfigStr =
                        substitutor.replace(properties.getStringProperty(LOG4J1_CONFIGURATION_FILE_PROPERTY));
                if (log4j1ConfigStr != null) {
                    System.setProperty(LOG4J1_EXPERIMENTAL.getSystemKey(), "true");
                    return getConfiguration(LOG4J1_VERSION, loggerContext, log4j1ConfigStr, configurationFactories);
                }
            }
            for (final URIConfigurationFactory factory : configurationFactories) {
                final String[] types = factory.getSupportedExtensions();
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
                    final Configuration config =
                            getConfiguration(null, loggerContext, sourceLocation.trim(), configurationFactories);
                    if (config instanceof AbstractConfiguration) {
                        configs.add((AbstractConfiguration) config);
                    } else {
                        LOGGER.error("Failed to created configuration at {}", sourceLocation);
                        return null;
                    }
                }
                return new CompositeConfiguration(loggerContext, configs);
            }
            final String configLocationStr = configLocation.toString();
            for (final URIConfigurationFactory factory : configurationFactories) {
                final String[] types = factory.getSupportedExtensions();
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

        Configuration config = getConfiguration(loggerContext, true, name, configurationFactories);
        if (config == null) {
            config = getConfiguration(loggerContext, true, null, configurationFactories);
            if (config == null) {
                config = getConfiguration(loggerContext, false, name, configurationFactories);
                if (config == null) {
                    config = getConfiguration(loggerContext, false, null, configurationFactories);
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
                        + "Set system property 'log4j2.*.{}' "
                        + "to show Log4j 2 internal initialization logging. "
                        + "See https://logging.apache.org/log4j/2.x/manual/configuration.html for instructions on how to configure Log4j 2",
                LoggingSystemProperty.STATUS_LOGGER_DEBUG);
        return new DefaultConfiguration(loggerContext);
    }

    private Configuration getConfiguration(
            final String requiredVersion,
            final LoggerContext loggerContext,
            final String configLocation,
            final Iterable<? extends URIConfigurationFactory> configurationFactories) {
        ConfigurationSource source = null;
        try {
            source = ConfigurationSource.fromUri(NetUtils.toURI(configLocation));
        } catch (final Exception ex) {
            // Ignore the error and try as a String.
            LOGGER.catching(Level.DEBUG, ex);
        }
        if (source != null) {
            for (final URIConfigurationFactory factory : configurationFactories) {
                if (requiredVersion != null && !factory.getVersion().equals(requiredVersion)) {
                    continue;
                }
                final String[] types = factory.getSupportedExtensions();
                if (types != null) {
                    for (final String type : types) {
                        if (type.equals(ALL_TYPES) || configLocation.endsWith(type)) {
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
            final LoggerContext loggerContext,
            final boolean isTest,
            final CharSequence name,
            final Iterable<? extends URIConfigurationFactory> configurationFactories) {
        final boolean named = Strings.isNotEmpty(name);
        final ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
        for (final URIConfigurationFactory factory : configurationFactories) {
            String configName;
            final String prefix = isTest ? factory.getTestPrefix() : factory.getDefaultPrefix();
            final String[] types = factory.getSupportedExtensions();
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
                    try {
                        return factory.getConfiguration(loggerContext, source);
                    } catch (final LinkageError e) {
                        LOGGER.warn(
                                "Failed to create configuration from resource {} using {}.",
                                source,
                                factory.getClass().getName(),
                                e);
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected String[] getSupportedTypes() {
        return null;
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        if (source != null) {
            final List<URIConfigurationFactory> configurationFactories =
                    loadConfigurationFactories(loggerContext.getInstanceFactory());
            final String config = source.getLocation();
            for (final URIConfigurationFactory factory : configurationFactories) {
                final String[] types = factory.getSupportedExtensions();
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
        return new String[] {uris[0]};
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
        return new String[] {configLocations};
    }

    private static List<URIConfigurationFactory> loadConfigurationFactories(final InstanceFactory instanceFactory) {
        final List<URIConfigurationFactory> factories = new ArrayList<>();

        Optional.ofNullable(instanceFactory
                        .getInstance(PropertyEnvironment.class)
                        .getStringProperty(Log4jPropertyKey.CONFIG_CONFIGURATION_FACTORY_CLASS_NAME))
                .flatMap(DefaultConfigurationFactory::tryLoadFactoryClass)
                .map(clazz -> {
                    try {
                        return instanceFactory.getInstance(clazz);
                    } catch (final Exception ex) {
                        LOGGER.error("Unable to create instance of {}", clazz, ex);
                        return null;
                    }
                })
                .ifPresent(factories::add);

        final List<Class<? extends URIConfigurationFactory>> configurationFactoryPluginClasses = new ArrayList<>();
        instanceFactory.getInstance(PLUGIN_NAMESPACE_KEY).forEach(type -> {
            try {
                configurationFactoryPluginClasses.add(type.getPluginClass().asSubclass(URIConfigurationFactory.class));
            } catch (final Exception ex) {
                LOGGER.warn("Unable to add class {}", type.getPluginClass(), ex);
            }
        });
        configurationFactoryPluginClasses.sort(OrderComparator.getInstance());
        configurationFactoryPluginClasses.forEach(clazz -> {
            try {
                factories.add(instanceFactory.getInstance(clazz));
            } catch (final Exception ex) {
                LOGGER.error("Unable to create instance of {}", clazz, ex);
            }
        });

        return factories;
    }

    private static Optional<Class<? extends URIConfigurationFactory>> tryLoadFactoryClass(final String factoryClass) {
        try {
            return Optional.of(Loader.loadClass(factoryClass).asSubclass(ConfigurationFactory.class));
        } catch (final Exception ex) {
            LOGGER.error("Unable to load ConfigurationFactory class {}", factoryClass, ex);
            return Optional.empty();
        }
    }
}
