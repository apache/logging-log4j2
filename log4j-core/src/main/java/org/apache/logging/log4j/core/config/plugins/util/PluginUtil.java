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
package org.apache.logging.log4j.core.config.plugins.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * {@link org.apache.logging.log4j.core.config.plugins.Plugin} utilities.
 *
 * @see PluginManager
 */
public final class PluginUtil {

    private PluginUtil() {}

    /**
     * Shortcut for collecting plugins matching with the given {@code category}.
     */
    public static Map<String, PluginType<?>> collectPluginsByCategory(final String category) {
        Objects.requireNonNull(category, "category");
        return collectPluginsByCategoryAndPackage(category, Collections.emptyList());
    }

    /**
     * Short for collecting plugins matching with the given {@code category} in provided {@code packages}.
     */
    public static Map<String, PluginType<?>> collectPluginsByCategoryAndPackage(
            final String category, final List<String> packages) {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(packages, "packages");
        final PluginManager pluginManager = new PluginManager(category);
        pluginManager.collectPlugins(packages);
        return pluginManager.getPlugins();
    }

    /**
     * Instantiates the given plugin using its no-arg {@link PluginFactory}-annotated static method.
     * @throws IllegalStateException if instantiation fails
     */
    public static <V> V instantiatePlugin(final Class<V> pluginClass) {
        Objects.requireNonNull(pluginClass, "pluginClass");
        final Method pluginFactoryMethod = findPluginFactoryMethod(pluginClass);
        try {
            @SuppressWarnings("unchecked")
            final V instance = (V) pluginFactoryMethod.invoke(null);
            return instance;
        } catch (IllegalAccessException | InvocationTargetException error) {
            final String message = String.format(
                    "failed to instantiate plugin of type %s using the factory method %s",
                    pluginClass, pluginFactoryMethod);
            throw new IllegalStateException(message, error);
        }
    }

    /**
     * Finds the {@link PluginFactory}-annotated static method of the given class.
     * @throws IllegalStateException if no such method could be found
     */
    public static Method findPluginFactoryMethod(final Class<?> pluginClass) {
        Objects.requireNonNull(pluginClass, "pluginClass");
        for (final Method method : pluginClass.getDeclaredMethods()) {
            final boolean methodAnnotated = method.isAnnotationPresent(PluginFactory.class);
            if (methodAnnotated) {
                final boolean methodStatic = Modifier.isStatic(method.getModifiers());
                if (methodStatic) {
                    return method;
                }
            }
        }
        throw new IllegalStateException("no factory method found for class " + pluginClass);
    }
}
