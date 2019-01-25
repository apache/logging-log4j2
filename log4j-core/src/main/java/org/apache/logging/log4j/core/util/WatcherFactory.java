/*
 * Copyright (c) 2019 Nextiva, Inc. to Present.
 * All rights reserved.
 */
package org.apache.logging.log4j.core.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFileWatcher;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Creates Watchers of various types.
 */
public class WatcherFactory {

    private static Logger LOGGER = StatusLogger.getLogger();
    private static PluginManager pluginManager = new PluginManager(Watcher.CATEGORY);

    private static volatile WatcherFactory factory = null;

    private final Map<String, PluginType<?>> plugins;

    private WatcherFactory(List<String> packages) {
        pluginManager.collectPlugins(packages);
        plugins = pluginManager.getPlugins();
    }

    public static WatcherFactory getInstance(List<String> packages) {
        if (factory == null) {
            synchronized(pluginManager) {
                if (factory == null) {
                    factory = new WatcherFactory(packages);
                }
            }
        }
        return factory;
    }

    @SuppressWarnings("unchecked")
    public Watcher newWatcher(Source source, final Configuration configuration, final Reconfigurable reconfigurable,
        final List<ConfigurationListener> configurationListeners, long lastModifiedMillis) {
        if (source.getFile() != null) {
            return new ConfigurationFileWatcher(configuration, reconfigurable, configurationListeners,
                lastModifiedMillis);
        } else {
            String name = source.getURI().getScheme();
            PluginType<?> pluginType = plugins.get(name);
            if (pluginType != null) {
                return instantiate(name, (Class<? extends Watcher>) pluginType.getPluginClass(),
                    configuration, reconfigurable, configurationListeners, lastModifiedMillis);
            }
            LOGGER.info("No Watcher plugin is available for protocol '{}'", name);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Watcher> T instantiate(String name, final Class<T> clazz, final Configuration configuration,
        final Reconfigurable reconfigurable, final List<ConfigurationListener> listeners, long lastModifiedMillis) {
        Objects.requireNonNull(clazz, "No class provided");
        try {
            Constructor constructor = clazz.getConstructor(Configuration.class, Reconfigurable.class, List.class, long.class);
            return (T) constructor.newInstance(configuration, reconfigurable, listeners, lastModifiedMillis);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("No valid constructor for Watcher plugin " + name, ex);
        } catch (final LinkageError | InstantiationException e) {
            // LOG4J2-1051
            // On platforms like Google App Engine and Android, some JRE classes are not supported: JMX, JNDI, etc.
            throw new IllegalArgumentException(e);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            Throwables.rethrow(e.getCause());
            throw new InternalError("Unreachable");
        }
    }
}
