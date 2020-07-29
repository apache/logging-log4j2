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

package org.apache.logging.log4j.plugins.inject;

import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PluginElementInjector extends AbstractConfigurationInjector<PluginElement, Object> {
    @Override
    public void inject(final Object factory) {
        final Optional<Class<?>> componentType = getComponentType(conversionType);
        if (componentType.isPresent()) {
            final Class<?> compType = componentType.get();
            final List<Object> values = new ArrayList<>();
            final Collection<Node> used = new ArrayList<>();
            debugLog.append("={");
            boolean first = true;
            for (final Node child : node.getChildren()) {
                final PluginType<?> type = child.getType();
                if (name.equalsIgnoreCase(type.getElementName()) || compType.isAssignableFrom(type.getPluginClass())) {
                    if (!first) {
                        debugLog.append(", ");
                    }
                    first = false;
                    used.add(child);
                    final Object childObject = child.getObject();
                    if (childObject == null) {
                        LOGGER.warn("Skipping null object returned for element {} in node {}", child.getName(), node.getName());
                    } else if (childObject.getClass().isArray()) {
                        Object[] children = (Object[]) childObject;
                        debugLog.append(Arrays.toString(children)).append('}');
                        node.getChildren().removeAll(used);
                        configurationBinder.bindObject(factory, children);
                        return;
                    } else {
                        debugLog.append(child.toString());
                        values.add(childObject);
                    }
                }
            }
            debugLog.append('}');
            if (!values.isEmpty() && !TypeUtil.isAssignable(compType, values.get(0).getClass())) {
                LOGGER.error("Cannot assign element {} a list of {} as it is incompatible with {}", name, values.get(0).getClass(), compType);
                return;
            }
            node.getChildren().removeAll(used);
            // using List::toArray here would cause type mismatch later on
            final Object[] vals = (Object[]) Array.newInstance(compType, values.size());
            for (int i = 0; i < vals.length; i++) {
                vals[i] = values.get(i);
            }
            configurationBinder.bindObject(factory, vals);
        } else {
            final Optional<Node> matchingChild = node.getChildren().stream().filter(this::isRequestedNode).findAny();
            if (matchingChild.isPresent()) {
                final Node child = matchingChild.get();
                debugLog.append(child.getName()).append('(').append(child.toString()).append(')');
                node.getChildren().remove(child);
                configurationBinder.bindObject(factory, child.getObject());
            } else {
                debugLog.append(name).append("=null");
                configurationBinder.bindObject(factory, null);
            }
        }
    }

    private boolean isRequestedNode(final Node child) {
        final PluginType<?> type = child.getType();
        return name.equalsIgnoreCase(type.getElementName()) || TypeUtil.isAssignable(conversionType, type.getPluginClass());
    }

    private static Optional<Class<?>> getComponentType(final Type type) {
        if (type instanceof Class<?>) {
            final Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return Optional.of(clazz.getComponentType());
            }
        }
        return Optional.empty();
    }
}
