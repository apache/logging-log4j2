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
import org.apache.logging.log4j.plugins.validation.ConstraintValidationException;
import org.apache.logging.log4j.plugins.validation.ConstraintValidators;
import org.apache.logging.log4j.plugins.validation.PluginValidator;
import org.apache.logging.log4j.plugins.visit.NodeVisitor;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LazyValue;
import org.apache.logging.log4j.util.ServiceRegistry;
import org.apache.logging.log4j.util.StringBuilders;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
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
    private Lookup lookup = MethodHandles.lookup();

    DefaultInjector() {
        bindingMap = new BindingMap();
        bindingMap.put(Key.forClass(Injector.class), () -> this);
        scopes.put(Singleton.class, new SingletonScope());
    }

    DefaultInjector(final DefaultInjector original) {
        bindingMap = new BindingMap(original.bindingMap);
        scopes.putAll(original.scopes);
        lookup = original.lookup;
    }

    @Override
    public void init() {
        final List<InjectorCallback> callbacks = ServiceRegistry.getInstance()
                .getServices(InjectorCallback.class, layer -> ServiceLoader.load(layer, InjectorCallback.class), null);
        callbacks.sort(InjectorCallback.COMPARATOR);
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
    public <T> Supplier<T> getFactory(final Key<T> key) {
        return getFactory(key, Set.of(), null, Set.of());
    }

    @Override
    public void injectMembers(final Object instance) {
        injectMembers(Key.forClass(instance.getClass()), null, instance, Set.of(), null);
    }

    @Override
    public <T> T configure(final Node node) {
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
        verifyAttributesConsumed(node);
        verifyChildrenConsumed(node);
        return node.getObject();
    }

    @Override
    public void registerScope(final Class<? extends Annotation> scopeType, final Scope scope) {
        scopes.put(scopeType, scope);
    }

    @Override
    public Scope getScope(final Class<? extends Annotation> scopeType) {
        return scopes.get(scopeType);
    }

    @Override
    public void registerBundle(final Object bundle) {
        if (bundle instanceof Class<?>) {
            registerModuleInstance(getInstance((Class<?>) bundle));
        } else {
            registerModuleInstance(bundle);
        }
    }

    @Override
    public <T> Injector registerBinding(final Key<T> key, final Supplier<? extends T> factory) {
        bindingMap.put(key, factory::get);
        return this;
    }

    @Override
    public <T> Injector registerBindingIfAbsent(final Key<T> key, final Supplier<? extends T> factory) {
        bindingMap.bindIfAbsent(key, factory::get);
        return this;
    }

    @Override
    public void setLookup(final Lookup lookup) {
        this.lookup = lookup;
    }

    private <T> Supplier<T> getFactory(
            final InjectionPoint<T> point, final Node node, final Set<Key<?>> chain, final StringBuilder debugLog) {
        final AnnotatedElement element = point.getElement();
        final Key<? extends NodeVisitor> visitorKey = NodeVisitor.keyFor(element);
        final NodeVisitor visitor = visitorKey != null ? getInstance(visitorKey) : null;
        if (visitor != null) {
            if (element instanceof Field) {
                return () -> TypeUtil.cast(visitor.visitField((Field) element, node, debugLog));
            } else {
                return () -> TypeUtil.cast(visitor.visitParameter((Parameter) element, node, debugLog));
            }
        }
        final Key<T> key = point.getKey();
        final Collection<String> aliases = point.getAliases();
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
            injectMembers(key, node, instance, chain, debugLog);
            return instance;
        };
        final Scope scope = getScopeForType(key.getRawType());
        return bindingMap.bindIfAbsent(key, scope.get(key, instanceSupplier));
    }

    private Object getInjectableInstance(
            final Key<?> key, final Node node, final Set<Key<?>> chain, final StringBuilder debugLog) {
        final Constructor<?> constructor = getInjectableConstructor(key, chain);
        final List<InjectionPoint<?>> points = InjectionPoint.fromExecutable(constructor);
        final MethodHandle handle = getConstructorHandle(constructor, lookup);
        final var args = getArguments(key, node, points, chain, debugLog);
        return rethrow(() -> handle.invokeWithArguments(args));
    }

    private void injectMembers(
            final Key<?> key, final Node node, final Object instance, final Set<Key<?>> chain, final StringBuilder debugLog) {
        injectFields(key.getRawType(), node, instance, debugLog);
        injectMethods(key, node, instance, chain, debugLog);
    }

    private void injectFields(final Class<?> rawType, final Node node, final Object instance, final StringBuilder debugLog) {
        for (Class<?> clazz = rawType; clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (final Field field : clazz.getDeclaredFields()) {
                if (isInjectable(field)) {
                    injectField(field, node, instance, debugLog);
                }
            }
        }
    }

    private <T> void injectField(final Field field, final Node node, final Object instance, final StringBuilder debugLog) {
        final Lookup fieldLookup = node != null ? node.getType().getPluginLookup() : lookup.in(instance.getClass());
        final VarHandle handle = getFieldHandle(field, fieldLookup);
        final InjectionPoint<T> point = InjectionPoint.forField(field);
        final Supplier<T> factory = getFactory(point, node, Set.of(), debugLog);
        final Key<T> key = point.getKey();
        final Object value = key.getRawType() == Supplier.class ? factory : factory.get();
        if (value != null) {
            handle.set(instance, value);
        }
        if (ConstraintValidators.hasConstraints(field)) {
            final Object fieldValue = handle.get(instance);
            if (!ConstraintValidators.isValid(field, key.getName(), fieldValue)) {
                throw new ConstraintValidationException(field, key.getName(), fieldValue);
            }
        }
    }

    private void injectMethods(
            final Key<?> key, final Node node, final Object instance, final Set<Key<?>> chain, final StringBuilder debugLog) {
        final Class<?> rawType = key.getRawType();
        final Lookup methodLookup = node != null ? node.getType().getPluginLookup() : lookup.in(instance.getClass());
        final List<MethodHandle> injectMethodsWithNoArgs = new ArrayList<>();
        for (Class<?> clazz = rawType; clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (final Method method : clazz.getDeclaredMethods()) {
                if (isInjectable(method)) {
                    final MethodHandle handle = getMethodHandle(method, methodLookup).bindTo(instance);
                    if (method.getParameterCount() == 0) {
                        injectMethodsWithNoArgs.add(handle);
                    } else {
                        final List<InjectionPoint<?>> injectionPoints = InjectionPoint.fromExecutable(method);
                        final List<?> args = getArguments(key, node, injectionPoints, chain, debugLog);
                        rethrow(() -> handle.invokeWithArguments(args));
                    }
                }
            }
        }
        injectMethodsWithNoArgs.forEach(handle -> rethrow(handle::invoke));
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
            final Object instance = getInjectablePluginInstance(node, debugLog);
            if (instance instanceof Supplier<?>) {
                // configure plugin builder class and obtain plugin from that
                injectMembers(Key.forClass(instance.getClass()), node, instance, Set.of(), debugLog);
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

    private Object getInjectablePluginInstance(final Node node, final StringBuilder debugLog) {
        final PluginType<?> type = node.getType();
        final Class<?> rawType = type.getPluginClass();
        final Key<?> key = Key.forClass(rawType);
        final Executable factory = Stream.of(rawType.getDeclaredMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()) &&
                        AnnotationUtil.isMetaAnnotationPresent(method, FactoryType.class))
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
                .map(Executable.class::cast)
                .orElseGet(() -> getInjectableConstructor(key, Set.of()));
        final List<InjectionPoint<?>> points = InjectionPoint.fromExecutable(factory);
        final Lookup pluginLookup = type.getPluginLookup();
        final MethodHandle handle;
        if (factory instanceof Method) {
            handle = getMethodHandle((Method) factory, pluginLookup);
        } else {
            handle = getConstructorHandle((Constructor<?>) factory, pluginLookup);
        }
        final var args = getArguments(key, node, points, Set.of(), debugLog);
        return rethrow(() -> handle.invokeWithArguments(args));
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

    private <T> List<Binding<T>> createMethodBindings(final Object instance, final Method method) {
        final Key<T> primaryKey = Key.forMethod(method);
        final List<InjectionPoint<?>> points = InjectionPoint.fromExecutable(method);
        final MethodHandle handle = getMethodHandle(method, lookup);
        final MethodHandle boundHandle = Modifier.isStatic(method.getModifiers()) ? handle : handle.bindTo(instance);
        final var argumentFactories = getArgumentFactories(primaryKey, null, points, Set.of(primaryKey), null);
        final Supplier<T> unscoped = () -> {
            final List<Object> args = argumentFactories.entrySet()
                    .stream()
                    .flatMap(e -> {
                        final Object value = e.getValue().get();
                        return e.getKey().isVarArgs() ? Stream.of((Object[]) value) : Stream.of(value);
                    })
                    .collect(Collectors.toList());
            return TypeUtil.cast(rethrow(() -> boundHandle.invokeWithArguments(args)));
        };
        final Supplier<T> factory = getScopeForMethod(method).get(primaryKey, unscoped);
        final Collection<String> aliases = AnnotatedElementAliasesProvider.getAliases(method);
        final List<Binding<T>> bindings = new ArrayList<>(1 + aliases.size());
        bindings.add(Binding.bind(primaryKey, factory));
        for (final String alias : aliases) {
            bindings.add(Binding.bind(primaryKey.withName(alias), factory));
        }
        return bindings;
    }

    private List<Object> getArguments(
            final Key<?> key, final Node node, final List<InjectionPoint<?>> points, final Set<Key<?>> chain,
            final StringBuilder debugLog) {
        return getArgumentFactories(key, node, points, chain, debugLog)
                .entrySet()
                .stream()
                .flatMap(e -> {
                    final Parameter parameter = e.getKey();
                    final String name = AnnotatedElementNameProvider.getName(parameter);
                    final Object value = e.getValue().get();
                    if (!ConstraintValidators.isValid(parameter, name, value)) {
                        throw new ConstraintValidationException(parameter, name, value);
                    }
                    return parameter.isVarArgs() ? Stream.of((Object[]) value) : Stream.of(value);
                })
                .collect(Collectors.toList());
    }

    private Map<Parameter, Supplier<?>> getArgumentFactories(
            final Key<?> key, final Node node, final List<InjectionPoint<?>> points, final Set<Key<?>> chain,
            final StringBuilder debugLog) {
        final Map<Parameter, Supplier<?>> argFactories = new LinkedHashMap<>();
        for (final InjectionPoint<?> point : points) {
            final Key<?> parameterKey = point.getKey();
            final Parameter parameter = (Parameter) point.getElement();
            if (parameterKey.getRawType().equals(Supplier.class)) {
                argFactories.put(parameter, () -> getFactory(point, node, chain, debugLog));
            } else {
                final var newChain = chain(chain, key);
                if (newChain.contains(parameterKey)) {
                    final StringBuilder sb = new StringBuilder("Circular dependency encountered: ");
                    for (final Key<?> chainKey : newChain) {
                        sb.append(chainKey).append(" -> ");
                    }
                    sb.append(parameterKey);
                    throw new PluginException(sb.toString());
                }
                argFactories.put(parameter, () -> getFactory(point, node, newChain, debugLog).get());
            }
        }
        return argFactories;
    }

    private Scope getScopeForMethod(final Method method) {
        final Annotation methodScope = AnnotationUtil.getMetaAnnotation(method, ScopeType.class);
        return methodScope != null ? scopes.get(methodScope.annotationType()) : getScopeForType(method.getReturnType());
    }

    private Scope getScopeForType(final Class<?> type) {
        final Annotation scope = AnnotationUtil.getMetaAnnotation(type, ScopeType.class);
        return scope != null ? scopes.get(scope.annotationType()) : DefaultScope.INSTANCE;
    }

    private static void verifyAttributesConsumed(final Node node) {
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

    private static void verifyChildrenConsumed(final Node node) {
        final PluginType<?> type = node.getType();
        if (type != null && !type.isDeferChildren() && node.hasChildren()) {
            for (final Node child : node.getChildren()) {
                final String nodeType = node.getType().getElementName();
                final String start = nodeType.equals(node.getName()) ? node.getName() : nodeType + ' ' + node.getName();
                LOGGER.error("{} has no field or parameter that matches element {}", start, child.getName());
            }
        }
    }

    private static Set<Key<?>> chain(final Set<Key<?>> chain, final Key<?> newKey) {
        if (chain == null || chain.isEmpty()) {
            return Set.of(newKey);
        }
        final var newChain = new LinkedHashSet<>(chain);
        newChain.add(newKey);
        return newChain;
    }

    private static VarHandle getFieldHandle(final Field field, final Lookup lookup) {
        final Class<?> declaringClass = field.getDeclaringClass();
        try {
            return lookup.unreflectVarHandle(field);
        } catch (final IllegalAccessException outer) {
            try {
                return MethodHandles.privateLookupIn(declaringClass, lookup).unreflectVarHandle(field);
            } catch (final IllegalAccessException inner) {
                final var error = new IllegalAccessError("Cannot access field " + field);
                error.addSuppressed(inner);
                error.addSuppressed(outer);
                throw error;
            }
        }
    }

    private static MethodHandle getMethodHandle(final Method method, final Lookup lookup) {
        final Class<?> declaringClass = method.getDeclaringClass();
        try {
            return lookup.unreflect(method);
        } catch (final IllegalAccessException outer) {
            try {
                return MethodHandles.privateLookupIn(declaringClass, lookup).unreflect(method);
            } catch (final IllegalAccessException inner) {
                final var error = new IllegalAccessError("Cannot access method " + method);
                error.addSuppressed(inner);
                error.addSuppressed(outer);
                throw error;
            }
        }
    }

    private static MethodHandle getConstructorHandle(final Constructor<?> constructor, final Lookup lookup) {
        final Class<?> declaringClass = constructor.getDeclaringClass();
        try {
            return lookup.unreflectConstructor(constructor);
        } catch (final IllegalAccessException outer) {
            try {
                return MethodHandles.privateLookupIn(declaringClass, lookup).unreflectConstructor(constructor);
            } catch (final IllegalAccessException inner) {
                final var error = new IllegalAccessError("Cannot access constructor " + constructor);
                error.addSuppressed(inner);
                error.addSuppressed(outer);
                throw error;
            }
        }
    }

    private static <T> Constructor<T> getInjectableConstructor(final Key<T> key, final Set<Key<?>> chain) {
        final Class<T> rawType = key.getRawType();
        final String name = key.getName();
        if (!ConstraintValidators.isValid(rawType, name, rawType)) {
            throw new ConstraintValidationException(rawType, name, rawType);
        }
        final List<Constructor<?>> injectConstructors = Stream.of(rawType.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
        if (injectConstructors.size() > 1) {
            throw new PluginException("Multiple @Inject constructors found in " + rawType);
        }
        if (injectConstructors.size() == 1) {
            return TypeUtil.cast(injectConstructors.get(0));
        }
        try {
            return rawType.getDeclaredConstructor();
        } catch (final NoSuchMethodException ignored) {
        }
        try {
            return rawType.getConstructor();
        } catch (final NoSuchMethodException ignored) {
        }
        final List<Key<?>> keys = new ArrayList<>(chain);
        keys.add(0, key);
        final String prefix = chain.isEmpty() ? "" : "chain ";
        final String keysToString =
                prefix + keys.stream().map(Key::toString).collect(Collectors.joining(" -> "));
        throw new PluginException(
                "No @Inject constructors or no-arg constructor found for " + keysToString);
    }

    private static boolean isInjectable(final Field field) {
        return field.isAnnotationPresent(Inject.class) || AnnotationUtil.isMetaAnnotationPresent(field, QualifierType.class);
    }

    private static boolean isInjectable(final Method method) {
        return method.isAnnotationPresent(Inject.class) ||
                !AnnotationUtil.isMetaAnnotationPresent(method, FactoryType.class) &&
                        Stream.of(method.getParameters()).anyMatch(
                                parameter -> AnnotationUtil.isMetaAnnotationPresent(parameter, QualifierType.class));
    }

    private static <T> T rethrow(final CheckedSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (final Throwable e) {
            rethrow(e);
            throw new IllegalStateException("unreachable", e);
        }
    }

    // type inference and erasure ensures that checked exceptions can be thrown here without being checked anymore
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void rethrow(final Throwable t) throws T {
        throw (T) t;
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Throwable;
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
