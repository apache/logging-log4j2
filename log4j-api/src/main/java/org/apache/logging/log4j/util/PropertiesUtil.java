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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContext;

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

    private static final String LOG4J_NAMESPACE = "component";
    private static final String LOG4J_PREFIX = "log4j2.";
    private static final String LOG4J_CONTEXT_PREFIX = "log4j2.context.";
    private static final String DELIMITER = ".";
    private static final String META_INF = "META-INF/";
    private static final String LOG4J_SYSTEM_PROPERTIES_FILE_NAME = "log4j2.system.properties";
    private static final Lazy<PropertiesUtil> COMPONENT_PROPERTIES = Lazy.lazy(PropertiesUtil::new);

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
     * Constructs a PropertiesUtil using a given Properties object as its source of defined properties.
     *
     * @param props the Properties to use by default
     * @param includeInvalid includes invalid properties.
     */
    public PropertiesUtil(final Properties props, final boolean includeInvalid) {
        this(new PropertiesPropertySource(props, PropertySource.SYSTEM_CONTEXT,
                PropertySource.DEFAULT_PRIORITY, includeInvalid));
    }

    /**
     * Constructs a PropertiesUtil for a given properties file name on the classpath. The properties specified in this
     * file are used by default. If a property is not defined in this file, then the equivalent system property is used.
     *
     * @param propertiesFileName the location of properties file to load
     */
    public PropertiesUtil(final String propertiesFileName) {
        this(PropertySource.SYSTEM_CONTEXT, propertiesFileName, true);
    }
    /**
     * Constructs a PropertiesUtil for a given properties file name on the classpath. The properties specified in this
     * file are used by default. If a property is not defined in this file, then the equivalent system property is used.
     *
     * @param propertiesFileName the location of properties file to load
     */
    public PropertiesUtil(final String contextName, final String propertiesFileName) {
        this(contextName, propertiesFileName, true);
    }

    private PropertiesUtil(final String contextName, final String propertiesFileName, final boolean useTccl) {
        List<PropertySource> sources = new ArrayList<>();
        if (propertiesFileName.endsWith(".json") || propertiesFileName.endsWith(".jsn")) {
            PropertySource source = getJsonPropertySource(propertiesFileName, useTccl, 50);
            if (source != null) {
                sources.add(source);
            }
        } else {
            PropertySource source = new PropertiesPropertySource(loadPropertiesFile(propertiesFileName, useTccl),
                    null, 60, true);
            sources.add(source);
        }
        this.environment = new Environment(contextName, sources);
    }

    private PropertiesUtil() {
        this.environment = getEnvironment(LOG4J_NAMESPACE, true);
    }

    /**
     * Constructs a PropertiesUtil for a given property source as source of additional properties.
     * @param source a property source
     */
    public PropertiesUtil(final PropertySource source) {
        List<PropertySource> sources = Collections.singletonList(source);
        this.environment = new Environment(PropertySource.SYSTEM_CONTEXT, sources);
    }

    private PropertiesUtil(final String contextName, final List<PropertySource> sources) {
        this.environment = new Environment(contextName, sources);
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
        return getEnvironment(namespace, true);
    }

    private static Environment getEnvironment(final String namespace, final boolean useTccl) {
        List<PropertySource> sources = new ArrayList<>();
        PropertySource source = new PropertiesPropertySource(loadPropertiesFile(
                        String.format("log4j2.%s.properties", namespace), useTccl), 50);
        sources.add(source);
        source = getJsonPropertySource(String.format("log4j2.%s.json", namespace), useTccl, 60);
        if (source != null) {
            sources.add(source);
        }
        return new Environment(PropertySource.SYSTEM_CONTEXT, sources);
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
     * Locates the PropertyEnvironment for the LoggerContext, if available.
     * @param classLoader The ClassLoader to use.
     * @return the located PropertyEnvironment or null if one has not been created.
     */
    public static PropertyEnvironment locateProperties(ClassLoader classLoader) {
        if (LogManager.getFactory().hasContext(Activator.class.getName(), classLoader, false)) {
            LoggerContext ctx = LogManager.getFactory().getContext(Activator.class.getName(),
                    classLoader, null, false);
            return ctx.getProperties();
        } else {
            return null;
        }
    }

    /**
     * Get the properties for a LoggerContext just by the name. This ALWAYS creates a new PropertiesUtil. Use
     * locateProperties to obtain the current properties.
     * @param contextName The context name.
     * @return The PropertiesUtil that was created.
     */
    public static PropertiesUtil getContextProperties(String contextName) {
        return getContextProperties(contextName, getProperties());
    }

    /**
     * Get the properties for a LoggerContext just by the name. This method is used for testing.
     * @param contextName The context name.
     * @return The PropertiesUtil that was created.
     */
    public static PropertiesUtil getContextProperties(String contextName, PropertiesUtil propertiesUtil) {
        ClassLoader classLoader = PropertiesUtil.class.getClassLoader();
        String filePrefix = META_INF + LOG4J_PREFIX + contextName + ".";
        List<PropertySource> contextSources = new ArrayList<>();
        contextSources.addAll(getContextPropertySources(classLoader, filePrefix, contextName));
        contextSources.addAll(propertiesUtil.environment.sources);
        return new PropertiesUtil(contextName, contextSources);
    }

    /**
     * Get the properties associated with a LoggerContext that is associated with a ClassLoader. This ALWAYS creates
     * a new PropertiesUtil. Use locateProperties to obtain the current properties.
     * @param classLoader The ClassLoader.
     * @param contextName The context name.
     * @return The PropertiesUtil created.
     */
    public static PropertiesUtil getContextProperties(final ClassLoader classLoader, final String contextName) {
        String filePrefix = META_INF + LOG4J_CONTEXT_PREFIX;
        List<PropertySource> contextSources = new ArrayList<>();
        contextSources.addAll(getContextPropertySources(classLoader, filePrefix, contextName));
        contextSources.addAll(getProperties().environment.sources);
        return new PropertiesUtil(contextName, contextSources);
    }

    private static Properties loadPropertiesFile(final String fileName, final boolean useTccl) {
        final Properties props = new Properties();
        for (final URL url : LoaderUtil.findResources(fileName, useTccl)) {
            try (final InputStream in = url.openStream()) {
                props.load(in);
            } catch (final IOException e) {
                LowLevelLogUtil.logException("Unable to read " + url, e);
            }
        }
        return props;
    }

    private static List<PropertySource> getContextPropertySources(final ClassLoader classLoader,
                                                                  final String filePrefix, final String contextName) {
        List<PropertySource> sources = new ArrayList<>();
        String fileName = filePrefix + "json";
        try {
            Enumeration<URL> urls = classLoader.getResources(fileName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream is = url.openStream()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    PropertySource propertySource = parseJsonProperties(json, contextName, 10);
                    if (propertySource != null) {
                        sources.add(propertySource);
                    }
                } catch (Exception ex) {
                    LowLevelLogUtil.logException("Unable to parse JSON for " + url.toString(), ex);
                }
            }
        } catch (Exception ex) {
            LowLevelLogUtil.logException("Unable to access " + fileName, ex);
        }
        fileName = filePrefix + "properties";
        try {
            Enumeration<URL> urls = classLoader.getResources(fileName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                try (InputStream is = url.openStream()) {
                    Properties props = new Properties();
                    props.load(is);
                    PropertySource source = new PropertiesPropertySource(props, contextName, 20);
                    sources.add(source);
                } catch (Exception ex) {
                    LowLevelLogUtil.logException("Unable to access properties for " + url.toString(), ex);
                }
            }
        } catch (Exception ex) {
            LowLevelLogUtil.logException("Unable to access " + fileName, ex);
        }
        return sources;
    }

    private static PropertySource getJsonPropertySource(final String fileName, final boolean useTccl, int priority) {
        if (fileName.startsWith("file://")) {
            try {
                URL url = new URL(fileName);
                try (InputStream is = url.openStream()) {
                    return parseJsonProperties(new String(is.readAllBytes(), StandardCharsets.UTF_8), "*",
                            priority);
                }
            } catch (Exception ex) {
                LowLevelLogUtil.logException("Unable to read " + fileName, ex);
            }
        } else {
            File file = new File(fileName);
            if (file.exists()) {
                try {
                    return parseJsonProperties(new String(new FileInputStream(file).readAllBytes(),
                            StandardCharsets.UTF_8), "*", priority);
                } catch (IOException ioe) {
                    LowLevelLogUtil.logException("Unable to read " + fileName, ioe);
                }
            } else {
                for (final URL url : LoaderUtil.findResources(fileName, useTccl)) {
                    try (final InputStream in = url.openStream()) {
                        return parseJsonProperties(new String(in.readAllBytes(),
                                StandardCharsets.UTF_8), "*", priority);
                    } catch (final IOException e) {
                        LowLevelLogUtil.logException("Unable to read " + url, e);
                    }
                }
            }
        }
        return null;
    }

    private static PropertySource parseJsonProperties(final String json, final String contextName, final int priority) {
        final Map<String, Object> root = Cast.cast(JsonReader.read(json));
        Properties props = new Properties();
        populateProperties(props, "", root);
        return new PropertiesPropertySource(props, contextName, priority);
    }

    private static void populateProperties(Properties props, String prefix, Map<String, Object> root) {
        if (!root.isEmpty()) {
            for (Map.Entry<String, Object> entry : root.entrySet()) {
                if (entry.getValue() instanceof String) {
                    props.setProperty(createKey(prefix, entry.getKey()), (String) entry.getValue());
                } else if (entry.getValue() instanceof List) {
                    final StringBuilder sb = new StringBuilder();
                    List<Object> entries = Cast.cast(entry.getValue());
                    entries.forEach((obj) -> {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(obj.toString());
                    });
                    props.setProperty(createKey(prefix, entry.getKey()), sb.toString());
                } else {
                    populateProperties(props, createKey(prefix, entry.getKey()), Cast.cast(entry.getValue()));
                }
            }
        }
    }

    private static String createKey(String prefix, String suffix) {
        if (prefix.isEmpty()) {
            return suffix;
        }
        return prefix + "." + suffix;
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
         * Maps a key to its value or the value of its normalization in the lowest priority source that contains it.
         */
        private final Map<String, String> literal = new ConcurrentHashMap<>();

        private final String contextName;

        private Environment(final String contextName, final List<PropertySource> propertySources) {
            try {
                Properties sysProps = loadPropertiesFile(LOG4J_SYSTEM_PROPERTIES_FILE_NAME, false);
                for (String key : sysProps.stringPropertyNames()) {
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, sysProps.getProperty(key));
                    }
                };
            } catch (final SecurityException ex) {
                // Access to System Properties is restricted so just skip it.
            }
            sources.addAll(propertySources);
            final ServiceRegistry registry = ServiceRegistry.getInstance();
            // Does not log errors using StatusLogger, which depends on PropertiesUtil being initialized.
            sources.addAll(registry.getServices(PropertySource.class, MethodHandles.lookup(), null, /*verbose=*/false));
            this.contextName = contextName;
            reload();
        }

        @Override
        public void addPropertySource(final PropertySource propertySource) {
            sources.add(propertySource);
        }

        private synchronized void reload() {
            literal.clear();
            sources.forEach((s) -> {
                if (s instanceof ReloadablePropertySource) {
                    ((ReloadablePropertySource)s).reload();
                }
            });
            // 1. Collects all property keys from enumerable sources.
            final Set<String> keys = new HashSet<>();
            sources.stream()
                   .map(PropertySource::getPropertyNames)
                   .forEach(keys::addAll);
            // 2. Fills the property caches. Sources with higher priority values don't override the previous ones.
            keys.stream()
                .filter(Objects::nonNull)
                .forEach(key -> {
                    String contextKey = getContextKey(key);
                    if (contextName != null && !contextName.equals(PropertySource.SYSTEM_CONTEXT)) {
                        sources.forEach(source -> {
                            if (source instanceof ContextAwarePropertySource) {
                                ContextAwarePropertySource src = Cast.cast(source);
                                if (src.containsProperty(contextName, contextKey)) {
                                    literal.putIfAbsent(key, src.getProperty(contextName, contextKey));
                                }
                            }
                        });
                    }
                    sources.forEach(source -> {
                        if (source.containsProperty(contextKey)) {
                            literal.putIfAbsent(key, source.getProperty(contextKey));
                        }
                    });
                });
        }

        @Override
        public String getStringProperty(final String key) {
            if (literal.containsKey(key)) {
                return literal.get(key);
            }
            String result = null;
            String contextKey = getContextKey(key);
            if (contextName != null && !contextName.equals(PropertySource.SYSTEM_CONTEXT)) {
                for (final PropertySource source : sources) {
                    if (source instanceof ContextAwarePropertySource) {
                        ContextAwarePropertySource src = Cast.cast(source);
                        result = src.getProperty(contextName, contextKey);
                    }
                    if (result != null) {
                        return result;
                    }
                }
            }
            for (final PropertySource source : sources) {
                result = source.getProperty(contextKey);
                if (result != null) {
                    return result;
                }
            }
            return result;
        }

        @Override
        public boolean hasProperty(final String key) {
            if (literal.containsKey(key)) {
                return true;
            }
            String contextKey = getContextKey(key);
            if (!contextName.equals(PropertySource.SYSTEM_CONTEXT)) {
                for (final PropertySource source : sources) {
                    if (source instanceof ContextAwarePropertySource) {
                        ContextAwarePropertySource src = Cast.cast(source);
                        if (src.containsProperty(contextName, contextKey)) {
                            return true;
                        }
                    }
                }
            }
            for (final PropertySource source : sources) {
                if (source instanceof ContextAwarePropertySource) {
                    ContextAwarePropertySource src = Cast.cast(source);
                    if (src.containsProperty(contextName, contextKey)
                            || (!contextName.equals(PropertySource.SYSTEM_CONTEXT)
                            && src.containsProperty(PropertySource.SYSTEM_CONTEXT, contextKey))) {
                        return true;
                    }
                } else {
                    if (source.containsProperty(key)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private String getContextKey(String key) {
            String keyToCheck = key;
            if (keyToCheck.startsWith(LOG4J_PREFIX)) {
                List<CharSequence> tokens = PropertySource.Util.tokenize(key);
                if (tokens.size() > 3) {
                    keyToCheck = PropertySource.Util.join(tokens.subList(2, tokens.size())).toString();
                }
            }
            return keyToCheck;
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
