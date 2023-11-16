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
package org.apache.logging.log4j.core.config.plugins.visitors;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;

/**
 * PluginVisitor implementation for {@link PluginElement}. Supports arrays as well as singular values.
 */
public class PluginElementVisitor extends AbstractPluginVisitor<PluginElement> {
    public PluginElementVisitor() {
        super(PluginElement.class);
    }

    @Override
    public Object visit(
            final Configuration configuration, final Node node, final LogEvent event, final StringBuilder log) {
        final String name = this.annotation.value();
        if (this.conversionType.isArray()) {
            setConversionType(this.conversionType.getComponentType());
            final List<Object> values = new ArrayList<>();
            final Collection<Node> used = new ArrayList<>();
            log.append("={");
            boolean first = true;
            for (final Node child : node.getChildren()) {
                final PluginType<?> childType = child.getType();
                if (name.equalsIgnoreCase(childType.getElementName())
                        || this.conversionType.isAssignableFrom(childType.getPluginClass())) {
                    if (!first) {
                        log.append(", ");
                    }
                    first = false;
                    used.add(child);
                    final Object childObject = child.getObject();
                    if (childObject == null) {
                        LOGGER.error("Null object returned for {} in {}.", child.getName(), node.getName());
                        continue;
                    }
                    if (childObject.getClass().isArray()) {
                        log.append(Arrays.toString((Object[]) childObject)).append('}');
                        node.getChildren().removeAll(used);
                        return childObject;
                    }
                    log.append(child.toString());
                    values.add(childObject);
                }
            }
            log.append('}');
            // note that we need to return an empty array instead of null if the types are correct
            if (!values.isEmpty()
                    && !this.conversionType.isAssignableFrom(values.get(0).getClass())) {
                LOGGER.error(
                        "Attempted to assign attribute {} to list of type {} which is incompatible with {}.",
                        name,
                        values.get(0).getClass(),
                        this.conversionType);
                return null;
            }
            node.getChildren().removeAll(used);
            // we need to use reflection here because values.toArray() will cause type errors at runtime
            final Object[] array = (Object[]) Array.newInstance(this.conversionType, values.size());
            for (int i = 0; i < array.length; i++) {
                array[i] = values.get(i);
            }
            return array;
        }
        final Node namedNode = findNamedNode(name, node.getChildren());
        if (namedNode == null) {
            log.append(name).append("=null");
            return null;
        }
        log.append(namedNode.getName()).append('(').append(namedNode.toString()).append(')');
        node.getChildren().remove(namedNode);
        return namedNode.getObject();
    }

    private Node findNamedNode(final String name, final Iterable<Node> children) {
        for (final Node child : children) {
            final PluginType<?> childType = child.getType();
            final boolean elementNameMatch = childType != null && name.equalsIgnoreCase(childType.getElementName());
            final boolean isAssignableByPluginClass =
                    childType != null && this.conversionType.isAssignableFrom(childType.getPluginClass());
            if (elementNameMatch || isAssignableByPluginClass) {
                return child;
            }
        }
        return null;
    }
}
