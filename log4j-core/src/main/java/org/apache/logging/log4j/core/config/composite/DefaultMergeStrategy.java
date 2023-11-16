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
package org.apache.logging.log4j.core.config.composite;

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;
import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.util.Integers;

/**
 * The default merge strategy for composite configurations.
 * <p>
 * The default merge strategy performs the merge according to the following rules:
 * <ol>
 * <li>Aggregates the global configuration attributes with those in later configurations replacing those in previous
 * configurations with the exception that the highest status level and the lowest monitorInterval greater than 0 will
 * be used.</li>
 * <li>Properties from all configurations are aggregated. Duplicate properties replace those in previous
 * configurations.</li>
 * <li>Filters are aggregated under a CompositeFilter if more than one Filter is defined. Since Filters are not named
 * duplicates may be present.</li>
 * <li>Scripts and ScriptFile references are aggregated. Duplicate definitions replace those in previous
 * configurations.</li>
 * <li>Appenders are aggregated. Appenders with the same name are replaced by those in later configurations, including
 * all of the Appender's subcomponents.</li>
 * <li>Loggers are all aggregated. Logger attributes are individually merged with duplicates being replaced by those
 * in later configurations. Appender references on a Logger are aggregated with duplicates being replaced by those in
 * later configurations. Filters on a Logger are aggregated under a CompositeFilter if more than one Filter is defined.
 * Since Filters are not named duplicates may be present. Filters under Appender references included or discarded
 * depending on whether their parent Appender reference is kept or discarded.</li>
 * </ol>
 */
public class DefaultMergeStrategy implements MergeStrategy {

    private static final String APPENDERS = "appenders";
    private static final String PROPERTIES = "properties";
    private static final String LOGGERS = "loggers";
    private static final String SCRIPTS = "scripts";
    private static final String FILTERS = "filters";
    private static final String STATUS = "status";
    private static final String NAME = "name";
    private static final String REF = "ref";

    /**
     * Merge the root properties.
     * @param rootNode The composite root node.
     * @param configuration The configuration to merge.
     */
    @Override
    public void mergeRootProperties(final Node rootNode, final AbstractConfiguration configuration) {
        for (final Map.Entry<String, String> attribute :
                configuration.getRootNode().getAttributes().entrySet()) {
            boolean isFound = false;
            for (final Map.Entry<String, String> targetAttribute :
                    rootNode.getAttributes().entrySet()) {
                if (targetAttribute.getKey().equalsIgnoreCase(attribute.getKey())) {
                    if (attribute.getKey().equalsIgnoreCase(STATUS)) {
                        final Level targetLevel = Level.getLevel(toRootUpperCase(targetAttribute.getValue()));
                        final Level sourceLevel = Level.getLevel(toRootUpperCase(attribute.getValue()));
                        if (targetLevel != null && sourceLevel != null) {
                            if (sourceLevel.isLessSpecificThan(targetLevel)) {
                                targetAttribute.setValue(attribute.getValue());
                            }
                        } else if (sourceLevel != null) {
                            targetAttribute.setValue(attribute.getValue());
                        }
                    } else if (attribute.getKey().equalsIgnoreCase("monitorInterval")) {
                        final int sourceInterval = Integers.parseInt(attribute.getValue());
                        final int targetInterval = Integers.parseInt(targetAttribute.getValue());
                        if (targetInterval == 0 || sourceInterval < targetInterval) {
                            targetAttribute.setValue(attribute.getValue());
                        }
                    } else if (attribute.getKey().equalsIgnoreCase("packages")) {
                        final String sourcePackages = attribute.getValue();
                        final String targetPackages = targetAttribute.getValue();
                        if (sourcePackages != null) {
                            if (targetPackages != null) {
                                targetAttribute.setValue(targetPackages + "," + sourcePackages);
                            } else {
                                targetAttribute.setValue(sourcePackages);
                            }
                        }
                    } else {
                        targetAttribute.setValue(attribute.getValue());
                    }
                    isFound = true;
                }
            }
            if (!isFound) {
                rootNode.getAttributes().put(attribute.getKey(), attribute.getValue());
            }
        }
    }

