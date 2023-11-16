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

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.apache.logging.log4j.core.util.Builder;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.core.util.TypeUtil;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * Builder class to instantiate and configure a Plugin object using a PluginFactory method or PluginBuilderFactory
 * builder class.
 */
public class PluginBuilder implements Builder<Object> {

    private static final Field[] EMPTY_FIELD_ARRAY = new Field[] {};

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
            LOGGER.debug(
                    "Building Plugin[name={}, class={}].",
                    pluginType.getElementName(),
                    pluginType.getPluginClass().getName());
            final Builder<?> builder = createBuilder(this.clazz);
            if (builder != null) {
                injectFields(builder);
                return builder.build();
            }
        } catch (final ConfigurationException e) { // LOG4J2-1908
            LOGGER.error("Could not create plugin of type {} for element {}", this.clazz, node.getName(), e);
            return null; // no point in trying the factory method
        } catch (final Throwable t) {
            LOGGER.error(
                    "Could not create plugin of type {} for element {}: {}",
                    this.clazz,
                    node.getName(),
                    (t instanceof InvocationTargetException ? ((InvocationTargetException) t).getCause() : t)
                            .toString(),
                    t);
        }
        // or fall back to factory method if no builder class is available
        try {
            final Method factory = findFactoryMethod(this.clazz);
            final Object[] params = generateParameters(factory);
            return factory.invoke(null, params);
        } catch (final Throwable t) {
            LOGGER.error(
                    "Unable to invoke factory method in {} for element {}: {}",
                    this.clazz,
                    this.node.getName(),
                    (t instanceof InvocationTargetException ? ((InvocationTargetException) t).getCause() : t)
                            .toString(),
                    t);
            return null;
        }
    }

    private void verify() {
        Objects.requireNonNull(this.configuration, "No Configuration object was set.");
        Objects.requireNonNull(this.node, "No Node object was set.");
    }

    private static Builder<?> createBuilder(final Class<?> clazz)
            throws InvocationTargetException, IllegalAccessException {
        for (final Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PluginBuilderFactory.class)
                    && Modifier.isStatic(method.getModifiers())
                    && TypeUtil.isAssignable(Builder.class, method.getReturnType())) {
                ReflectionUtil.makeAccessible(method);
                return (Builder<?>) method.invoke(null);
            }
        }
        return null;
    }

    private void injectFields(final Builder<?> builder) throws IllegalAccessException {
        final List<Field> fields = TypeUtil.getAllDeclaredFields(builder.getClass());
        AccessibleObject.setAccessible(fields.toArray(EMPTY_FIELD_ARRAY), true);
        final StringBuilder log = new StringBuilder();
        for (final Field field : fields) {
            log.append(log.length() == 0 ? simpleName(builder) + "(" : ", ");
            final Annotation[] annotations = field.getDeclaredAnnotations();
            final String[] aliases = extractPluginAliases(annotations);
            for (final Annotation a : annotations) {
                if (a instanceof PluginAliases) {
                    continue; // already processed
                }
                final PluginVisitor<? extends Annotation> visitor = PluginVisitors.findVisitor(a.annotationType());
                if (visitor != null) {
                    final Object value = visitor.setAliases(aliases)
                            .setAnnotation(a)
                            .setConversionType(field.getType())
                            .setStrSubstitutor(
                                    event == null
                                            ? configuration.getConfigurationStrSubstitutor()
                                            : configuration.getStrSubstitutor())
                            .setMember(field)
                            .visit(configuration, node, event, log);
                    // don't overwrite default values if the visitor gives us no value to inject
                    if (value != null) {
                        field.set(builder, value);
                    }
                }
            }
        }
        final String reason = validateFields(builder, fields);
        log.append(log.length() == 0 ? builder.getClass().getSimpleName() + "()" : ")");
        LOGGER.debug(log.toString());
        if (!reason.isEmpty()) {
            throw new ConfigurationException(
                    "Arguments given for element " + node.getName() + " are invalid: " + reason);
        }
        checkForRemainingAttributes();
        verifyNodeChildrenUsed();
    }

    private static String validateFields(final Builder<?> builder, final List<Field> fields)
            throws IllegalAccessException {
        String reason = "";
        for (final Field field : fields) {
            final Annotation[] annotations = field.getDeclaredAnnotations();
            final Collection<ConstraintValidator<?>> validators = ConstraintValidators.findValidators(annotations);
            final Object value = field.get(builder);
            for (final ConstraintValidator<?> validator : validators) {
                if (!validator.isValid(field.getName(), value)) {
                    if (!reason.isEmpty()) {
                        reason += ", ";
                    }
                    reason += "field '" + field.getName() + "' has invalid value '" + value + "'";
                }
            }
        }
        return reason;
    }

    public static boolean validateFields(final Builder<?> builder, final String errorPrefix) {
        final List<Field> fields = TypeUtil.getAllDeclaredFields(builder.getClass());
        AccessibleObject.setAccessible(fields.toArray(EMPTY_FIELD_ARRAY), true);
        try {
            final String reason = validateFields(builder, fields);
            if (!reason.isEmpty()) {
                LOGGER.error("{}: {}", errorPrefix, reason);
                return false;
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("{}: {}", errorPrefix, e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * {@code object.getClass().getSimpleName()} returns {@code Builder}, when we want {@code PatternLayout$Builder}.
     */
    private static String simpleName(final Object object) {
        if (object == null) {
            return "null";
        }
        final String cls = object.getClass().getName();
        final int index = cls.lastIndexOf('.');
        return index < 0 ? cls : cls.substring(index + 1);
    }

    private static Method findFactoryMethod(final Class<?> clazz) {
        for (final Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PluginFactory.class) && Modifier.isStatic(method.getModifiers())) {
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
            log.append(log.length() == 0 ? factory.getName() + "(" : ", ");
            final String[] aliases = extractPluginAliases(annotations[i]);
            for (final Annotation a : annotations[i]) {
                if (a instanceof PluginAliases) {
                    continue; // already processed
                }
                final PluginVisitor<? extends Annotation> visitor = PluginVisitors.findVisitor(a.annotationType());
                if (visitor != null) {
                    final Object value = visitor.setAliases(aliases)
                            .setAnnotation(a)
                            .setConversionType(types[i])
                            .setStrSubstitutor(
                                    event == null
                                            ? configuration.getConfigurationStrSubstitutor()
                                            : configuration.getStrSubstitutor())
                            .setMember(factory)
                            .visit(configuration, node, event, log);
                    // don't overwrite existing values if the visitor gives us no value to inject
                    if (value != null) {
                        args[i] = value;
                    }
                }
            }
            final Collection<ConstraintValidator<?>> validators = ConstraintValidators.findValidators(annotations[i]);
            final Object value = args[i];
            final String argName = "arg[" + i + "](" + simpleName(value) + ")";
            for (final ConstraintValidator<?> validator : validators) {
                if (!validator.isValid(argName, value)) {
                    invalid = true;
                }
            }
        }
        log.append(log.length() == 0 ? factory.getName() + "()" : ")");
        checkForRemainingAttributes();
        verifyNodeChildrenUsed();
        LOGGER.debug(log.toString());
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
                StringBuilders.appendDqValue(sb, key);
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
