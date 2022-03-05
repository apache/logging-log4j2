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

package org.apache.logging.log4j.plugins.di;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.FactoryType;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginException;
import org.apache.logging.log4j.plugins.QualifierType;
import org.apache.logging.log4j.plugins.ScopeType;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.name.AnnotatedElementAliasesProvider;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.plugins.util.PluginType;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.validation.ConstraintValidators;
import org.apache.logging.log4j.plugins.validation.PluginValidator;
import org.apache.logging.log4j.plugins.visit.NodeVisitor;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LazyValue;
import org.apache.logging.log4j.util.ServiceRegistry;
import org.apache.logging.log4j.util.StringBuilders;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DefaultInjector implements Injector {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final BindingMap bindingMap;
    private final Map<Class<? extends Annotation>, Scope> scopes = new ConcurrentHashMap<>();
    private ReflectionCallerContext callerContext = ReflectionCallerContext.DEFAULT;

    DefaultInjector() {
        bindingMap = new BindingMap();
        bindingMap.put(Key.forClass(Injector.class), () -> this);
        scopes.put(Singleton.class, new SingletonScope());
    }

    DefaultInjector(final DefaultInjector parent) {
        bindingMap = new BindingMap(parent.bindingMap);
        scopes.putAll(parent.scopes);
        callerContext = parent.callerContext;
    }

    @Override
    public <T> Supplier<T> getFactory(final Class<T> clazz) {
        return getFactory(Key.forClass(clazz));
    }

    @Override
    public <T> Supplier<T> getFactory(final Key<T> key) {
        return getFactory(key, Set.of(), null, Set.of());
    }

    @Override
    public <T> T getInstance(final Class<T> clazz) {
        return getFactory(clazz).get();
    }

    @Override
    public <T> T getInstance(final Key<T> key) {
        return getFactory(key).get();
    }

    @Override
    public <T> T getInstance(final Node node) {
        configure(node);
        return node.getObject();
    }

    @Override
    public void removeBinding(final Key<?> key) {
        bindingMap.remove(key);
    }

    @Override
    public void installModule(final Object module) {
        if (module instanceof Class<?>) {
            registerModuleClass((Class<?>) module);
        } else {
            registerModuleInstance(module);
        }
    }

    @Override
    public void setCallerContext(final ReflectionCallerContext callerContext) {
        this.callerContext = callerContext;
    }

    @Override
    public void bindScope(final Class<? extends Annotation> scopeType, final Scope scope) {
        scopes.put(scopeType, scope);
    }

    @Override
    public Scope getScope(final Class<? extends Annotation> scopeType) {
        return scopes.get(scopeType);
    }

    @Override
    public void init() {
        final List<InjectorCallback> callbacks = ServiceRegistry.getInstance()
                .getServices(InjectorCallback.class, layer -> ServiceLoader.load(layer, InjectorCallback.class), null);
        callbacks.sort(Comparator.comparingInt(InjectorCallback::getPriority).reversed()
                .thenComparing(listener -> listener.getClass().getName()));
        for (final InjectorCallback callback : callbacks) {
            try {
                callback.configure(this);
            } catch (final Exception e) {
                LOGGER.error("Unable to configure injection callback {}: {}", callback, e.getMessage(), e);
            }
        }
    }

    @Override
    public Injector copy() {
        return new DefaultInjector(this);
    }

    @Override
    public <T> Injector bindFactory(final Key<T> key, final Supplier<? extends T> factory) {
        bindingMap.put(key, factory::get);
        return this;
    }

    @Override
    public <T> Injector bindIfAbsent(final Key<T> key, final Supplier<? extends T> factory) {
        bindingMap.bindIfAbsent(key, factory::get);
        return this;
    }

    @Override
    public <T> Injector bindInstance(final Key<T> key, final T instance) {
        bindingMap.put(key, () -> instance);
        return this;
    }

    private void registerModuleInstance(final Object module) {
        final Class<?> moduleClass = module.getClass();
        final List<Method> providerMethods = new ArrayList<>();
        Stream.<Class<?>>iterate(moduleClass, c -> c != Object.class, Class::getSuperclass)
                .flatMap(c -> Stream.of(c.getDeclaredMethods()))
                .filter(method -> AnnotationUtil.isMetaAnnotationPresent(method, FactoryType.class))
                .forEachOrdered(method -> {
                    if (method.getDeclaringClass().equals(moduleClass) || providerMethods.stream().noneMatch(m ->
                            m.getName().equals(method.getName()) &&
                                    Arrays.equals(m.getParameterTypes(), method.getParameterTypes()))) {
                        providerMethods.add(method);
                        createMethodBindings(module, method).forEach(binding -> {
                            final var key = binding.getKey();
                            if (!bindingMap.putIfAbsent(key, binding.getSupplier())) {
                                throw new PluginException(String.format(
                                        "Duplicate @Factory method (%s: %s) found for %s", moduleClass, method, key));
                            }
                        });
                    }
                });
    }

    private void registerModuleClass(final Class<?> moduleClass) {
        findStaticFactoryMethods(moduleClass)
                .forEach(method -> createMethodBindings(null, method)
                        .forEach(binding -> {
                            final var key = binding.getKey();
                            if (!bindingMap.putIfAbsent(key, binding.getSupplier())) {
                                throw new PluginException(String.format(
                                        "Duplicate @Factory method (%s: %s) found for %s", moduleClass, method, key));
                            }
                        }));
    }

    private <T> List<Binding<T>> createMethodBindings(final Object instance, final Method method) {
        final Key<T> primaryKey = Key.forMethod(method);
        final List<Supplier<?>> parameterFactories = getParameterFactories(primaryKey, null, method, Set.of(primaryKey), null);
        final Supplier<T> factory = getScopeForMethod(method).get(primaryKey,
                () -> {
                    final Object[] parameters = parameterFactories.stream().map(Supplier::get).toArray();
                    try {
                        return TypeUtil.cast(callerContext.invoke(method, instance, parameters));
                    } catch (final InvocationTargetException e) {
                        throw new PluginException("Unable to invoke factory method " + method, e.getTargetException());
                    }
                });
        final Collection<String> aliases = AnnotatedElementAliasesProvider.getAliases(method);
        final List<Binding<T>> bindings = new ArrayList<>(1 + aliases.size());
        bindings.add(Binding.bind(primaryKey, factory));
        for (final String alias : aliases) {
            bindings.add(Binding.bind(primaryKey.withName(alias), factory));
        }
        return bindings;
    }

    private void configure(final Node node) {
        final PluginType<?> type = node.getType();
        if (type != null && type.isDeferChildren()) {
            inject(node);
        } else {
            node.getChildren().forEach(this::configure);
            if (type == null) {
                if (node.getParent() == null) {
                    LOGGER.error("Unable to locate plugin for node {}", node.getName());
                }
            } else {
                inject(node);
            }
        }
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
        final List<Node> children = node.getChildren();
        if (!(type == null || type.isDeferChildren() || children.isEmpty())) {
            for (final Node child : children) {
                final String nodeType = node.getType().getElementName();
                final String start = nodeType.equals(node.getName()) ? node.getName() : nodeType + ' ' + node.getName();
                LOGGER.error("{} has no parameter that matches element {}", start, child.getName());
            }
        }
    }

    private void inject(final Node node) {
        final PluginType<?> type = node.getType();
        final Class<?> pluginClass = type.getPluginClass();
        final List<Node> children = node.getChildren();
        // support for plugin classes that implement Map; unused in Log4j, but possibly used by custom plugins
        if (Map.class.isAssignableFrom(pluginClass)) {
            final Map<String, Object> map = new LinkedHashMap<>(children.size());
            children.forEach(child -> map.put(child.getName(), child.getObject()));
            node.setObject(map);
            return;
        }
        // support for plugin classes that implement Collection; unused in Log4j, but possibly used by custom plugins
        if (Collection.class.isAssignableFrom(pluginClass)) {
            final List<Object> list = new ArrayList<>(children.size());
            children.forEach(child -> list.add(child.getObject()));
            node.setObject(list);
            return;
        }
        final String elementName = type.getElementName();
        if (!PluginValidator.validatePlugin(pluginClass, elementName)) {
            LOGGER.error("Could not configure plugin of type {} for element {} due to constraint violations", pluginClass,
                    elementName);
            return;
        }
        try {
            final StringBuilder debugLog = new StringBuilder();
            final Object instance = getInjectableInstance(Key.forClass(pluginClass), node, Set.of(), debugLog);
            if (instance instanceof Supplier<?>) {
                // configure plugin builder class and obtain plugin from that
                injectFields(instance.getClass(), node, instance, Set.of(), debugLog);
                injectMethods(Key.forClass(instance.getClass()), node, instance, Set.of(), debugLog);
                node.setObject(((Supplier<?>) instance).get());
            } else {
                // usually created via static plugin factory method, but otherwise assume this is the final plugin instance
                node.setObject(instance);
            }
            LOGGER.debug("Configured plugin element {}[{}]", node.getName(), debugLog);
        } catch (final Throwable e) {
            LOGGER.error("Could not configure plugin element {}: {}", node.getName(), e.toString(), e);
        }
    }

    private <T> Supplier<T> getFactory(
            final Field field, final Node node, final Set<Key<?>> chain, final StringBuilder debugLog) {
        final Key<? extends NodeVisitor> visitorKey = NodeVisitor.keyFor(field);
        final NodeVisitor visitor = visitorKey != null ? getInstance(visitorKey) : null;
        if (visitor != null) {
            return () -> TypeUtil.cast(visitor.visitField(field, node, debugLog));
        }
        final Key<T> key = Key.forField(field);
        final Collection<String> aliases = AnnotatedElementAliasesProvider.getAliases(field);
        final Key<T> suppliedType = key.getSuppliedType();
        return suppliedType != null ? getFactory(suppliedType, aliases, node, Set.of()) : getFactory(key, aliases, node, chain);
    }

    private <T> Supplier<T> getFactory(
            final Parameter parameter, final Node node, final Set<Key<?>> chain, final StringBuilder debugLog) {
        final Key<? extends NodeVisitor> visitorKey = NodeVisitor.keyFor(parameter);
        final NodeVisitor visitor = visitorKey != null ? getInstance(visitorKey) : null;
        if (visitor != null) {
            return () -> TypeUtil.cast(visitor.visitParameter(parameter, node, debugLog));
        }
        final Key<T> key = Key.forParameter(parameter);
        final Collection<String> aliases = AnnotatedElementAliasesProvider.getAliases(parameter);
        final Key<T> suppliedType = key.getSuppliedType();
        return suppliedType != null ? getFactory(suppliedType, aliases, node, Set.of()) : getFactory(key, aliases, node, chain);
    }

    private <T> Supplier<T> getFactory(
            final Key<T> key, final Collection<String> aliases, final Node node, final Set<Key<?>> chain) {
        final Binding<T> existing = bindingMap.get(key);
        if (existing != null) {
            return existing.getSupplier();
        }
        for (final String alias : aliases) {
            final Binding<T> binding = bindingMap.get(key.withName(alias));
            if (binding != null) {
                return binding.getSupplier();
            }
        }
        final Supplier<T> instanceSupplier = () -> {
            final StringBuilder debugLog = new StringBuilder();
            final T instance = TypeUtil.cast(getInjectableInstance(key, node, chain, debugLog));
            injectFields(key.getRawType(), node, instance, Set.of(), debugLog);
            injectMethods(key, node, instance, chain, debugLog);
            return instance;
        };
        final Scope scope = getScopeForType(key.getRawType());
        return bindingMap.bindIfAbsent(key, scope.get(key, instanceSupplier));
    }

    private Object getInjectableInstance(
            final Key<?> key, final Node node, final Set<Key<?>> chain, final StringBuilder debugLog) {
        final Class<?> rawType = key.getRawType();
        return findStaticFactoryMethods(rawType)
                .min(Comparator.comparingInt(Method::getParameterCount).thenComparing(Method::getReturnType, (c1, c2) -> {
                    if (c1.equals(c2)) {
                        return 0;
                    } else if (Supplier.class.isAssignableFrom(c1)) {
                        return -1;
                    } else if (Supplier.class.isAssignableFrom(c2)) {
                        return 1;
                    } else {
                        return c1.getName().compareTo(c2.getName());
                    }
                }))
                .map(method -> {
                    final Object[] parameters = getParameters(key, node, method, chain, debugLog);
                    try {
                        return callerContext.invoke(method, null, parameters);
                    } catch (final InvocationTargetException e) {
                        final Throwable cause = e.getTargetException();
                        throw new PluginException("Error while invoking " + method + ": " + cause.getMessage(), cause);
                    }
                })
                .orElseGet(() -> {
                    final List<Constructor<?>> injectConstructors = Stream.of(rawType.getDeclaredConstructors())
                            .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                            .collect(Collectors.toList());
                    if (injectConstructors.size() > 1) {
                        throw new PluginException("Multiple @Inject constructors found in " + rawType);
                    }
                    Constructor<?> constructor;
                    if (injectConstructors.size() == 1) {
                        constructor = injectConstructors.get(0);
                    } else {
                        try {
                            constructor = rawType.getDeclaredConstructor();
                        } catch (final NoSuchMethodException ignored) {
                            try {
                                constructor = rawType.getConstructor();
                            } catch (final NoSuchMethodException ignore) {
                                final List<Key<?>> keys = new ArrayList<>(chain);
                                keys.add(0, key);
                                final String prefix = chain.isEmpty() ? "" : "chain ";
                                final String keysToString =
                                        prefix + keys.stream().map(Key::toString).collect(Collectors.joining(" -> "));
                                throw new PluginException(
                                        "No @Inject constructors or no-arg constructor found for " + keysToString);
                            }
                        }
                    }
                    try {
                        return callerContext.construct(constructor, getParameters(key, node, constructor, chain, debugLog));
                    } catch (final InvocationTargetException e) {
                        final Throwable cause = e.getTargetException();
                        throw new PluginException("Error while invoking " + constructor + ": " + cause.getMessage(), cause);
                    } catch (final InstantiationException e) {
                        throw new PluginException("Unable to invoke injectable constructor " + constructor, e);
                    }
                });
    }

    private void injectFields(
            final Class<?> rawType, final Node node, final Object instance, final Set<Key<?>> chain,
            final StringBuilder debugLog) {
        for (final Field field : getInjectableFields(rawType)) {
            final Supplier<?> factory = getFactory(field, node, chain, debugLog);
            final Object value = Supplier.class == field.getType() ? factory : factory.get();
            if (value != null) {
                callerContext.set(field, instance, value);
            }
            if (ConstraintValidators.hasConstraints(field)) {
                final String name = AnnotatedElementNameProvider.getName(field);
                final Object fieldValue = callerContext.get(field, instance);
                if (!ConstraintValidators.isValid(field, name, fieldValue)) {
                    throw new PluginException("Validation failed for field " + field + " with value '" + value + "'");
                }
            }
        }
    }

    private void injectMethods(
            final Key<?> key, final Node node, final Object instance, final Set<Key<?>> chain,
            final StringBuilder debugLog) {
        final List<Method> injectableMethods = getInjectableMethods(key.getRawType());
        final List<Method> methodsWithParameters = injectableMethods.stream()
                .filter(method -> method.getParameterCount() > 0)
                .collect(Collectors.toList());
        for (final Method method : methodsWithParameters) {
            final Object[] parameters = getParameters(key, node, method, chain, debugLog);
            try {
                callerContext.invoke(method, instance, parameters);
            } catch (final InvocationTargetException e) {
                final Throwable cause = e.getTargetException();
                throw new PluginException("Error while invoking " + method + ": " + cause.getMessage(), cause);
            }
        }
        for (final Method method : injectableMethods) {
            if (method.getParameterCount() == 0) {
                try {
                    callerContext.invoke(method, instance);
                } catch (final InvocationTargetException e) {
                    final Throwable cause = e.getTargetException();
                    throw new PluginException("Error while invoking " + method + ": " + cause.getMessage(), cause);
                }
            }
        }
    }

    private Object[] getParameters(
            final Key<?> key, final Node node, final Executable executable, final Set<Key<?>> chain,
            final StringBuilder debugLog) {
        final List<Supplier<?>> parameterFactories = getParameterFactories(key, node, executable, chain, debugLog);
        final Object[] parameters = new Object[parameterFactories.size()];
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = parameterFactories.get(i).get();
        }
        return parameters;
    }

    private List<Supplier<?>> getParameterFactories(
            final Key<?> key, final Node node, final Executable executable, final Set<Key<?>> chain,
            final StringBuilder debugLog) {
        final int parameterCount = executable.getParameterCount();
        if (parameterCount == 0) {
            return List.of();
        }
        final List<Supplier<?>> factories = new ArrayList<>(parameterCount);
        final Parameter[] parameters = executable.getParameters();
        for (int i = 0; i < parameterCount; i++) {
            final Parameter parameter = parameters[i];
            final Class<?> parameterType = parameter.getType();
            if (parameterType.equals(Supplier.class)) {
                factories.add(() -> getFactory(parameter, node, chain, debugLog));
            } else {
                final var parameterKey = Key.forParameter(parameter);
                final Set<Key<?>> newChain;
                if (chain == null || chain.isEmpty()) {
                    newChain = Set.of(key);
                } else {
                    newChain = new LinkedHashSet<>(chain);
                    newChain.add(key);
                }
                if (newChain.contains(parameterKey)) {
                    final StringBuilder sb = new StringBuilder("Circular dependency encountered: ");
                    for (final Key<?> chainKey : newChain) {
                        sb.append(chainKey).append(" -> ");
                    }
                    sb.append(parameterKey);
                    throw new PluginException(sb.toString());
                }
                factories.add(() -> getFactory(parameter, node, newChain, debugLog).get());
            }
        }
        return factories;
    }

    private Scope getScopeForMethod(final Method method) {
        final Annotation methodScope = AnnotationUtil.getMetaAnnotation(method, ScopeType.class);
        return methodScope != null ? scopes.get(methodScope.annotationType()) : getScopeForType(method.getReturnType());
    }

    private Scope getScopeForType(final Class<?> type) {
        final Annotation scope = AnnotationUtil.getMetaAnnotation(type, ScopeType.class);
        return scope != null ? scopes.get(scope.annotationType()) : DefaultScope.INSTANCE;
    }

    private static Stream<Method> findStaticFactoryMethods(final Class<?> pluginClass) {
        return Arrays.stream(pluginClass.getDeclaredMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()) &&
                        AnnotationUtil.isMetaAnnotationPresent(method, FactoryType.class));
    }

    private static List<Field> getInjectableFields(final Class<?> rawType) {
        return Stream.<Class<?>>iterate(rawType, clazz -> clazz != Object.class, Class::getSuperclass)
                .flatMap(clazz -> Stream.of(clazz.getDeclaredFields()))
                .filter(DefaultInjector::isInjectable)
                .collect(Collectors.toList());
    }

    private static List<Method> getInjectableMethods(final Class<?> rawType) {
        return Stream.<Class<?>>iterate(rawType, clazz -> clazz != Object.class, Class::getSuperclass)
                .flatMap(clazz -> Stream.of(clazz.getDeclaredMethods()))
                .filter(DefaultInjector::isInjectable)
                .collect(Collectors.toList());
    }

    private static boolean isInjectable(final Field field) {
        return field.isAnnotationPresent(Inject.class) || AnnotationUtil.isMetaAnnotationPresent(field, QualifierType.class);
    }

    private static boolean isInjectable(final Method method) {
        return method.isAnnotationPresent(Inject.class) ||
                !AnnotationUtil.isMetaAnnotationPresent(method, FactoryType.class) &&
                        Arrays.stream(method.getParameters()).anyMatch(
                                parameter -> AnnotationUtil.isMetaAnnotationPresent(parameter, QualifierType.class));
    }

    private static class SingletonScope implements Scope {
        private final Map<Key<?>, Supplier<?>> singletonProviders = new ConcurrentHashMap<>();

        @Override
        public <T> Supplier<T> get(final Key<T> key, final Supplier<T> unscoped) {
            return TypeUtil.cast(singletonProviders.computeIfAbsent(key, ignored -> new LazyValue<>(unscoped)));
        }

        @Override
        public String toString() {
            return "[SingletonScope; size=" + singletonProviders.size() + "]";
        }
    }

    private enum DefaultScope implements Scope {
        INSTANCE;

        @Override
        public <T> Supplier<T> get(final Key<T> key, final Supplier<T> unscoped) {
            return unscoped;
        }

        @Override
        public String toString() {
            return "[Unscoped]";
        }
    }
}