    /**
     * Merge the source Configuration into the target Configuration.
     *
     * @param target        The target node to merge into.
     * @param source        The source node.
     * @param pluginManager The PluginManager.
     */
    @Override
    public void mergConfigurations(final Node target, final Node source, final PluginManager pluginManager) {
        for (final Node sourceChildNode : source.getChildren()) {
            final boolean isFilter = isFilterNode(sourceChildNode);
            boolean isMerged = false;
            for (final Node targetChildNode : target.getChildren()) {
                if (isFilter) {
                    if (isFilterNode(targetChildNode)) {
                        updateFilterNode(target, targetChildNode, sourceChildNode, pluginManager);
                        isMerged = true;
                        break;
                    }
                    continue;
                }

                if (!targetChildNode.getName().equalsIgnoreCase(sourceChildNode.getName())) {
                    continue;
                }

                switch (toRootLowerCase(targetChildNode.getName())) {
                    case PROPERTIES:
                    case SCRIPTS:
                    case APPENDERS: {
                        for (final Node node : sourceChildNode.getChildren()) {
                            for (final Node targetNode : targetChildNode.getChildren()) {
                                if (Objects.equals(
                                        targetNode.getAttributes().get(NAME),
                                        node.getAttributes().get(NAME))) {
                                    targetChildNode.getChildren().remove(targetNode);
                                    break;
                                }
                            }
                            targetChildNode.getChildren().add(node);
                        }
                        isMerged = true;
                        break;
                    }
                    case LOGGERS: {
                        final Map<String, Node> targetLoggers = new HashMap<>();
                        for (final Node node : targetChildNode.getChildren()) {
                            targetLoggers.put(node.getName(), node);
                        }
                        for (final Node node : sourceChildNode.getChildren()) {
                            final Node targetNode = getLoggerNode(
                                    targetChildNode, node.getAttributes().get(NAME));
                            final Node loggerNode = new Node(targetChildNode, node.getName(), node.getType());
                            if (targetNode != null) {
                                targetNode.getAttributes().putAll(node.getAttributes());
                                for (final Node sourceLoggerChild : node.getChildren()) {
                                    if (isFilterNode(sourceLoggerChild)) {
                                        boolean foundFilter = false;
                                        for (final Node targetChild : targetNode.getChildren()) {
                                            if (isFilterNode(targetChild)) {
                                                updateFilterNode(
                                                        loggerNode, targetChild, sourceLoggerChild, pluginManager);
                                                foundFilter = true;
                                                break;
                                            }
                                        }
                                        if (!foundFilter) {
                                            final Node childNode = new Node(
                                                    loggerNode,
                                                    sourceLoggerChild.getName(),
                                                    sourceLoggerChild.getType());
                                            childNode.getAttributes().putAll(sourceLoggerChild.getAttributes());
                                            childNode.getChildren().addAll(sourceLoggerChild.getChildren());
                                            targetNode.getChildren().add(childNode);
                                        }
                                    } else {
                                        final Node childNode = new Node(
                                                loggerNode, sourceLoggerChild.getName(), sourceLoggerChild.getType());
                                        childNode.getAttributes().putAll(sourceLoggerChild.getAttributes());
                                        childNode.getChildren().addAll(sourceLoggerChild.getChildren());
                                        if (childNode.getName().equalsIgnoreCase("AppenderRef")) {
                                            for (final Node targetChild : targetNode.getChildren()) {
                                                if (isSameReference(targetChild, childNode)) {
                                                    targetNode.getChildren().remove(targetChild);
                                                    break;
                                                }
                                            }
                                        } else {
                                            for (final Node targetChild : targetNode.getChildren()) {
                                                if (isSameName(targetChild, childNode)) {
                                                    targetNode.getChildren().remove(targetChild);
                                                    break;
                                                }
                                            }
                                        }

                                        targetNode.getChildren().add(childNode);
                                    }
                                }
                            } else {
                                loggerNode.getAttributes().putAll(node.getAttributes());
                                loggerNode.getChildren().addAll(node.getChildren());
                                targetChildNode.getChildren().add(loggerNode);
                            }
                        }
                        isMerged = true;
                        break;
                    }
                    default: {
                        targetChildNode.getChildren().addAll(sourceChildNode.getChildren());
                        isMerged = true;
                        break;
                    }
                }
            }
            if (!isMerged) {
                if (sourceChildNode.getName().equalsIgnoreCase("Properties")) {
                    target.getChildren().add(0, sourceChildNode);
                } else {
                    target.getChildren().add(sourceChildNode);
                }
            }
        }
    }

    private Node getLoggerNode(final Node parentNode, final String name) {
        for (final Node node : parentNode.getChildren()) {
            final String nodeName = node.getAttributes().get(NAME);
            if (name == null && nodeName == null) {
                return node;
            }
            if (nodeName != null && nodeName.equals(name)) {
                return node;
            }
        }
        return null;
    }

    private void updateFilterNode(
            final Node target,
            final Node targetChildNode,
            final Node sourceChildNode,
            final PluginManager pluginManager) {
        if (CompositeFilter.class.isAssignableFrom(targetChildNode.getType().getPluginClass())) {
            final Node node = new Node(targetChildNode, sourceChildNode.getName(), sourceChildNode.getType());
            node.getChildren().addAll(sourceChildNode.getChildren());
            node.getAttributes().putAll(sourceChildNode.getAttributes());
            targetChildNode.getChildren().add(node);
        } else {
            final PluginType pluginType = pluginManager.getPluginType(FILTERS);
            final Node filtersNode = new Node(targetChildNode, FILTERS, pluginType);
            final Node node = new Node(filtersNode, sourceChildNode.getName(), sourceChildNode.getType());
            node.getAttributes().putAll(sourceChildNode.getAttributes());
            final List<Node> children = filtersNode.getChildren();
            children.add(targetChildNode);
            children.add(node);
            final List<Node> nodes = target.getChildren();
            nodes.remove(targetChildNode);
            nodes.add(filtersNode);
        }
    }

    private boolean isFilterNode(final Node node) {
        return Filter.class.isAssignableFrom(node.getType().getPluginClass());
    }

    private boolean isSameName(final Node node1, final Node node2) {
        final String value = node1.getAttributes().get(NAME);
        return value != null
                && toRootLowerCase(value)
                        .equals(toRootLowerCase(node2.getAttributes().get(NAME)));
    }

    private boolean isSameReference(final Node node1, final Node node2) {
        final String value = node1.getAttributes().get(REF);
        return value != null
                && toRootLowerCase(value)
                        .equals(toRootLowerCase(node2.getAttributes().get(REF)));
    }
}
