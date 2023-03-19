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

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.FactoryType;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginException;
import org.apache.logging.log4j.plugins.ScopeType;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.condition.Conditional;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.convert.TypeConverterFactory;
import org.apache.logging.log4j.plugins.internal.util.BeanUtils;
import org.apache.logging.log4j.plugins.internal.util.HierarchicalMap;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginRegistry;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.plugins.util.OrderedComparator;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.validation.Constraint;
import org.apache.logging.log4j.plugins.validation.ConstraintValidationException;
import org.apache.logging.log4j.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.plugins.visit.NodeVisitor;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.ServiceRegistry;
import org.apache.logging.log4j.util.StringBuilders;

class DefaultInjector implements Injector {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Set<Class<?>> COLLECTION_INJECTION_TYPES = Set.of(
            Collection.class, Iterable.class, List.class, Map.class, Optional.class, Set.class, Stream.class);

    private final HierarchicalMap<Key<?>, Binding<?>> bindings;
    private final HierarchicalMap<Class<? extends Annotation>, Scope> scopes;
    private ReflectionAccessor accessor = object -> object.setAccessible(true);

    DefaultInjector() {
        bindings = HierarchicalMap.newRootMap();
        bindings.put(KEY, Binding.from(KEY).toInstance(this));
        scopes = HierarchicalMap.newRootMap();
        scopes.put(Singleton.class, new SingletonScope());
    }

    DefaultInjector(final DefaultInjector original) {
        bindings = original.bindings.newChildMap();
        scopes = original.scopes.newChildMap();
        accessor = original.accessor;
    }

    @Override
    public void init() {
        final List<InjectorCallback> callbacks = ServiceRegistry.getInstance()
                .getServices(InjectorCallback.class, MethodHandles.lookup(), null);
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
        return getFactory(key, Set.of(), null, DependencyChain.empty());
    }

    @Override
    public TypeConverter<?> getTypeConverter(final Type type) {
        return getInstance(TypeConverterFactory.class).getTypeConverter(type);
    }

