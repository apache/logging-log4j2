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
package org.apache.logging.log4j.util;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * <em>Consider this class private.</em>
 * <p>
 * Provides utility methods for managing {@link Properties} instances as well as access to the global configuration
 * system. Properties by default are loaded from the system properties, system environment, and a classpath resource
 * file named {@value #LOG4J_PROPERTIES_FILE_NAME}. Additional properties can be loaded by implementing a custom
 * {@link PropertySource} service and specifying it via a {@link ServiceLoader} file called
 * {@code META-INF/services/org.apache.logging.log4j.util.PropertySource} with a list of fully qualified class names
 * implementing that interface.
 * </p>
 *
 * @see PropertySource
 */
@InternalApi
public class PropertiesUtil implements PropertyEnvironment {

    private static final String LOG4J_PROPERTIES_FILE_NAME = "log4j2.component.properties";
    private static final String LOG4J_SYSTEM_PROPERTIES_FILE_NAME = "log4j2.system.properties";
    private static final Lazy<PropertiesUtil> COMPONENT_PROPERTIES =
            Lazy.lazy(() -> new PropertiesUtil(LOG4J_PROPERTIES_FILE_NAME));

    private final Environment environment;

    /**
     * Constructs a PropertiesUtil using a given Properties object as its source of defined properties.
     *
     * @param props the Properties to use by default
     */
    public PropertiesUtil(final Properties props) {
        this(new PropertiesPropertySource(props));
    }

    /**
     * Constructs a PropertiesUtil for a given properties file name on the classpath. The properties specified in this
     * file are used by default. If a property is not defined in this file, then the equivalent system property is used.
     *
     * @param propertiesFileName the location of properties file to load
     */
    public PropertiesUtil(final String propertiesFileName) {
        this(propertiesFileName, true);
    }

    private PropertiesUtil(final String propertiesFileName, final boolean useTccl) {
        this.environment = new Environment(new PropertyFilePropertySource(propertiesFileName, useTccl));
    }

    /**
     * Constructs a PropertiesUtil for a give property source as source of additional properties.
     * @param source a property source
     */
    PropertiesUtil(final PropertySource source) {
        this.environment = new Environment(source);
    }

    /**
     * Returns the PropertiesUtil used by Log4j.
     *
     * @return the main Log4j PropertiesUtil instance.
     */
    public static PropertiesUtil getProperties() {
        return COMPONENT_PROPERTIES.value();
    }

    public static PropertyEnvironment getProperties(final String namespace) {
        return new Environment(new PropertyFilePropertySource(String.format("log4j2.%s.properties", namespace)));
    }

    public static ResourceBundle getCharsetsResourceBundle() {
        return ResourceBundle.getBundle("Log4j-charsets");
    }

    @Override
    public void addPropertySource(PropertySource propertySource) {
        if (environment != null) {
            environment.addPropertySource(propertySource);
        }
    }

    /**
     * Returns {@code true} if the specified property is defined, regardless of its value (it may not have a value).
     *
     * @param name the name of the property to verify
     * @return {@code true} if the specified property is defined, regardless of its value
     */
    @Override
    public boolean hasProperty(final String name) {
        return environment.hasProperty(name);
    }

    /**
     * Gets the named property as a double.
     *
     * @param name         the name of the property to look up
     * @param defaultValue the default value to use if the property is undefined
     * @return the parsed double value of the property or {@code defaultValue} if it was undefined or could not be parsed.
     */
    public double getDoubleProperty(final String name, final double defaultValue) {
        final String prop = getStringProperty(name);
        if (prop != null) {
            try {
                return Double.parseDouble(prop);
            } catch (final Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Gets the named property as a String.
     *
     * @param name the name of the property to look up
     * @return the String value of the property or {@code null} if undefined.
     */
    @Override
    public String getStringProperty(final String name) {
        return environment.getStringProperty(name);
    }

    /**
     * Return the system properties or an empty Properties object if an error occurs.
     *
     * @return The system properties.
     */
    public static Properties getSystemProperties() {
        try {
            return new Properties(System.getProperties());
        } catch (final SecurityException ex) {
            LowLevelLogUtil.logException("Unable to access system properties.", ex);
            // Sandboxed - can't read System Properties
            return new Properties();
        }
    }

    /**
     * Reloads all properties. This is primarily useful for unit tests.
     *
     * @since 2.10.0
     */
    public void reload() {
        environment.reload();
    }

    /**
     * Provides support for looking up global configuration properties via environment variables, property files,
     * and system properties, in three variations:
     * <p>
     * Normalized: all log4j-related prefixes removed, remaining property is camelCased with a log4j2 prefix for
     * property files and system properties, or follows a LOG4J_FOO_BAR format for environment variables.
     * <p>
     * Legacy: the original property name as defined in the source pre-2.10.0.
     * <p>
     * Tokenized: loose matching based on word boundaries.
     *
     * @since 2.10.0
     */
    private static class Environment implements PropertyEnvironment {

        private final Set<PropertySource> sources = new ConcurrentSkipListSet<>(new PropertySource.Comparator());
        /**
         * Maps a key to its value in the lowest priority source that contains it.
         */
        private final Map<String, String> literal = new ConcurrentHashMap<>();
        /**
         * Maps a key to the value associated to its normalization in the lowest
         * priority source that contains it.
         */
        private final Map<String, String> normalized = new ConcurrentHashMap<>();
        private final Map<List<CharSequence>, String> tokenized = new ConcurrentHashMap<>();

        private Environment(final PropertySource propertySource) {
            final PropertyFilePropertySource sysProps = new PropertyFilePropertySource(LOG4J_SYSTEM_PROPERTIES_FILE_NAME);
            try {
                sysProps.forEach((key, value) -> {
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                });
            } catch (final SecurityException ex) {
                // Access to System Properties is restricted so just skip it.
            }
            sources.add(propertySource);
            final ServiceRegistry registry = ServiceRegistry.getInstance();
            // Does not log errors using StatusLogger, which depends on PropertiesUtil being initialized.
            sources.addAll(registry.getServices(PropertySource.class, MethodHandles.lookup(), null, /*verbose=*/false));
            reload();
        }

        @Override
        public void addPropertySource(final PropertySource propertySource) {
            sources.add(propertySource);
        }

        private synchronized void reload() {
            literal.clear();
            normalized.clear();
            tokenized.clear();
            // 1. Collects all property keys from enumerable sources.
            final Set<String> keys = new HashSet<>();
            sources.stream()
                   .map(PropertySource::getPropertyNames)
                   .forEach(keys::addAll);
            // 2. Fills the property caches. Sources with higher priority values don't override the previous ones.
            keys.stream()
                .filter(Objects::nonNull)
                .forEach(key -> {
                    final List<CharSequence> tokens = PropertySource.Util.tokenize(key);
                    final boolean hasTokens = !tokens.isEmpty();
                    sources.forEach(source -> {
                        if (source.containsProperty(key)) {
                            final String value = source.getProperty(key);
                            literal.putIfAbsent(key, value);
                            if (hasTokens) {
                                tokenized.putIfAbsent(tokens, value);
                            }
                        }
                        if (hasTokens) {
                            final String normalKey = Objects.toString(source.getNormalForm(tokens), null);
                            if (normalKey != null && source.containsProperty(normalKey)) {
                                normalized.putIfAbsent(key, source.getProperty(normalKey));
                            }
                        }
                    });
                });
        }

        @Override
        public String getStringProperty(final String key) {
            if (normalized.containsKey(key)) {
                return normalized.get(key);
            }
            if (literal.containsKey(key)) {
                return literal.get(key);
            }
            final List<CharSequence> tokens = PropertySource.Util.tokenize(key);
            final boolean hasTokens = !tokens.isEmpty();
            for (final PropertySource source : sources) {
                if (hasTokens) {
                    final String normalKey = Objects.toString(source.getNormalForm(tokens), null);
                    if (normalKey != null && source.containsProperty(normalKey)) {
                        return source.getProperty(normalKey);
                    }
                }
                if (source.containsProperty(key)) {
                    return source.getProperty(key);
                }
            }
            return tokenized.get(tokens);
        }

        @Override
        public boolean hasProperty(final String key) {
            List<CharSequence> tokens = PropertySource.Util.tokenize(key);
            return normalized.containsKey(key) ||
                   literal.containsKey(key) ||
                   tokenized.containsKey(tokens) ||
                   sources.stream().anyMatch(s -> {
                        final CharSequence normalizedKey = s.getNormalForm(tokens);
                        return s.containsProperty(key) || normalizedKey != null && s.containsProperty(normalizedKey.toString());
                   });
        }
    }

    /**
     * Extracts properties that start with or are equals to the specific prefix and returns them in a new Properties
     * object with the prefix removed.
     *
     * @param properties The Properties to evaluate.
     * @param prefix     The prefix to extract.
     * @return The subset of properties.
     */
    public static Properties extractSubset(final Properties properties, final String prefix) {
        final Properties subset = new Properties();

        if (prefix == null || prefix.length() == 0) {
            return subset;
        }

        final String prefixToMatch = prefix.charAt(prefix.length() - 1) != '.' ? prefix + '.' : prefix;

        final List<String> keys = new ArrayList<>();

        for (final String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefixToMatch)) {
                subset.setProperty(key.substring(prefixToMatch.length()), properties.getProperty(key));
                keys.add(key);
            }
        }
        for (final String key : keys) {
            properties.remove(key);
        }

        return subset;
    }

    /**
     * Partitions a properties map based on common key prefixes up to the first period.
     *
     * @param properties properties to partition
     * @return the partitioned properties where each key is the common prefix (minus the period) and the values are
     * new property maps without the prefix and period in the key
     * @since 2.6
     */
    public static Map<String, Properties> partitionOnCommonPrefixes(final Properties properties) {
        return partitionOnCommonPrefixes(properties, false);
    }

    /**
     * Partitions a properties map based on common key prefixes up to the first period.
     *
     * @param properties properties to partition
     * @param includeBaseKey when true if a key exists with no '.' the key will be included.
     * @return the partitioned properties where each key is the common prefix (minus the period) and the values are
     * new property maps without the prefix and period in the key
     * @since 2.17.2
     */
    public static Map<String, Properties> partitionOnCommonPrefixes(final Properties properties,
            final boolean includeBaseKey) {
        final Map<String, Properties> parts = new ConcurrentHashMap<>();
        for (final String key : properties.stringPropertyNames()) {
            final int idx = key.indexOf('.');
            if (idx < 0) {
                if (includeBaseKey) {
                    if (!parts.containsKey(key)) {
                        parts.put(key, new Properties());
                    }
                    parts.get(key).setProperty("", properties.getProperty(key));
                }
                continue;
            }
            final String prefix = key.substring(0, idx);
            if (!parts.containsKey(prefix)) {
                parts.put(prefix, new Properties());
            }
            parts.get(prefix).setProperty(key.substring(idx + 1), properties.getProperty(key));
        }
        return parts;
    }


}
