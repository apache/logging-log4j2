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
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.core.config.plugins.validation.ConstraintValidators;
import org.apache.logging.log4j.core.config.plugins.visitors.PluginVisitor;
import org.apache.logging.log4j.core.config.plugins.visitors.PluginVisitors;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.core.util.Builder;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.core.util.TypeUtil;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Builder class to instantiate and configure a Plugin object using a PluginFactory method or PluginBuilderFactory
 * builder class.
 */
public class PluginBuilder implements Builder<Object> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final PluginType<?> pluginType;
    private final Class<?> clazz;

    private Configuration configuration;
    private Node node;
    private LogEvent event;

    /**
     * Constructs a PluginBuilder for a given PluginType.
     *
     * @param pluginType type of plugin to configure
     */
    public PluginBuilder(final PluginType<?> pluginType) {
        this.pluginType = pluginType;
        this.clazz = pluginType.getPluginClass();
    }

    /**
     * Specifies the Configuration to use for constructing the plugin instance.
     *
     * @param configuration the configuration to use.
     * @return {@code this}
     */
    public PluginBuilder withConfiguration(final Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Specifies the Node corresponding to the plugin object that will be created.
     *
     * @param node the plugin configuration node to use.
     * @return {@code this}
     */
    public PluginBuilder withConfigurationNode(final Node node) {
        this.node = node;
        return this;
    }

    /**
     * Specifies the LogEvent that may be used to provide extra context for string substitutions.
     *
     * @param event the event to use for extra information.
     * @return {@code this}
     */
    public PluginBuilder forLogEvent(final LogEvent event) {
        this.event = event;
        return this;
    }

    /**
     * Builds the plugin object.
     *
     * @return the plugin object or {@code null} if there was a problem creating it.
     */
    @Override
    public Object build() {
        verify();
        // first try to use a builder class if one is available
        try {
            LOGGER.debug("Building Plugin[name={}, class={}]. Searching for builder factory method...", pluginType.getElementName(),
                    pluginType.getPluginClass().getName());
            final Builder<?> builder = createBuilder(this.clazz);
            if (builder != null) {
                injectFields(builder);
                final Object result = builder.build();
                LOGGER.debug("Built Plugin[name={}] OK from builder factory method.", pluginType.getElementName());
                return result;
            }
        } catch (final Exception e) {
            LOGGER.error("Unable to inject fields into builder class for plugin type {}, element {}.", this.clazz,
                node.getName(), e);
        }
        // or fall back to factory method if no builder class is available
        try {
            LOGGER.debug("Still building Plugin[name={}, class={}]. Searching for factory method...",
                    pluginType.getElementName(), pluginType.getPluginClass().getName());
            final Method factory = findFactoryMethod(this.clazz);
            final Object[] params = generateParameters(factory);
            final Object plugin = factory.invoke(null, params);
            LOGGER.debug("Built Plugin[name={}] OK from factory method.", pluginType.getElementName());
            return plugin;
        } catch (final Exception e) {
            LOGGER.error("Unable to invoke factory method in class {} for element {}.", this.clazz, this.node.getName(),
                e);
            return null;
        }
    }

    private void verify() {
        Assert.requireNonNull(this.configuration, "No Configuration object was set.");
        Assert.requireNonNull(this.node, "No Node object was set.");
    }

    private static Builder<?> createBuilder(final Class<?> clazz)
        throws InvocationTargetException, IllegalAccessException {
        for (final Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PluginBuilderFactory.class) &&
                Modifier.isStatic(method.getModifiers()) &&
                TypeUtil.isAssignable(Builder.class, method.getGenericReturnType())) {
                ReflectionUtil.makeAccessible(method);
                @SuppressWarnings("unchecked")
                final Builder<?> builder = (Builder<?>) method.invoke(null);
                LOGGER.debug("Found builder factory method [{}]: {}.", method.getName(), method);
                return builder;
            }
        }
        LOGGER.debug("No builder factory method found in class {}. Going to try finding a factory method instead.",
            clazz.getName());
        return null;
    }

    private void injectFields(final Builder<?> builder) throws IllegalAccessException {
        final Field[] fields = builder.getClass().getDeclaredFields();
        AccessibleObject.setAccessible(fields, true);
        final StringBuilder log = new StringBuilder();
        boolean invalid = false;
        for (final Field field : fields) {
            log.append(log.length() == 0 ? "with params(" : ", ");
            final Annotation[] annotations = field.getDeclaredAnnotations();
            final String[] aliases = extractPluginAliases(annotations);
            for (final Annotation a : annotations) {
                if (a instanceof PluginAliases) {
                    continue; // already processed
                }
                final PluginVisitor<? extends Annotation> visitor =
                    PluginVisitors.findVisitor(a.annotationType());
                if (visitor != null) {
                    final Object value = visitor.setAliases(aliases)
                        .setAnnotation(a)
                        .setConversionType(field.getType())
                        .setStrSubstitutor(configuration.getStrSubstitutor())
                        .setMember(field)
                        .visit(configuration, node, event, log);
                    // don't overwrite default values if the visitor gives us no value to inject
                    if (value != null) {
                        field.set(builder, value);
                    }
                }
            }
            final Collection<ConstraintValidator<?>> validators =
                ConstraintValidators.findValidators(annotations);
            final Object value = field.get(builder);
            for (final ConstraintValidator<?> validator : validators) {
                if (!validator.isValid(value)) {
                    invalid = true;
                }
            }
        }
        if (log.length() > 0) {
            log.append(')');
        }
        LOGGER.debug("Calling build() on class {} for element {} {}", builder.getClass(), node.getName(),
            log.toString());
        if (invalid) {
            throw new ConfigurationException("Arguments given for element " + node.getName() + " are invalid");
        }
        checkForRemainingAttributes();
        verifyNodeChildrenUsed();
    }

    private static Method findFactoryMethod(final Class<?> clazz) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PluginFactory.class) &&
                Modifier.isStatic(method.getModifiers())) {
                LOGGER.debug("Found factory method [{}]: {}.", method.getName(), method);
                ReflectionUtil.makeAccessible(method);
                return method;
            }
        }
        throw new IllegalStateException("No factory method found for class " + clazz.getName());
    }

    private Object[] generateParameters(final Method factory) {
        final StringBuilder log = new StringBuilder();
        final Class<?>[] types = factory.getParameterTypes();
        final Annotation[][] annotations = factory.getParameterAnnotations();
        final Object[] args = new Object[annotations.length];
        boolean invalid = false;
        for (int i = 0; i < annotations.length; i++) {
            log.append(log.length() == 0 ? "with params(" : ", ");
            final String[] aliases = extractPluginAliases(annotations[i]);
            for (final Annotation a : annotations[i]) {
                if (a instanceof PluginAliases) {
                    continue; // already processed
                }
                final PluginVisitor<? extends Annotation> visitor = PluginVisitors.findVisitor(
                    a.annotationType());
                if (visitor != null) {
                    final Object value = visitor.setAliases(aliases)
                        .setAnnotation(a)
                        .setConversionType(types[i])
                        .setStrSubstitutor(configuration.getStrSubstitutor())
                        .setMember(factory)
                        .visit(configuration, node, event, log);
                    // don't overwrite existing values if the visitor gives us no value to inject
                    if (value != null) {
                        args[i] = value;
                    }
                }
            }
            final Collection<ConstraintValidator<?>> validators =
                ConstraintValidators.findValidators(annotations[i]);
            final Object value = args[i];
            for (final ConstraintValidator<?> validator : validators) {
                if (!validator.isValid(value)) {
                    invalid = true;
                }
            }
        }
        if (log.length() > 0) {
            log.append(')');
        }
        checkForRemainingAttributes();
        verifyNodeChildrenUsed();
        LOGGER.debug("Calling {} on class {} for element {} {}", factory.getName(), clazz.getName(), node.getName(),
            log.toString());
        if (invalid) {
            throw new ConfigurationException("Arguments given for element " + node.getName() + " are invalid");
        }
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
