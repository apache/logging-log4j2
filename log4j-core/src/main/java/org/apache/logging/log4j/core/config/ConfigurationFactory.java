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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.BasicAuthorizationProvider;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;

/**
 * Factory class for parsed {@link Configuration} objects from a configuration file.
 * ConfigurationFactory allows the configuration implementation to be
 * dynamically chosen in 1 of 3 ways:
 * <ol>
 * <li>A system property named "log4j.configurationFactory" can be set with the
 * name of the ConfigurationFactory to be used.</li>
 * <li>An {@link Injector} binding for ConfigurationFactory may be registered.</li>
 * <li>
 * A ConfigurationFactory implementation can be added to the classpath and configured as a plugin in the
 * {@link #NAMESPACE ConfigurationFactory} category. The {@link Order} annotation should be used to configure the
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
        super();
        // TEMP For breakpoints
    }

    /**
     * Allows the ConfigurationFactory class to be specified as a system property.
     */
    public static final String CONFIGURATION_FACTORY_PROPERTY = Log4jProperties.CONFIG_CONFIGURATION_FACTORY_CLASS_NAME;

    /**
     * Allows the location of the configuration file to be specified as a system property.
     */
    public static final String CONFIGURATION_FILE_PROPERTY = Log4jProperties.CONFIG_LOCATION;

    public static final String LOG4J1_CONFIGURATION_FILE_PROPERTY = Log4jProperties.CONFIG_V1_FILE_NAME;

    public static final String LOG4J1_EXPERIMENTAL = Log4jProperties.CONFIG_V1_COMPATIBILITY_ENABLED;

    /**
     * Plugin category used to inject a ConfigurationFactory {@link org.apache.logging.log4j.plugins.Plugin}
     * class.
     *
     * @since 2.1
     */
    public static final String NAMESPACE = "ConfigurationFactory";

    public static final Key<ConfigurationFactory> KEY = new Key<>() {};

    public static final Key<PluginNamespace> PLUGIN_CATEGORY_KEY = new @Namespace(NAMESPACE) Key<>() {};

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

    private static final String[] PREFIXES = {"log4j2.", "log4j2.Configuration."};

    @Deprecated(since = "3.0.0", forRemoval = true)
    public static ConfigurationFactory getInstance() {
        return LoggerContext.getContext(false).getInjector().getInstance(KEY);
    }

    public static AuthorizationProvider authorizationProvider(final PropertyEnvironment props) {
        final String authClass = props.getStringProperty(PREFIXES, "authorizationProvider", null);
        AuthorizationProvider provider = null;
        if (authClass != null) {
            try {
                final Object obj = LoaderUtil.newInstanceOf(authClass);
                if (obj instanceof AuthorizationProvider) {
                    provider = (AuthorizationProvider) obj;
                } else {
                    LOGGER.warn("{} is not an AuthorizationProvider, using default", obj.getClass().getName());
                }
            } catch (final Exception ex) {
                LOGGER.warn("Unable to create {}, using default: {}", authClass, ex.getMessage());
            }
        }
        if (provider == null) {
            provider = new BasicAuthorizationProvider(props);
        }
        return provider;
    }

    protected StrSubstitutor substitutor;

    @Inject
    public void setSubstitutor(final StrSubstitutor substitutor) {
        this.substitutor = substitutor;
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
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation) {
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
    public Configuration getConfiguration(final LoggerContext loggerContext, final String name, final URI configLocation, final ClassLoader loader) {
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

}
