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

package org.apache.logging.log4j.plugins.spi.impl;

import org.apache.logging.log4j.plugins.di.DependentScoped;
import org.apache.logging.log4j.plugins.di.Disposes;
import org.apache.logging.log4j.plugins.di.Producer;
import org.apache.logging.log4j.plugins.di.Provider;
import org.apache.logging.log4j.plugins.di.ScopeType;
import org.apache.logging.log4j.plugins.di.SingletonScoped;
import org.apache.logging.log4j.plugins.di.model.PluginSource;
import org.apache.logging.log4j.plugins.name.AnnotatedElementAliasesProvider;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.spi.AmbiguousBeanException;
import org.apache.logging.log4j.plugins.spi.Bean;
import org.apache.logging.log4j.plugins.spi.BeanManager;
import org.apache.logging.log4j.plugins.spi.DefinitionException;
import org.apache.logging.log4j.plugins.spi.InitializationContext;
import org.apache.logging.log4j.plugins.spi.InjectionException;
import org.apache.logging.log4j.plugins.spi.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.InjectionTargetFactory;
import org.apache.logging.log4j.plugins.spi.ProducerFactory;
import org.apache.logging.log4j.plugins.spi.ResolutionException;
import org.apache.logging.log4j.plugins.spi.ScopeContext;
import org.apache.logging.log4j.plugins.spi.UnsatisfiedBeanException;
import org.apache.logging.log4j.plugins.spi.ValidationException;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.plugins.util.LazyValue;
import org.apache.logging.log4j.plugins.util.ResolverUtil;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.util.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultBeanManager implements BeanManager {

    private final Injector injector = new Injector(this);

    private final Collection<Bean<?>> enabledBeans = ConcurrentHashMap.newKeySet();
    private final Map<Type, Collection<Bean<?>>> beansByType = new ConcurrentHashMap<>();
    private final Collection<DisposesMethod> disposesMethods = new ArrayList<>();
    private final Map<Class<? extends Annotation>, ScopeContext> scopes = new LinkedHashMap<>();

    public DefaultBeanManager() {
        // TODO: need a better way to register scope contexts
        // TODO: need ThreadLocalScopeContext for LoggerContext (~ContextSelector) and ConfigurationContext scopes
        //  (can potentially modify LoggerContext[Factory] to set these thread local values on construction et al.
        scopes.put(DependentScoped.class, new DependentScopeContext());
        scopes.put(SingletonScoped.class, new DefaultScopeContext(SingletonScoped.class));
    }

    @Override
    public Collection<Bean<?>> loadBeans(final Collection<Class<?>> beanClasses) {
        if (beanClasses.isEmpty()) {
            return Set.of();
        }
        final Collection<Bean<?>> loadedBeans = new LinkedHashSet<>();
        for (final Class<?> beanClass : beanClasses) {
            final Bean<?> bean = isInjectable(beanClass) ? createBean(beanClass) : null;
            loadDisposerMethods(beanClass, bean);
            for (Class<?> clazz = beanClass; clazz != null; clazz = clazz.getSuperclass()) {
                for (final Method method : clazz.getDeclaredMethods()) {
                    if (AnnotationUtil.isMetaAnnotationPresent(method, Producer.class)) {
                        method.setAccessible(true);
                        loadedBeans.add(createBean(method, bean));
                    }
                }
            }
            for (Class<?> clazz = beanClass; clazz != null; clazz = clazz.getSuperclass()) {
                for (final Field field : clazz.getDeclaredFields()) {
                    if (AnnotationUtil.isMetaAnnotationPresent(field, Producer.class)) {
                        field.setAccessible(true);
                        loadedBeans.add(createBean(field, bean));
                    }
                }
            }
            if (bean != null) {
                loadedBeans.add(bean);
            }
        }
        return loadedBeans;
    }

    @Override
    public Collection<Bean<?>> scanAndLoadBeans(final ClassLoader classLoader, final String packageName) {
        if (Strings.isBlank(packageName)) {
            return Set.of();
        }
        final ResolverUtil resolver = new ResolverUtil();
        if (classLoader != null) {
            resolver.setClassLoader(classLoader);
        }
        resolver.findInPackage(new BeanTest(this::isInjectable), packageName);
        return loadBeans(resolver.getClasses());
    }

    @Override
    public Collection<Bean<?>> loadBeansFromPluginSources(final Collection<PluginSource> pluginSources) {
        if (pluginSources.isEmpty()) {
            return Set.of();
        }
        // TODO: enhance PluginSource metadata to support lazy loading of classes in respective beans for implemented interfaces
        final Set<Class<?>> beanClasses = pluginSources.stream()
                .map(PluginSource::getDeclaringClass)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return loadBeans(beanClasses);
    }

    @Override
    public <T> Bean<T> createBean(final Class<T> beanClass) {
        final Collection<Type> types = TypeUtil.getTypeClosure(beanClass);
        final String name = AnnotatedElementNameProvider.getName(beanClass);
        final Class<? extends Annotation> scopeType = getScopeType(beanClass);
        final InjectionTargetFactory<T> factory =
                new DefaultInjectionTargetFactory<>(this, injector, beanClass);
        return addBean(new InjectionTargetBean<>(types, name, scopeType, beanClass, factory));
    }

    @Override
    public Bean<?> createBean(final Field producerField, final Bean<?> owner) {
        final Collection<Type> types = TypeUtil.getTypeClosure(producerField.getGenericType());
        final String name = AnnotatedElementNameProvider.getName(producerField);
        final Method disposerMethod = resolveDisposerMethod(types, name, owner);
        final Collection<InjectionPoint> disposerIPs =
                disposerMethod == null ? Set.of() : createExecutableInjectionPoints(disposerMethod, owner);
        final ProducerFactory factory =
                new FieldProducerFactory(this, owner, producerField, disposerMethod, disposerIPs);
        return addBean(new ProducerBean<>(types, name, getScopeType(producerField), producerField.getDeclaringClass(), factory));
    }

    @Override
    public Bean<?> createBean(final Method producerMethod, final Bean<?> owner) {
        final Collection<Type> types = TypeUtil.getTypeClosure(producerMethod.getGenericReturnType());
        final String name = AnnotatedElementNameProvider.getName(producerMethod);
        final Method disposerMethod = resolveDisposerMethod(types, name, owner);
        final Collection<InjectionPoint> disposerIPs =
                disposerMethod == null ? Collections.emptySet() : createExecutableInjectionPoints(disposerMethod, owner);
        final Collection<InjectionPoint> producerIPs = createExecutableInjectionPoints(producerMethod, owner);
        final ProducerFactory factory =
                new MethodProducerFactory(this, owner, producerMethod, producerIPs, disposerMethod, disposerIPs);
        return addBean(new ProducerBean<>(types, name, getScopeType(producerMethod), producerMethod.getDeclaringClass(), factory));
    }

    private <T> Bean<T> addBean(final Bean<T> bean) {
        if (enabledBeans.add(bean)) {
            addBeanTypes(bean);
        }
        return bean;
    }

    private void addBeanTypes(final Bean<?> bean) {
        for (final Type type : bean.getTypes()) {
            addBeanType(bean, type);
            if (type instanceof ParameterizedType) {
                final Type rawType = ((ParameterizedType) type).getRawType();
                addBeanType(bean, rawType);
            } else if (type instanceof Class<?>) {
                final Class<?> clazz = (Class<?>) type;
                if (clazz.isPrimitive()) {
                    addBeanType(bean, TypeUtil.getReferenceType(clazz));
                }
            }
        }
    }

    private void addBeanType(final Bean<?> bean, final Type type) {
        beansByType.computeIfAbsent(type, ignored -> ConcurrentHashMap.newKeySet()).add(bean);
    }

    private void loadDisposerMethods(final Class<?> beanClass, final Bean<?> bean) {
        for (final Method method : beanClass.getMethods()) {
            for (final Parameter parameter : method.getParameters()) {
                if (parameter.isAnnotationPresent(Disposes.class)) {
                    final String name = AnnotatedElementNameProvider.getName(parameter);
                    final Collection<String> aliases = AnnotatedElementAliasesProvider.getAliases(parameter);
                    disposesMethods.add(new DisposesMethod(parameter.getParameterizedType(), name, aliases, bean, method));
                }
            }
        }
    }

    private Method resolveDisposerMethod(final Collection<Type> types, final String name, final Bean<?> disposingBean) {
        final List<Method> methods = disposesMethods.stream()
                .filter(method -> method.matches(types, name, disposingBean))
                .map(method -> method.disposesMethod)
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() == 1) {
            return methods.get(0);
        }
        throw new ResolutionException("Ambiguous @Disposes methods for matching types " + types + " and name '" + name + "': " + methods);
    }

    @Override
    public void validateBeans(final Iterable<Bean<?>> beans) {
        final List<Throwable> errors = new ArrayList<>();
        for (final Bean<?> bean : beans) {
            for (final InjectionPoint point : bean.getInjectionPoints()) {
                try {
                    validateInjectionPoint(point);
                } catch (final InjectionException e) {
                    errors.add(e);
                }
            }
        }
        if (!errors.isEmpty()) {
            throw ValidationException.fromValidationErrors(errors);
        }
    }

    @Override
    public void validateInjectionPoint(final InjectionPoint point) {
        final AnnotatedElement element = point.getElement();
        if (AnnotationUtil.isMetaAnnotationPresent(element, Producer.class)) {
            throw new DefinitionException("Cannot inject into a @Produces element: " + element);
        }
        final Type type = point.getType();
        if (type instanceof TypeVariable<?>) {
            throw new DefinitionException("Cannot inject into a TypeVariable: " + point);
        }
        final Class<?> rawType = TypeUtil.getRawType(type);
        if (rawType.equals(InjectionPoint.class)) {
            final Bean<?> bean = point.getBean()
                    .orElseThrow(() -> new DefinitionException("Cannot inject " + point + " into a non-bean"));
            if (!bean.isDependentScoped()) {
                throw new DefinitionException("Injection points can only be @DependentScoped scoped; got: " + point);
            }
        }
        if (rawType.equals(Bean.class)) {
            final Bean<?> bean = point.getBean().orElseThrow(() -> new UnsatisfiedBeanException(point));
            if (bean instanceof InjectionTargetBean<?>) {
                validateBeanInjectionPoint(point, bean.getDeclaringClass());
            } else if (bean instanceof ProducerBean<?>) {
                validateBeanInjectionPoint(point, ((ProducerBean<?>) bean).getType());
            }
        }
        final Optional<Bean<Object>> bean = getBean(point.getType(), point.getName(), point.getAliases());
        if (bean.isEmpty() && !rawType.equals(Optional.class)) {
            throw new UnsatisfiedBeanException(point);
        }
    }

    private void validateBeanInjectionPoint(final InjectionPoint point, final Type expectedType) {
        final Type type = point.getType();
        if (!(type instanceof ParameterizedType)) {
            throw new DefinitionException("Expected parameterized type for " + point + " but got " + expectedType);
        }
        final ParameterizedType parameterizedType = (ParameterizedType) type;
        final Type[] typeArguments = parameterizedType.getActualTypeArguments();
        if (typeArguments.length != 1) {
            throw new DefinitionException("Expected one type parameter argument for " + point + " but got " +
                    Arrays.toString(typeArguments));
        }
        final Type typeArgument = typeArguments[0];
        if (!typeArgument.equals(expectedType)) {
            throw new DefinitionException("Expected type " + expectedType + " but got " + typeArgument + " in " + point);
        }
    }

    @Override
    public <T> Optional<Bean<T>> getBean(final Type type, final String name, final Collection<String> aliases) {
        // TODO: this will need to allow for TypeConverter usage somehow
        // first, look for an existing bean
        final Optional<Bean<T>> existingBean = getExistingOrProvidedBean(type, name, aliases);
        if (existingBean.isPresent()) {
            return existingBean;
        }
        if (type instanceof ParameterizedType) {
            final Class<?> rawType = TypeUtil.getRawType(type);
            if (rawType == Provider.class) {
                final Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
                // if a Provider<T> is requested, we can convert an existing Bean<T> into a Bean<Provider<T>>
                final Optional<Bean<T>> actualExistingBean = getExistingBean(actualType, name, aliases);
                return actualExistingBean.map(bean -> new ProviderBean<>(type, bean, context -> () -> getValue(bean, context)))
                        .map(this::addBean)
                        .map(TypeUtil::cast);
            } else if (rawType == Optional.class) {
                final Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
                // fake T like in Provider above
                final Bean<Optional<T>> optionalBean = addBean(new OptionalBean<>(type, name,
                        LazyValue.forSupplier(() -> getExistingOrProvidedBean(actualType, name, aliases)),
                        this::getValue));
                return Optional.of(optionalBean).map(TypeUtil::cast);
            }
        }
        return Optional.empty();
    }

    private <T> Optional<Bean<T>> getExistingOrProvidedBean(final Type type, final String name, final Collection<String> aliases) {
        final Optional<Bean<T>> existingBean = getExistingBean(type, name, aliases);
        if (existingBean.isPresent()) {
            return existingBean;
        }
        return getProvidedBean(type, name, aliases);
    }

    private <T> Optional<Bean<T>> getExistingBean(final Type type, final String name, final Collection<String> aliases) {
        final Set<Bean<T>> beans = this.<T>beansWithType(type)
                .filter(bean -> name.equalsIgnoreCase(bean.getName()) || aliases.stream().anyMatch(bean.getName()::equalsIgnoreCase))
                .collect(Collectors.toSet());
        if (beans.size() > 1) {
            throw new AmbiguousBeanException(beans, "type " + type + ", name '" + name + "', and aliases " + aliases);
        }
        return beans.isEmpty() ? Optional.empty() : Optional.of(beans.iterator().next());
    }

    private <T> Optional<Bean<T>> getProvidedBean(final Type providedType, final String name, final Collection<String> aliases) {
        // TODO: need a way to get @Plugin(name) for builder class (potential alias?)
        final Optional<Bean<Provider<T>>> existingBean =
                getExistingBean(TypeUtil.getParameterizedType(Provider.class, providedType), name, aliases);
        final Optional<Bean<T>> providedBean = existingBean.map(bean ->
                new ProvidedBean<>(providedType, bean, context -> getValue(bean, context)));
        return providedBean.map(this::addBean);
    }

    private <T> Stream<Bean<T>> beansWithType(final Type requiredType) {
        if (beansByType.containsKey(requiredType)) {
            return beansByType.get(requiredType).stream().map(TypeUtil::cast);
        }
        if (requiredType instanceof ParameterizedType) {
            return beansByType.getOrDefault(((ParameterizedType) requiredType).getRawType(), Collections.emptySet())
                    .stream()
                    .filter(bean -> bean.hasMatchingType(requiredType))
                    .map(TypeUtil::cast);
        }
        return Stream.empty();
    }

    @Override
    public InjectionPoint createFieldInjectionPoint(final Field field, final Bean<?> owner) {
        Objects.requireNonNull(field);
        return DefaultInjectionPoint.forField(field, owner);
    }

    @Override
    public InjectionPoint createParameterInjectionPoint(final Executable executable, final Parameter parameter, final Bean<?> owner) {
        Objects.requireNonNull(executable);
        Objects.requireNonNull(parameter);
        return DefaultInjectionPoint.forParameter(executable, parameter, owner);
    }

    private Class<? extends Annotation> getScopeType(final AnnotatedElement element) {
        for (final Annotation annotation : element.getAnnotations()) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.isAnnotationPresent(ScopeType.class)) {
                return annotationType;
            }
        }
        if (element instanceof Field) {
            return getScopeType(((Field) element).getType());
        }
        if (element instanceof Method) {
            return getScopeType(((Method) element).getReturnType());
        }
        return DependentScoped.class;
    }

    @Override
    public <T> InitializationContext<T> createInitializationContext(final Bean<T> bean) {
        return new DefaultInitializationContext<>(bean);
    }

    @Override
    public <T> T getValue(final Bean<T> bean, final InitializationContext<?> parentContext) {
        Objects.requireNonNull(bean);
        Objects.requireNonNull(parentContext);
        final ScopeContext context = getScopeContext(bean.getScopeType());
        return context.getOrCreate(bean, parentContext.createDependentContext(bean));
    }

    private ScopeContext getScopeContext(final Class<? extends Annotation> scopeType) {
        final ScopeContext scopeContext = scopes.get(scopeType);
        if (scopeContext == null) {
            throw new ResolutionException("No active scope context found for scope @" + scopeType.getName());
        }
        return scopeContext;
    }

    @Override
    public <T> Optional<T> getInjectableValue(final InjectionPoint point, final InitializationContext<?> parentContext) {
        final Bean<T> resolvedBean = this.<T>getBean(point.getType(), point.getName(), point.getAliases())
                .orElseThrow(() -> new UnsatisfiedBeanException(point));
        final Optional<T> existingValue = point.getBean()
                .filter(bean -> !bean.equals(resolvedBean))
                .flatMap(bean -> getExistingValue(resolvedBean, bean, parentContext));
        if (existingValue.isPresent()) {
            return existingValue;
        }
        final InitializationContext<?> context =
                resolvedBean.isDependentScoped() ? parentContext : createInitializationContext(resolvedBean);
        return Optional.of(getValue(resolvedBean, context));
    }

    private <T> Optional<T> getExistingValue(final Bean<T> resolvedBean, final Bean<?> pointBean,
                                             final InitializationContext<?> parentContext) {
        if (!pointBean.isDependentScoped() || parentContext.getNonDependentScopedDependent().isPresent()) {
            final Optional<T> incompleteInstance = parentContext.getIncompleteInstance(resolvedBean);
            if (incompleteInstance.isPresent()) {
                return incompleteInstance;
            }
            return getScopeContext(resolvedBean.getScopeType()).getIfExists(resolvedBean);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        beansByType.clear();
        enabledBeans.clear();
        disposesMethods.clear();
        final List<Class<? extends Annotation>> scopeTypes = new ArrayList<>(scopes.keySet());
        Collections.reverse(scopeTypes);
        for (final Class<? extends Annotation> scopeType : scopeTypes) {
            scopes.get(scopeType).close();
        }
    }

    private static class BeanTest implements ResolverUtil.Test {
        private final Predicate<Class<?>> isBeanClass;

        private BeanTest(final Predicate<Class<?>> isBeanClass) {
            this.isBeanClass = isBeanClass;
        }

        @Override
        public boolean matches(final Class<?> type) {
            if (isBeanClass.test(type)) {
                return true;
            }
            for (final Annotation annotation : type.getAnnotations()) {
                final String name = annotation.annotationType().getName();
                if (name.startsWith("org.apache.logging.log4j.") && name.endsWith(".plugins.Plugin")) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean matches(final URI resource) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean doesMatchClass() {
            return true;
        }

        @Override
        public boolean doesMatchResource() {
            return false;
        }
    }

    private static class DisposesMethod {
        private final Type type;
        private final String name;
        private final Collection<String> aliases;
        private final Bean<?> declaringBean;
        private final Method disposesMethod;

        private DisposesMethod(final Type type, final String name, final Collection<String> aliases,
                               final Bean<?> declaringBean, final Method disposesMethod) {
            this.type = type;
            this.name = name;
            this.aliases = aliases;
            this.declaringBean = declaringBean;
            this.disposesMethod = disposesMethod;
        }

        boolean matches(final Collection<Type> types, final String name, final Bean<?> declaringBean) {
            return Objects.equals(declaringBean, this.declaringBean) && matchesName(name) && matchesType(types);
        }

        private boolean matchesType(final Collection<Type> types) {
            for (final Type t : types) {
                if (TypeUtil.typesMatch(type, t)) {
                    return true;
                }
            }
            return false;
        }

        private boolean matchesName(final String name) {
            return this.name.equalsIgnoreCase(name) || aliases.stream().anyMatch(name::equalsIgnoreCase);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            final DisposesMethod that = (DisposesMethod) o;
            return type.equals(that.type) && name.equals(that.name) && aliases.equals(that.aliases) && Objects.equals(
                    declaringBean, that.declaringBean) && disposesMethod.equals(that.disposesMethod);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, name, aliases, declaringBean, disposesMethod);
        }
    }

}
