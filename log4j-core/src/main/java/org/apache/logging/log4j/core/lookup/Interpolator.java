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
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationAware;
import org.apache.logging.log4j.core.config.LoggerContextAware;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.status.StatusLogger;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Proxies other {@link StrLookup}s using a keys within ${} markers.
 */
public class Interpolator extends AbstractConfigurationAwareLookup implements LoggerContextAware {

    /** Constant for the prefix separator. */
    public static final char PREFIX_SEPARATOR = ':';

    private static final String LOOKUP_KEY_WEB = "web";

    private static final String LOOKUP_KEY_DOCKER = "docker";

    private static final String LOOKUP_KEY_KUBERNETES = "kubernetes";

    private static final String LOOKUP_KEY_SPRING = "spring";

    private static final String LOOKUP_KEY_JNDI = "jndi";

    private static final String LOOKUP_KEY_JVMRUNARGS = "jvmrunargs";

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Map<String, Supplier<? extends StrLookup>> strLookups = new ConcurrentHashMap<>();

    private final StrLookup defaultLookup;

    private WeakReference<LoggerContext> loggerContext = null;

    // Used by tests
    public Interpolator(final StrLookup defaultLookup) {
        this(defaultLookup, List.of());
    }

    /**
     * Constructs an Interpolator using a given StrLookup and a list of packages to find Lookup plugins in.
     * Only used in the Interpolator.
     *
     * @param defaultLookup  the default StrLookup to use as a fallback
     * @param pluginPackages a list of packages to scan for Lookup plugins
     * @since 2.1
     */
    public Interpolator(final StrLookup defaultLookup, final List<String> pluginPackages) {
        this.defaultLookup = defaultLookup == null ? new PropertiesLookup(Map.of()) : defaultLookup;
        final Injector injector = DI.createInjector();
        injector.registerBinding(Keys.PLUGIN_PACKAGES_KEY, () -> pluginPackages);
        injector.getInstance(PLUGIN_CATEGORY_KEY)
                .forEach((key, value) -> {
                    try {
                        strLookups.put(key, injector.getFactory(value.getPluginClass().asSubclass(StrLookup.class)));
                    } catch (final Throwable t) {
                        handleError(key, t);
                    }
                });
    }

    /**
     * Used by interpolatrorFactory.
     *
     * @param defaultLookup The default Lookup.
     * @param strLookupPlugins The Lookup Plugins.
     */
    public Interpolator(final StrLookup defaultLookup, final Map<String, Supplier<StrLookup>> strLookupPlugins) {
        this.defaultLookup = defaultLookup;
        strLookups.putAll(strLookupPlugins);
    }

    /**
     * Create the default Interpolator.
     */
    public Interpolator() {
        this(Map.of());
    }

    /**
     * Creates the default Interpolator with the provided properties.
     */
    public Interpolator(final Map<String, String> properties) {
        this(new PropertiesLookup(properties), List.of());
    }

    public StrLookup getDefaultLookup() {
        return defaultLookup;
    }

    public Map<String, StrLookup> getStrLookupMap() {
        return strLookups.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    private void handleError(final String lookupKey, final Throwable t) {
        switch (lookupKey) {
            case LOOKUP_KEY_JNDI:
                // java.lang.VerifyError: org/apache/logging/log4j/core/lookup/JndiLookup
                LOGGER.warn( // LOG4J2-1582 don't print the whole stack trace (it is just a warning...)
                        "JNDI lookup class is not available because this JRE does not support JNDI." +
                        " JNDI string lookups will not be available, continuing configuration. Ignoring " + t);
                break;
            case LOOKUP_KEY_JVMRUNARGS:
                // java.lang.VerifyError: org/apache/logging/log4j/core/lookup/JmxRuntimeInputArgumentsLookup
                LOGGER.warn(
                        "JMX runtime input lookup class is not available because this JRE does not support JMX. " +
                        "JMX lookups will not be available, continuing configuration. Ignoring " + t);
                break;
            case LOOKUP_KEY_WEB:
                LOGGER.info("Log4j appears to be running in a Servlet environment, but there's no log4j-web module " +
                        "available. If you want better web container support, please add the log4j-web JAR to your " +
                        "web archive or server lib directory.");
                break;
            case LOOKUP_KEY_DOCKER: case LOOKUP_KEY_SPRING:
                break;
            case LOOKUP_KEY_KUBERNETES:
                if (t instanceof NoClassDefFoundError) {
                    LOGGER.warn("Unable to create Kubernetes lookup due to missing dependency: {}", t.getMessage());
                }
                break;
            default:
                LOGGER.error("Unable to create Lookup for {}", lookupKey, t);
        }
    }

    /**
     * Resolves the specified variable. This implementation will try to extract
     * a variable prefix from the given variable name (the first colon (':') is
     * used as prefix separator). It then passes the name of the variable with
     * the prefix stripped to the lookup object registered for this prefix. If
     * no prefix can be found or if the associated lookup object cannot resolve
     * this variable, the default lookup object will be used.
     *
     * @param event The current LogEvent or null.
     * @param var the name of the variable whose value is to be looked up
     * @return the value of this variable or <b>null</b> if it cannot be
     * resolved
     */
    @Override
    public String lookup(final LogEvent event, String var) {
        if (var == null) {
            return null;
        }

        final int prefixPos = var.indexOf(PREFIX_SEPARATOR);
        if (prefixPos >= 0) {
            final String prefix = var.substring(0, prefixPos).toLowerCase(Locale.US);
            final String name = var.substring(prefixPos + 1);
            final Supplier<? extends StrLookup> lookupSupplier = strLookups.get(prefix);
            String value = null;
            if (lookupSupplier != null) {
                final StrLookup lookup = lookupSupplier.get();
                if (lookup instanceof ConfigurationAware) {
                    ((ConfigurationAware) lookup).setConfiguration(configuration);
                }
                if (lookup instanceof LoggerContextAware) {
                    ((LoggerContextAware) lookup).setLoggerContext(loggerContext.get());
                }
                value = event == null ? lookup.lookup(name) : lookup.lookup(event, name);
            }

            if (value != null) {
                return value;
            }
            var = var.substring(prefixPos + 1);
        }
        if (defaultLookup != null) {
            return event == null ? defaultLookup.lookup(var) : defaultLookup.lookup(event, var);
        }
        return null;
    }

    public void setLoggerContext(final LoggerContext loggerContext) {
        if (loggerContext == null) {
            return;
        }
        this.loggerContext = new WeakReference<>(loggerContext);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final String name : strLookups.keySet()) {
            if (sb.length() == 0) {
                sb.append('{');
            } else {
                sb.append(", ");
            }

            sb.append(name);
        }
        if (sb.length() > 0) {
            sb.append('}');
        }
        return sb.toString();
    }

}
