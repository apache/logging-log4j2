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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.ContextScoped;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertyResolver;
import org.apache.logging.log4j.util.Strings;

/**
 * Provides configuration instances for configuration sources using an appropriate {@link ConfigurationFactory}.
 *
 * @since 3.0.0
 */
@ContextScoped
public class ConfigurationProvider {
    private static final String ALL_TYPES = "*";
    private static final String OVERRIDE_PARAM = "override";
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final List<ConfigurationFactory> factories;
    private final ConfigurationResolver configurationResolver;
    private final PropertyResolver propertyResolver;
    private final StrSubstitutor substitutor;
    private final LoggerContext loggerContext;

    @Inject
    public ConfigurationProvider(@Namespace(ConfigurationFactory.NAMESPACE) final List<ConfigurationFactory> factories,
                                 final ConfigurationResolver configurationResolver,
                                 final PropertyResolver propertyResolver,
                                 final StrSubstitutor substitutor,
                                 final LoggerContext context) {
        this.factories = factories;
        this.configurationResolver = configurationResolver;
        this.propertyResolver = propertyResolver;
        this.substitutor = substitutor;
        loggerContext = context;
    }

    public Configuration getConfiguration(final String name, final URI configLocation) {
        if (configLocation == null) {
            final String configLocationStr = propertyResolver.getString(Log4jProperties.CONFIG_LOCATION)
                    .map(substitutor::replace)
                    .orElse(null);
            if (configLocationStr != null) {
                final String[] sources = parseConfigLocations(configLocationStr);
                if (sources.length > 1) {
                    final List<AbstractConfiguration> configs = new ArrayList<>();
                    for (final String sourceLocation : sources) {
                        final Configuration config = getConfiguration(null, sourceLocation.trim());
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
            } else {
                final String log4j1ConfigStr = propertyResolver.getString(Log4jProperties.CONFIG_V1_LOCATION)
                        .map(substitutor::replace)
                        .orElse(null);
                if (log4j1ConfigStr != null) {
                    System.setProperty(Log4jProperties.CONFIG_V1_COMPATIBILITY_ENABLED, "true");
                    return getConfiguration(ConfigurationFactory.LOG4J1_VERSION, log4j1ConfigStr);
                }
            }

            for (final ConfigurationFactory factory : factories) {
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
                    final Configuration config = getConfiguration(null, sourceLocation.trim());
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
            for (final ConfigurationFactory factory : factories) {
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
        if (config != null) {
            return config;
        }
        LOGGER.warn("No Log4j configuration file found. " +
                        "Using default configuration (logging only errors to the console), " +
                        "or user programmatically provided configurations. " +
                        "Set system property 'log4j2.{}.System.debug' " +
                        "to show Log4j internal initialization logging. " +
                        // TODO: update link for 3.x
                        "See https://logging.apache.org/log4j/2.x/manual/configuration.html for instructions on how to configure Log4j",
                loggerContext.getName());
        return new DefaultConfiguration(loggerContext);
    }

    public Configuration getConfiguration(final String name, final URI configLocation, final ClassLoader loader) {
        if (loader == null) {
            return getConfiguration(name, configLocation);
        }
        if (ConfigurationFactory.isClassLoaderUri(configLocation)) {
            final String path = ConfigurationFactory.extractClassLoaderUriPath(configLocation);
            final Configuration configuration = configurationResolver.tryResolve(path, loader)
                    .map(this::getConfiguration)
                    .orElse(null);
            if (configuration != null) {
                return configuration;
            }
        }
        return getConfiguration(name, configLocation);
    }

    public Configuration getConfiguration(final ConfigurationSource source) {
        if (source == null) {
            LOGGER.error("Cannot process configuration, input source is null");
            return null;
        }
        final String config = source.getLocation();
        for (final ConfigurationFactory factory : factories) {
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
        LOGGER.error("Cannot process configuration at '{}'; no instance returned from any ConfigurationFactory", config);
        return null;
    }

    private Configuration getConfiguration(final String requiredVersion, final String configLocation) {
        ConfigurationSource source = null;
        try {
            source = configurationResolver.tryResolve(NetUtils.toURI(configLocation)).orElse(null);
        } catch (final Exception ex) {
            // Ignore the error and try as a String.
            LOGGER.catching(Level.DEBUG, ex);
        }
        if (source != null) {
            for (final ConfigurationFactory factory : factories) {
                if (requiredVersion != null && !factory.getVersion().equals(requiredVersion)) {
                    continue;
                }
                final String[] supportedTypes = factory.getSupportedTypes();
                if (supportedTypes != null) {
                    for (final String type : supportedTypes) {
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

    private Configuration getConfiguration(final boolean isTest, final String name) {
        final boolean named = Strings.isNotEmpty(name);
        final ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
        for (final ConfigurationFactory factory : factories) {
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

                final ConfigurationSource source = configurationResolver.tryResolve(configName, loader).orElse(null);
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

    private static String[] parseConfigLocations(final URI configLocations) {
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

    private static String[] parseConfigLocations(final String configLocations) {
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
}
