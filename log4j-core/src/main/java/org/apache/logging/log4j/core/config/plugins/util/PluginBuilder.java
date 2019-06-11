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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.util.Builder;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.plugins.validation.ConstraintValidators;
import org.apache.logging.log4j.plugins.visitors.PluginVisitor;
import org.apache.logging.log4j.plugins.visitors.PluginVisitors;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.apache.logging.log4j.util.StringBuilders;

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
    private Substitutor substitutor;
    private ConcurrentMap<String, Boolean> aliases = new ConcurrentHashMap<>();

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
    public PluginBuilder setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    /**
     * Specifies the Node corresponding to the plugin object that will be created.
     *
     * @param node the plugin configuration node to use.
     * @return {@code this}
     */
    public PluginBuilder setConfigurationNode(final Node node) {
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
        substitutor = new Substitutor(event);
        // first try to use a builder class if one is available
        try {
            LOGGER.debug("Building Plugin[name={}, class={}].", pluginType.getElementName(),
                    pluginType.getPluginClass().getName());
            final Builder<?> builder = createBuilder(this.clazz);
            if (builder != null) {
                injectFields(builder);
                return builder.build();
            }
        } catch (final ConfigurationException e) { // LOG4J2-1908
            LOGGER.error("Could not create plugin of type {} for element {}", this.clazz, node.getName(), e);
            return null; // no point in trying the factory method
        } catch (final Exception e) {
            LOGGER.error("Could not create plugin of type {} for element {}: {}",
                    this.clazz, node.getName(),
                    (e instanceof InvocationTargetException ? e.getCause() : e).toString(), e);
        }
        // or fall back to factory method if no builder class is available
        try {
            final Method factory = findFactoryMethod(this.clazz);
            final Object[] params = generateParameters(factory);
            return factory.invoke(null, params);
        } catch (final Exception e) {
            LOGGER.error("Unable to invoke factory method in {} for element {}: {}",
                    this.clazz, this.node.getName(),
                    (e instanceof InvocationTargetException ? e.getCause() : e).toString(), e);
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
            if (method.isAnnotationPresent(PluginBuilderFactory.class) &&
                Modifier.isStatic(method.getModifiers()) &&
                TypeUtil.isAssignable(Builder.class, method.getReturnType())) {
                ReflectionUtil.makeAccessible(method);
                return (Builder<?>) method.invoke(null);
            } else if (method.isAnnotationPresent(org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory.class) &&
                    Modifier.isStatic(method.getModifiers()) &&
                    TypeUtil.isAssignable(org.apache.logging.log4j.core.util.Builder.class, method.getReturnType())) {
                ReflectionUtil.makeAccessible(method);
                return new BuilderWrapper((org.apache.logging.log4j.core.util.Builder<?>) method.invoke(null));
            }
        }
        return null;
    }

    private void injectFields(final Builder<?> builder) throws IllegalAccessException {
        final Object target = builder instanceof BuilderWrapper ? ((BuilderWrapper) builder).getBuilder() : builder;
        final List<Field> fields = TypeUtil.getAllDeclaredFields(target.getClass());
        AccessibleObject.setAccessible(fields.toArray(new Field[] {}), true);
        final StringBuilder log = new StringBuilder();
        boolean invalid = false;
        StringBuilder reason = new StringBuilder();
        for (final Field field : fields) {
            log.append(log.length() == 0 ? simpleName(target) + "(" : ", ");
            final Annotation[] annotations = field.getDeclaredAnnotations();
            final String[] aliases = extractPluginAliases(annotations);
            for (Annotation a : annotations) {
                if (a instanceof PluginAliases ||
                        a instanceof org.apache.logging.log4j.core.config.plugins.PluginAliases) {
                    continue; // already processed
                }
                final PluginVisitor<? extends Annotation, Configuration> visitor =
                    PluginVisitors.findVisitor(a.annotationType());
                if (visitor != null) {
                    final Object value = visitor.setAliases(aliases)
                        .setAnnotation(a)
                        .setConversionType(field.getType())
                        .setMember(field)
                        .visit(configuration, node, substitutor, log);
                    // don't overwrite default values if the visitor gives us no value to inject
                    if (value != null) {
                        field.set(target, value);
                    }
                }
            }
            final Collection<ConstraintValidator<?>> validators =
                ConstraintValidators.findValidators(annotations);
            final Object value = field.get(target);
            for (final ConstraintValidator<?> validator : validators) {
                if (!validator.isValid(field.getName(), value)) {
                    invalid = true;
                    if (reason.length() > 0) {
                        reason.append(", ");
                    } else {
                        reason.append("Arguments given for element ").append(node.getName())
                            .append(" are invalid: ");
                    }
                    reason.append("field '").append(field.getName()).append("' has invalid value '")
                        .append(value).append("'");
                }
            }
        }
        log.append(log.length() == 0 ? builder.getClass().getSimpleName() + "()" : ")");
        LOGGER.debug(log.toString());
        if (invalid) {
            throw new ConfigurationException(reason.toString());
        }
        checkForRemainingAttributes();
        verifyNodeChildrenUsed();
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
            if ((method.isAnnotationPresent(PluginFactory.class) ||
                 method.isAnnotationPresent(org.apache.logging.log4j.core.config.plugins.PluginFactory.class)) &&
                Modifier.isStatic(method.getModifiers())) {
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
                if (a instanceof PluginAliases ||
                        a instanceof org.apache.logging.log4j.core.config.plugins.PluginAliases) {
                    continue; // already processed
                }
                final PluginVisitor<? extends Annotation, Configuration> visitor = PluginVisitors.findVisitor(
                    a.annotationType());

                if (visitor != null) {
                    final Object value = visitor.setAliases(aliases)
                        .setAnnotation(a)
                        .setConversionType(types[i])
                        .setMember(factory)
                        .visit(configuration, node, substitutor, log);
                    // don't overwrite existing values if the visitor gives us no value to inject
                    if (value != null) {
                        args[i] = value;
                    }
                }
            }
            final Collection<ConstraintValidator<?>> validators =
                ConstraintValidators.findValidators(annotations[i]);
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
            } else if (a instanceof org.apache.logging.log4j.core.config.plugins.PluginAliases) {
                aliases = ((org.apache.logging.log4j.core.config.plugins.PluginAliases) a).value();
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

    private class Substitutor implements Function<String, String> {
        private final LogEvent event;
        private final StrSubstitutor strSubstitutor;

        Substitutor(LogEvent event) {
            this.event = event;
            this.strSubstitutor = configuration.getStrSubstitutor();
        }

        @Override
        public String apply(String str) {
            return strSubstitutor.replace(event, str);
        }
    }

    public static class BuilderWrapper<T> implements Builder<T> {
        private final org.apache.logging.log4j.core.util.Builder<T> builder;

        BuilderWrapper(org.apache.logging.log4j.core.util.Builder<T> builder) {
            this.builder = builder;
        }

        public T build() {
            return builder.build();
        }

        org.apache.logging.log4j.core.util.Builder<T> getBuilder() {
            return builder;
        }
    }
}