    @Override
    public void injectMembers(final Object instance) {
        injectMembers(Key.forClass(instance.getClass()), null, instance, DependencyChain.empty(), null);
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
            registerBundleInstance(getInstance((Class<?>) bundle));
        } else {
            registerBundleInstance(bundle);
        }
    }

    @Override
    public <T> Injector registerBinding(final Key<T> key, final Supplier<? extends T> factory) {
        bindings.put(key, Binding.from(key).to(factory));
        return this;
    }

    @Override
    public <T> Injector registerBindingIfAbsent(final Key<T> key, final Supplier<? extends T> factory) {
        bindings.putIfAbsent(key, Binding.from(key).to(factory));
        return this;
    }

    @Override
    public void removeBinding(final Key<?> key) {
        bindings.remove(key);
    }

    @Override
    public boolean hasBinding(final Key<?> key) {
        return bindings.containsKey(key);
    }

    @Override
    public void setReflectionAccessor(final ReflectionAccessor accessor) {
        this.accessor = accessor;
    }

    private <T> Supplier<T> getFactory(
            final InjectionPoint<T> point, final Node node, final DependencyChain chain, final StringBuilder debugLog) {
        final AnnotatedElement element = point.getElement();
        final Key<? extends NodeVisitor> visitorKey = NodeVisitor.keyFor(element);
        final NodeVisitor visitor = visitorKey != null ? getInstance(visitorKey) : null;
        if (visitor != null) {
            if (element instanceof Field) {
                return () -> Cast.cast(visitor.visitField((Field) element, node, debugLog));
            } else {
                return () -> Cast.cast(visitor.visitParameter((Parameter) element, node, debugLog));
            }
        }
        final Key<T> key = point.getKey();
        final Collection<String> aliases = point.getAliases();
        final Key<T> suppliedType = key.getSuppliedType();
        return suppliedType != null
                ? getFactory(suppliedType, aliases, node, DependencyChain.empty())
                : getFactory(key, aliases, node, chain);
    }

    private <T> Supplier<T> getFactory(
            final Key<T> key, final Collection<String> aliases, final Node node, final DependencyChain chain) {
        final Binding<?> existing = bindings.get(key);
        if (existing != null) {
            return Cast.cast(existing);
        }
        for (final String alias : aliases) {
            final Key<T> keyAlias = key.withName(alias);
            final Binding<?> existingAlias = bindings.get(keyAlias);
            if (existingAlias != null) {
                return Cast.cast(existingAlias);
            }
        }

        final Class<T> rawType = key.getRawType();
        final Scope scope = getScopeForType(rawType);

        // @Namespace PluginNamespace injection
        if (rawType == PluginNamespace.class && !key.getNamespace().isEmpty()) {
            final Key<PluginNamespace> pluginNamespaceKey = Cast.cast(key);
            final Supplier<PluginNamespace> pluginNamespaceFactory = createPluginNamespaceFactory(pluginNamespaceKey);
            return Cast.cast(merge(pluginNamespaceKey, pluginNamespaceFactory));
        }

        // @Namespace Collection<T>/Map<String, T>/Stream<T>/etc. injection
        if (COLLECTION_INJECTION_TYPES.contains(rawType) && !key.getNamespace().isEmpty()) {
            if (Stream.class.isAssignableFrom(rawType)) {
                final Key<Stream<T>> streamKey = Cast.cast(key);
                final Supplier<Stream<T>> streamFactory =
                        () -> streamPluginInstancesFromNamespace(key.getParameterizedTypeArgument(0));
                return Cast.cast(merge(streamKey, streamFactory));
            } else if (Set.class.isAssignableFrom(rawType)) {
                final Key<Set<T>> setKey = Cast.cast(key);
                final Supplier<Set<T>> setFactory = () -> getPluginSet(key.getParameterizedTypeArgument(0));
                return Cast.cast(merge(setKey, setFactory));
            } else if (Map.class.isAssignableFrom(rawType)) {
                final Key<Map<String, T>> mapKey = Cast.cast(key);
                final Supplier<Map<String, T>> mapFactory = () -> getPluginMap(key.getParameterizedTypeArgument(1));
                return Cast.cast(merge(mapKey, mapFactory));
            } else if (Iterable.class.isAssignableFrom(rawType)) {
                final Key<Iterable<T>> iterableKey = Cast.cast(key);
                final Supplier<Iterable<T>> iterableFactory = () -> getPluginList(key.getParameterizedTypeArgument(0));
                return Cast.cast(merge(iterableKey, iterableFactory));
            } else if (Optional.class.isAssignableFrom(rawType)) {
                final Key<Optional<T>> optionalKey = Cast.cast(key);
                final Supplier<Optional<T>> optionalFactory = () -> getOptionalPlugin(key.getParameterizedTypeArgument(0));
                return Cast.cast(merge(optionalKey, optionalFactory));
            } else {
                throw new InjectException("Cannot inject plugins into " + key);
            }
        }

        // Optional<T> injection
        if (rawType == Optional.class) {
            final Key<Optional<T>> optionalKey = Cast.cast(key);
            final Supplier<Optional<T>> optionalFactory = () ->
                    getOptionalInstance(key.getParameterizedTypeArgument(0), aliases, node, chain);
            return Cast.cast(merge(optionalKey, optionalFactory));
        }

        // default namespace generic T injection
        final Supplier<T> instanceSupplier = () -> {
            final StringBuilder debugLog = new StringBuilder();
            final T instance = getInjectableInstance(key, node, chain, debugLog);
            injectMembers(key, node, instance, chain, debugLog);
            return instance;
        };
        return merge(key, scope.get(key, instanceSupplier));
    }

    private <T> Supplier<T> merge(final Key<T> key, final Supplier<T> factory) {
        final Binding<?> binding = bindings.merge(key, Binding.from(key).to(factory), (oldValue, value) ->
                oldValue.getKey().getOrder() <= value.getKey().getOrder() ? oldValue : value);
        return Cast.cast(binding);
    }

    private Supplier<PluginNamespace> createPluginNamespaceFactory(final Key<PluginNamespace> key) {
        return Lazy.lazy(() -> getInstance(PluginRegistry.class).getNamespace(key.getNamespace()))::value;
    }

    private <T> Stream<PluginType<T>> streamPluginsFromNamespace(final Key<T> itemKey) {
        if (itemKey == null) {
            return Stream.empty();
        }
        final PluginNamespace namespace = getInstance(PluginRegistry.class).getNamespace(itemKey.getNamespace());
        final Type type = itemKey.getType();
        return namespace.stream()
                .filter(pluginType -> TypeUtil.isAssignable(type, pluginType.getPluginClass()))
                .sorted(Comparator.comparing(PluginType::getPluginClass, OrderedComparator.INSTANCE))
                .map(o -> Cast.cast(o));
    }

    private <T> Stream<T> streamPluginInstancesFromNamespace(final Key<T> key) {
        if (key == null) {
            return Stream.empty();
        }
        if (key.getRawType() == Supplier.class) {
            final Key<T> itemKey = key.getParameterizedTypeArgument(0);
            final Stream<Supplier<T>> factoryStream = streamPluginsFromNamespace(itemKey)
                    .map(pluginType -> getFactory(pluginType.getPluginClass()));
            return Cast.cast(factoryStream);
        }
        return streamPluginsFromNamespace(key)
                .map(pluginType -> getInstance(pluginType.getPluginClass()));
    }

    private <T> Set<T> getPluginSet(final Key<T> key) {
        return streamPluginInstancesFromNamespace(key).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private <T> Map<String, T> getPluginMap(final Key<T> key) {
        if (key.getRawType() == Supplier.class) {
            final Key<T> itemKey = key.getParameterizedTypeArgument(0);
            final Map<String, Supplier<T>> map = streamPluginsFromNamespace(itemKey).collect(Collectors.toMap(
                    PluginType::getKey,
                    pluginType -> getFactory(pluginType.getPluginClass()),
                    (lhs, rhs) -> lhs,
                    LinkedHashMap::new));
            return Cast.cast(map);
        }
        return streamPluginsFromNamespace(key).collect(Collectors.toMap(
                PluginType::getKey,
                pluginType -> getInstance(pluginType.getPluginClass()),
                (lhs, rhs) -> lhs,
                LinkedHashMap::new));
    }

    private <T> List<T> getPluginList(final Key<T> key) {
        return streamPluginInstancesFromNamespace(key).collect(Collectors.toList());
    }

    private <T> Optional<T> getOptionalPlugin(final Key<T> key) {
        return streamPluginInstancesFromNamespace(key).findFirst();
    }

    private <T> Optional<T> getOptionalInstance(
            final Key<T> key, final Collection<String> aliases, final Node node, final DependencyChain chain) {
        try {
            return Optional.ofNullable(getFactory(key, aliases, node, chain).get());
        } catch (final PluginException e) {
            return Optional.empty();
        }
    }

    private <T> T getInjectableInstance(
            final Key<T> key, final Node node, final DependencyChain chain, final StringBuilder debugLog) {
        final Class<T> rawType = key.getRawType();
        validate(rawType, key.getName(), rawType);
        final Constructor<T> constructor = BeanUtils.getInjectableConstructor(key, chain);
        final List<InjectionPoint<?>> points = InjectionPoint.fromExecutable(constructor);
        final var args = getArguments(key, node, points, chain, debugLog);
        return accessor.newInstance(constructor, args);
    }

    private void validate(final AnnotatedElement element, final String name, final Object value) {
        int errors = 0;
        for (final Annotation annotation : element.getAnnotations()) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            final Constraint constraint = annotationType.getAnnotation(Constraint.class);
            if (constraint != null && isCompatibleValidator(constraint, annotationType)) {
                final ConstraintValidator<? extends Annotation> validator = getInstance(constraint.value());
                initializeConstraintValidator(validator, annotation);
                if (!validator.isValid(name, value)) {
                    errors++;
                }
            }
        }
        if (errors > 0) {
            throw new ConstraintValidationException(element, name, value);
        }
    }

    private void injectMembers(
            final Key<?> key, final Node node, final Object instance, final DependencyChain chain, final StringBuilder debugLog) {
        injectFields(key.getRawType(), node, instance, debugLog);
        injectMethods(key, node, instance, chain, debugLog);
    }

    private void injectFields(final Class<?> rawType, final Node node, final Object instance, final StringBuilder debugLog) {
        for (Class<?> clazz = rawType; clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (final Field field : clazz.getDeclaredFields()) {
                if (BeanUtils.isInjectable(field)) {
                    injectField(field, node, instance, debugLog);
                }
            }
        }
    }

    private <T> void injectField(final Field field, final Node node, final Object instance, final StringBuilder debugLog) {
        final InjectionPoint<T> point = InjectionPoint.forField(field);
        final Supplier<T> factory = getFactory(point, node, DependencyChain.empty(), debugLog);
        final Key<T> key = point.getKey();
        final Object value = key.getRawType() == Supplier.class ? factory : factory.get();
        if (value != null) {
            accessor.setFieldValue(field, instance, value);
        }
        if (AnnotationUtil.isMetaAnnotationPresent(field, Constraint.class)) {
            final Object fieldValue = accessor.getFieldValue(field, instance);
            validate(field, key.getName(), fieldValue);
        }
    }

    private void injectMethods(
            final Key<?> key, final Node node, final Object instance, final DependencyChain chain, final StringBuilder debugLog) {
        final Class<?> rawType = key.getRawType();
        final List<Method> injectMethodsWithNoArgs = new ArrayList<>();
        for (Class<?> clazz = rawType; clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (final Method method : clazz.getDeclaredMethods()) {
                if (BeanUtils.isInjectable(method)) {
                    accessor.makeAccessible(method, instance);
                    if (method.getParameterCount() == 0) {
                        injectMethodsWithNoArgs.add(method);
                    } else {
                        final List<InjectionPoint<?>> injectionPoints = InjectionPoint.fromExecutable(method);
                        final var args = getArguments(key, node, injectionPoints, chain, debugLog);
                        accessor.invokeMethod(method, instance, args);
                    }
                }
            }
        }
        injectMethodsWithNoArgs.forEach(method -> accessor.invokeMethod(method, instance));
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
        try {
            validate(pluginClass, type.getElementType(), pluginClass);
            final StringBuilder debugLog = new StringBuilder();
            final Object instance = getInjectablePluginInstance(node, debugLog);
            if (instance instanceof Supplier<?>) {
                // configure plugin builder class and obtain plugin from that
                injectMembers(Key.forClass(instance.getClass()), node, instance, DependencyChain.empty(), debugLog);
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
        // TODO(ms): this should combine all logical Conditional instances into one Conditional
        final Conditional conditional = AnnotationUtil.getLogicalAnnotation(rawType, Conditional.class);
        if (conditional != null && !Stream.of(conditional.value())
                .map(this::getInstance)
                .allMatch(condition -> condition.matches(key, rawType))) {
            return null;
        }
        final Executable factory = BeanUtils.getInjectableFactory(rawType);
        final List<InjectionPoint<?>> points = InjectionPoint.fromExecutable(factory);
        if (!factory.canAccess(null)) {
            accessor.makeAccessible(factory);
        }
        final var args = getArguments(key, node, points, DependencyChain.empty(), debugLog);
        if (factory instanceof Method) {
            return accessor.invokeMethod((Method) factory, null, args);
        } else {
            return accessor.newInstance((Constructor<?>) factory, args);
        }
    }

    private void registerBundleInstance(final Object bundle) {
        final Class<?> moduleClass = bundle.getClass();
        final List<Method> providerMethods = new ArrayList<>();
        Stream.<Class<?>>iterate(moduleClass, c -> c != Object.class, Class::getSuperclass)
                .flatMap(c -> Stream.of(c.getDeclaredMethods()))
                .filter(method -> AnnotationUtil.isMetaAnnotationPresent(method, FactoryType.class))
                .forEachOrdered(method -> {
                    if (method.getDeclaringClass().equals(moduleClass) || providerMethods.stream().noneMatch(m ->
                            m.getName().equals(method.getName()) &&
                                    Arrays.equals(m.getParameterTypes(), method.getParameterTypes()))) {
                        final var bindings = createMethodBindings(bundle, method);
                        if (!bindings.isEmpty()) {
                            providerMethods.add(method);
                            bindings.forEach(binding -> merge(binding.getKey(), binding));
                        }
                    }
                });
    }

    private <T> List<Binding<T>> createMethodBindings(final Object instance, final Method method) {
        accessor.makeAccessible(method, instance);
        final Key<T> primaryKey = Key.forMethod(method);
        LOGGER.debug("Checking {} on {} for conditions", primaryKey, method);
        final Conditional conditional = AnnotationUtil.getLogicalAnnotation(method, Conditional.class);
        if (conditional != null && !Stream.of(conditional.value())
                .map(this::getInstance)
                .allMatch(condition -> condition.matches(primaryKey, method))) {
            LOGGER.debug("One or more conditionals failed on {}; skipping", method);
            return List.of();
        }
        final List<InjectionPoint<?>> points = InjectionPoint.fromExecutable(method);
        final var argumentFactories = getArgumentFactories(primaryKey, null, points, DependencyChain.of(primaryKey), null);
        final Supplier<T> unscoped = () -> {
            final var args = argumentFactories.entrySet()
                    .stream()
                    .map(e -> {
                        final Parameter parameter = e.getKey();
                        final String name = Keys.getName(parameter);
                        final Object value = e.getValue().get();
                        validate(parameter, name, value);
                        return value;
                    })
                    .toArray();
            return Cast.cast(accessor.invokeMethod(method, instance, args));
        };
        final Supplier<T> factory = getScopeForMethod(method).get(primaryKey, unscoped);
        final Collection<String> aliases = Keys.getAliases(method);
        final List<Binding<T>> bindings = new ArrayList<>(1 + aliases.size());
        bindings.add(Binding.from(primaryKey).to(factory));
        for (final String alias : aliases) {
            bindings.add(Binding.from(primaryKey.withName(alias)).to(factory));
        }
        return bindings;
    }

    private Object[] getArguments(
            final Key<?> key, final Node node, final List<InjectionPoint<?>> points, final DependencyChain chain,
            final StringBuilder debugLog) {
        return getArgumentFactories(key, node, points, chain, debugLog)
                .entrySet()
                .stream()
                .map(e -> {
                    final Parameter parameter = e.getKey();
                    final String name = Keys.getName(parameter);
                    final Object value = e.getValue().get();
                    validate(parameter, name, value);
                    return value;
                })
                .toArray();
    }

    private Map<Parameter, Supplier<?>> getArgumentFactories(
            final Key<?> key, final Node node, final List<InjectionPoint<?>> points, final DependencyChain chain,
            final StringBuilder debugLog) {
        final Map<Parameter, Supplier<?>> argFactories = new LinkedHashMap<>();
        for (final InjectionPoint<?> point : points) {
            final Key<?> parameterKey = point.getKey();
            final Parameter parameter = (Parameter) point.getElement();
            if (parameterKey.getRawType().equals(Supplier.class)) {
                argFactories.put(parameter, () -> getFactory(point, node, chain, debugLog));
            } else {
                final var newChain = chain.withDependency(key);
                if (newChain.hasDependency(parameterKey)) {
                    throw new CircularDependencyException(parameterKey, newChain);
                }
                argFactories.put(parameter, () -> getFactory(point, node, newChain, debugLog).get());
            }
        }
        return argFactories;
    }

    private Scope getScopeForMethod(final Method method) {
        final Annotation methodScope = AnnotationUtil.getMetaAnnotation(method, ScopeType.class);
        return methodScope != null ? getScope(methodScope.annotationType()) : getScopeForType(method.getReturnType());
    }

    private Scope getScopeForType(final Class<?> type) {
        final Annotation scope = AnnotationUtil.getMetaAnnotation(type, ScopeType.class);
        return scope != null ? getScope(scope.annotationType()) : DefaultScope.INSTANCE;
    }

    private static boolean isCompatibleValidator(
            final Constraint constraint, final Class<? extends Annotation> annotationType) {
        for (final Type type : constraint.value().getGenericInterfaces()) {
            if (type instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                if (parameterizedType.getRawType() == ConstraintValidator.class &&
                        parameterizedType.getActualTypeArguments()[0] == annotationType) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void initializeConstraintValidator(
            final ConstraintValidator<? extends Annotation> validator, final Annotation annotation) {
        // runtime type checking ensures this raw type usage is correct
        ((ConstraintValidator) validator).initialize(annotation);
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
                final String nodeType = node.getType().getElementType();
                final String start = nodeType.equalsIgnoreCase(node.getName()) ? node.getName() : nodeType + ' ' + node.getName();
                LOGGER.error("{} has no field or parameter that matches element {}", start, child.getName());
            }
        }
    }
}
