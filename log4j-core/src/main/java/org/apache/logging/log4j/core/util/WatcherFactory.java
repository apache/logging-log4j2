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
package org.apache.logging.log4j.core.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFileWatcher;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Creates Watchers of various types.
 */
public class WatcherFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final PluginNamespace plugins;

    public WatcherFactory(final PluginNamespace watcherPlugins) {
        plugins = watcherPlugins;
    }

    public Watcher newWatcher(final Source source, final Configuration configuration, final Reconfigurable reconfigurable,
                              final List<ConfigurationListener> configurationListeners, final long lastModifiedMillis) {
        if (source.getFile() != null) {
            return new ConfigurationFileWatcher(configuration, reconfigurable, configurationListeners,
                lastModifiedMillis);
        } else {
            final String name = source.getURI().getScheme();
            final PluginType<?> pluginType = plugins.get(name);
            if (pluginType != null) {
                return instantiate(name, pluginType.getPluginClass().asSubclass(Watcher.class), configuration,
                    reconfigurable, configurationListeners, lastModifiedMillis);
            }
            LOGGER.info("No Watcher plugin is available for protocol '{}'", name);
            return null;
        }
    }

    public static <T extends Watcher> T instantiate(final String name, final Class<T> clazz,
                                                    final Configuration configuration, final Reconfigurable reconfigurable,
                                                    final List<ConfigurationListener> listeners, final long lastModifiedMillis) {
        Objects.requireNonNull(clazz, "No class provided");
        try {
            final Constructor<T> constructor = clazz
                .getConstructor(Configuration.class, Reconfigurable.class, List.class, long.class);
            return constructor.newInstance(configuration, reconfigurable, listeners, lastModifiedMillis);
        } catch (final NoSuchMethodException ex) {
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
