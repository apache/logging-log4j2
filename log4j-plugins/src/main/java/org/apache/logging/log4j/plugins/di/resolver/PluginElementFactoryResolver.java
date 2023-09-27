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
package org.apache.logging.log4j.plugins.di.resolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.spi.FactoryResolver;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;

/**
 * Factory resolver for {@link PluginElement}-annotated keys. This injects configurable child plugin instances.
 */
public class PluginElementFactoryResolver<T> implements FactoryResolver<T> {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Class<? extends Annotation> annotationType;

    public PluginElementFactoryResolver() {
        this(PluginElement.class);
    }

    protected PluginElementFactoryResolver(final Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public boolean supportsKey(final Key<?> key) {
        return key.getQualifierType() == annotationType;
    }

    @Override
    public Supplier<T> getFactory(final ResolvableKey<T> resolvableKey, final InstanceFactory instanceFactory) {
        final Key<?> key = resolvableKey.getKey();
        final Collection<String> aliases = resolvableKey.getAliases();
        return () -> {
            final Node node = instanceFactory.getInstance(Node.CURRENT_NODE);
            LOGGER.trace("Configuring node {} element {}", node.getName(), key);
            final String name = key.getName();
            final Class<?> rawType = key.getRawType();
            final Class<?> componentType = rawType.getComponentType();
            return componentType != null
                    ? findChildElements(node, name, aliases, componentType)
                    : findChildElement(node, name, aliases, rawType);
        };
    }

    private static <T> T findChildElements(
            final Node node, final String name, final Collection<String> aliases, final Class<?> componentType) {
        final Iterator<Node> iterator = node.getChildren().iterator();
        final List<Object> values = new ArrayList<>();
        while (iterator.hasNext()) {
            final Node child = iterator.next();
            if (nodeElementMatchesNameOrType(child, name, aliases, componentType)) {
                final Object value = child.getObject();
                if (value == null) {
                    LOGGER.error("Skipping null child object with name {} in element {}",
                            child.getName(), node.getName());
                    continue;
                }
                iterator.remove();
                if (value.getClass().isArray()) {
                    return Cast.cast(value);
                }
                values.add(value);
            }
        }
        final int size = values.size();
        final Object array = Array.newInstance(componentType, size);
        for (int i = 0; i < size; i++) {
            Array.set(array, i, values.get(i));
        }
        return Cast.cast(array);
    }

    private static <T> T findChildElement(
            final Node node, final String name, final Collection<String> aliases, final Class<?> targetType) {
        final List<Node> children = node.getChildren();
        final Iterator<Node> iterator = children.iterator();
        while (iterator.hasNext()) {
            final Node child = iterator.next();
            if (nodeElementMatchesNameOrType(child, name, aliases, targetType)) {
                iterator.remove();
                return child.getObject();
            }
        }
        return null;
    }

    private static boolean nodeElementMatchesNameOrType(
            final Node node, final String name, final Collection<String> aliases, final Class<?> targetType) {
        return Optional.ofNullable(node.getType())
                .filter(pluginType -> pluginTypeMatchesNameOrType(pluginType, name, aliases, targetType))
                .isPresent();
    }

    private static boolean pluginTypeMatchesNameOrType(
            final PluginType<?> pluginType, final String name, final Collection<String> aliases,
            final Class<?> targetType) {
        return elementNameMatches(pluginType, name, aliases) || targetType.isAssignableFrom(pluginType.getPluginClass());
    }

    private static boolean elementNameMatches(
            final PluginType<?> pluginType, final String name, final Collection<String> aliases) {
        final String elementName = pluginType.getElementName();
        return elementName.equalsIgnoreCase(name) || aliases.stream().anyMatch(elementName::equalsIgnoreCase);
    }
}
