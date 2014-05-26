/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.visitors.PluginVisitor;
import org.apache.logging.log4j.core.config.plugins.visitors.PluginVisitors;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.core.util.Builder;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Builder class to instantiate and configure a Plugin object using a PluginFactory method.
 *
 * @param <T> type of Plugin class.
 */
public class PluginBuilder<T> implements Builder<T> {

    // TODO: field injection for builder factories annotated with @PluginBuilderFactory

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final PluginType<T> pluginType;
    private final Class<T> clazz;

    private Configuration configuration;
    private Node node;
    private LogEvent event;

    private Method factory;
    private Annotation[][] annotations;
    private Class<?>[] types;

    /**
     * Constructs a PluginBuilder for a given PluginType.
     *
     * @param pluginType type of plugin to configure
     */
    public PluginBuilder(final PluginType<T> pluginType) {
        this.pluginType = pluginType;
        this.clazz = pluginType.getPluginClass();
    }

    /**
     * Specifies which annotation denotes a plugin factory method. The method must be static.
     *
     * @param annotationType class of annotation marking the plugin factory.
     * @param <A>            type of annotation.
     * @return {@code this}
     * @throws NoSuchMethodException
     */
    public <A extends Annotation> PluginBuilder<T> withFactoryMethodAnnotatedBy(final Class<A> annotationType)
            throws NoSuchMethodException {
        for (final Method method : clazz.getMethods()) {
            if (method.isAnnotationPresent(annotationType) && Modifier.isStatic(method.getModifiers())) {
                factory = method;
                LOGGER.trace("Using factory method {} on class {}", method.getName(), clazz.getName());
                return this;
            }
        }
        throw new NoSuchMethodException("No method annotated with " + annotationType.getName() + "was found in " + clazz.getName());
    }

    /**
     * Specifies the Configuration to use for constructing the plugin instance.
     *
     * @param configuration the configuration to use.
     * @return {@code this}
     */
    public PluginBuilder<T> withConfiguration(final Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Specifies the Node corresponding to the plugin object that will be created.
     *
     * @param node the plugin configuration node to use.
     * @return {@code this}
     */
    public PluginBuilder<T> withConfigurationNode(final Node node) {
        this.node = node;
        return this;
    }

    /**
     * Specifies the LogEvent that may be used to provide extra context for string substitutions.
     *
     * @param event the event to use for extra information.
     * @return {@code this}
     */
    public PluginBuilder<T> forLogEvent(final LogEvent event) {
        this.event = event;
        return this;
    }

    /**
     * Builds the plugin object.
     *
     * @return the plugin object or {@code null} if there was a problem creating it.
     */
    @Override
    public T build() {
        init();
        try {
            @SuppressWarnings("unchecked")
            final T plugin = (T) factory.invoke(null, generateParameters());
            return plugin;
        } catch (final Exception e) {
            LOGGER.error("Unable to invoke method {} in class {} for element {}",
                    factory.getName(), clazz.getName(), node.getName(), e);
            return null;
        }
    }

    private void init() {
        Assert.requireNonNull(factory, "No factory method was found.");
        Assert.requireNonNull(configuration, "No Configuration object was set.");
        Assert.requireNonNull(node, "No Node object was set.");
        annotations = factory.getParameterAnnotations();
        types = factory.getParameterTypes();
    }

    private Object[] generateParameters() {
        final Object[] args = new Object[annotations.length];
        for (int i = 0; i < annotations.length; i++) {
            final String[] aliases = extractPluginAliases(annotations[i]);
            LOGGER.debug("Constructing plugin of type {}", clazz);
            for (Annotation a : annotations[i]) {
                if (a instanceof PluginAliases) {
                    continue; // already processed
                }
                final PluginVisitor<? extends Annotation> helper = PluginVisitors.findVisitor(a.annotationType());
                if (helper != null) {
                    args[i] = helper.setAliases(aliases)
                        .setAnnotation(a)
                        .setConversionType(types[i])
                        .setStrSubstitutor(configuration.getStrSubstitutor())
                        .visit(configuration, node, event);
                }
            }
        }
        checkForRemainingAttributes();
        verifyNodeChildrenUsed();
        return args;
    }

    private static String[] extractPluginAliases(final Annotation... parmTypes) {
        String[] aliases = null;
        for (final Annotation a : parmTypes) {
            if (a instanceof PluginAliases) {
                aliases = ((PluginAliases) a).value();
            }
        }
        return aliases;
    }

    private void checkForRemainingAttributes() {
        final Map<String, String> attrs = node.getAttributes();
        if (!attrs.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (final String key : attrs.keySet()) {
                if (sb.length() == 0) {
                    sb.append(node.getName());
                    sb.append(" contains ");
                    if (attrs.size() == 1) {
                        sb.append("an invalid element or attribute ");
                    } else {
                        sb.append("invalid attributes ");
                    }
                } else {
                    sb.append(", ");
                }
                sb.append('"');
                sb.append(key);
                sb.append('"');

            }
            LOGGER.error(sb.toString());
        }
    }

    private void verifyNodeChildrenUsed() {
        final List<Node> children = node.getChildren();
        if (!(pluginType.isDeferChildren() || children.isEmpty())) {
            for (final Node child : children) {
                final String nodeType = node.getType().getElementName();
                final String start = nodeType.equals(node.getName()) ? node.getName() : nodeType + ' ' + node.getName();
                LOGGER.error("{} has no parameter that matches element {}", start, child.getName());
            }
        }
    }
}
