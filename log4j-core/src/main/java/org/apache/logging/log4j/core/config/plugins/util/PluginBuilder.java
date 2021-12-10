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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.bind.FactoryMethodBinder;
import org.apache.logging.log4j.plugins.bind.FieldConfigurationBinder;
import org.apache.logging.log4j.plugins.bind.MethodConfigurationBinder;
import org.apache.logging.log4j.plugins.inject.ConfigurationInjector;
import org.apache.logging.log4j.plugins.util.Builder;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.apache.logging.log4j.util.StringBuilders;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

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
    private final ConcurrentMap<String, Boolean> aliases = new ConcurrentHashMap<>();

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
        LOGGER.debug("Building Plugin[name={}, class={}].", pluginType.getElementName(),
                pluginType.getPluginClass().getName());
        substitutor = new Substitutor(event);
        // first try to use a builder class if one is available
        try {
            final Builder<?> builder = createBuilder(this.clazz);
            if (builder != null) {
                return injectBuilder(builder);
            }
        } catch (final InvocationTargetException e) {
            LOGGER.error("Could not create plugin builder for plugin {} and element {}", clazz, node.getName(), e.getCause());
            return null;
        } catch (final IllegalAccessException e) {
            LOGGER.error("Could not access plugin builder for plugin {} and element {}", clazz, node.getName());
            return null;
        } catch (final RuntimeException e) { // LOG4J2-1908
            LOGGER.error("Could not create plugin of type {} for element {}", clazz, node.getName(), e);
            return null; // no point in trying the factory method
        }
        // or fall back to factory method if no builder class is available
        try {
            return injectFactoryMethod(findFactoryMethod(this.clazz));
        } catch (final Throwable e) {
            LOGGER.error("Could not create plugin of type {} for element {}: {}", clazz, node.getName(),
                    e.toString(), e);
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
            if ((method.isAnnotationPresent(PluginFactory.class)) &&
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

    private Object injectBuilder(final Builder<?> builder) {
        final Object target = builder instanceof BuilderWrapper ? ((BuilderWrapper) builder).getBuilder() : builder;
        final List<Field> fields = TypeUtil.getAllDeclaredFields(target.getClass());
        AccessibleObject.setAccessible(fields.toArray(new Field[0]), true);
        final StringBuilder log = new StringBuilder();
        // TODO: collect OptionBindingExceptions into a composite error message (ConfigurationException?)
        for (final Field field : fields) {
            ConfigurationInjector.forAnnotatedElement(field).ifPresent(injector -> {
                log.append(log.length() == 0 ? simpleName(target) + "(" : ", ");
                injector.withAliases(extractPluginAliases(field.getAnnotations()))
                        .withConversionType(field.getGenericType())
                        .withConfigurationBinder(new FieldConfigurationBinder(field))
                        .withDebugLog(log)
                        .withStringSubstitutionStrategy(substitutor)
                        .withConfiguration(configuration)
                        .withNode(node)
                        .inject(target);
            });
        }
        // TODO: tests
        for (final Method method : target.getClass().getMethods()) {
            ConfigurationInjector.forAnnotatedElement(method).ifPresent(injector -> {
                if (method.getParameterCount() != 1) {
                    throw new IllegalArgumentException("Cannot inject to a plugin builder method with parameter count other than 1");
                }
                log.append(log.length() == 0 ? simpleName(target) + "(" : ", ");
                injector.withAliases(extractPluginAliases(method.getAnnotations()))
                        .withConversionType(method.getGenericParameterTypes()[0])
                        .withConfigurationBinder(new MethodConfigurationBinder(method))
                        .withDebugLog(log)
                        .withStringSubstitutionStrategy(substitutor)
                        .withConfiguration(configuration)
                        .withNode(node)
                        .inject(target);
            });
        }
        log.append(log.length() == 0 ? builder.getClass().getSimpleName() + "()" : ")");
        LOGGER.debug(log.toString());
        checkForRemainingAttributes();
        verifyNodeChildrenUsed();
        return builder.build();
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

    private Object injectFactoryMethod(final Method factory) throws Throwable {
        final StringBuilder log = new StringBuilder();
        final FactoryMethodBinder binder = new FactoryMethodBinder(factory);
        binder.forEachParameter((parameter, optionBinder) -> {
            log.append(log.length() == 0 ? factory.getName() + "(" : ", ");
            ConfigurationInjector.forAnnotatedElement(parameter).ifPresent(injector -> injector
                            .withAliases(extractPluginAliases(parameter.getAnnotations()))
                            .withConversionType(parameter.getParameterizedType())
                            .withConfigurationBinder(optionBinder)
                            .withDebugLog(log)
                            .withStringSubstitutionStrategy(substitutor)
                            .withConfiguration(configuration)
                            .withNode(node)
                            .inject(binder));
        });
        log.append(log.length() == 0 ? factory.getName() + "()" : ")");
        checkForRemainingAttributes();
        verifyNodeChildrenUsed();
        LOGGER.debug(log.toString());
        return binder.invoke();
    }

    private static String[] extractPluginAliases(final Annotation... paramTypes) {
        String[] aliases = {};
        for (final Annotation a : paramTypes) {
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

        Substitutor(final LogEvent event) {
            this.event = event;
            this.strSubstitutor = configuration.getStrSubstitutor();
        }

        @Override
        public String apply(final String str) {
            return strSubstitutor.replace(event, str);
        }
    }

    public static class BuilderWrapper<T> implements Builder<T> {
        private final org.apache.logging.log4j.core.util.Builder<T> builder;

        BuilderWrapper(final org.apache.logging.log4j.core.util.Builder<T> builder) {
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
