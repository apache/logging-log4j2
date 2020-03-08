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

package org.apache.logging.log4j.plugins.defaults.bean;

import org.apache.logging.log4j.plugins.api.Disposes;
import org.apache.logging.log4j.plugins.api.Produces;
import org.apache.logging.log4j.plugins.api.Dependent;
import org.apache.logging.log4j.plugins.api.Provider;
import org.apache.logging.log4j.plugins.api.Singleton;
import org.apache.logging.log4j.plugins.defaults.model.DefaultElementManager;
import org.apache.logging.log4j.plugins.defaults.model.DefaultVariable;
import org.apache.logging.log4j.plugins.spi.AmbiguousBeanException;
import org.apache.logging.log4j.plugins.spi.InjectionException;
import org.apache.logging.log4j.plugins.spi.ResolutionException;
import org.apache.logging.log4j.plugins.spi.UnsatisfiedBeanException;
import org.apache.logging.log4j.plugins.spi.ValidationException;
import org.apache.logging.log4j.plugins.spi.bean.Bean;
import org.apache.logging.log4j.plugins.spi.bean.BeanManager;
import org.apache.logging.log4j.plugins.spi.bean.InjectionFactory;
import org.apache.logging.log4j.plugins.spi.bean.InjectionTargetFactory;
import org.apache.logging.log4j.plugins.spi.bean.ProducerFactory;
import org.apache.logging.log4j.plugins.spi.model.ElementManager;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaClass;
import org.apache.logging.log4j.plugins.spi.model.MetaElement;
import org.apache.logging.log4j.plugins.spi.model.MetaField;
import org.apache.logging.log4j.plugins.spi.model.MetaMember;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;
import org.apache.logging.log4j.plugins.spi.model.MetaParameter;
import org.apache.logging.log4j.plugins.spi.model.Qualifiers;
import org.apache.logging.log4j.plugins.spi.model.Variable;
import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.spi.bean.ScopeContext;
import org.apache.logging.log4j.plugins.spi.bean.Scoped;
import org.apache.logging.log4j.plugins.util.ParameterizedTypeImpl;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultBeanManager implements BeanManager {

    private final ElementManager elementManager;
    private final InjectionFactory injectionFactory = new DefaultInjectionFactory(this);

    private final Collection<Bean<?>> enabledBeans = ConcurrentHashMap.newKeySet();
    private final Map<Type, Collection<Bean<?>>> beansByType = new ConcurrentHashMap<>();
    private final Collection<Bean<?>> sharedBeans = ConcurrentHashMap.newKeySet();
    private final Collection<DisposesMethod<?>> disposesMethods = Collections.synchronizedCollection(new ArrayList<>());
    private final Map<Class<? extends Annotation>, ScopeContext> scopes = new ConcurrentHashMap<>();

    public DefaultBeanManager() {
        this(new DefaultElementManager());
    }

    public DefaultBeanManager(final ElementManager elementManager) {
        this.elementManager = elementManager;
        // TODO: need a better way to register scope contexts
        scopes.put(Dependent.class, new DependentScopeContext());
        scopes.put(Singleton.class, new DefaultScopeContext(Singleton.class));
    }

    @Override
    public Collection<Bean<?>> loadBeans(final Collection<Class<?>> beanClasses) {
        final Collection<Bean<?>> beans = beanClasses.stream()
                .map(elementManager::getMetaClass)
                .flatMap(metaClass -> loadBeans(metaClass).stream())
                .collect(Collectors.toSet());
        validateBeans(beans);
        return beans;
    }

    private <T> Collection<Bean<?>> loadBeans(final MetaClass<T> metaClass) {
        final Bean<T> created;
        if (elementManager.isInjectable(metaClass)) {
            final Variable<T> variable = elementManager.createVariable(metaClass);
            final InjectionTargetFactory<T> factory =
                    new DefaultInjectionTargetFactory<>(elementManager, injectionFactory, metaClass);
            created = addBean(new InjectionTargetBean<>(variable, metaClass, factory));
        } else {
            created = null;
        }
        loadDisposerMethods(metaClass, created);
        final Collection<Bean<?>> beans = loadProducerBeans(metaClass, created);
        if (created != null) {
            beans.add(created);
        }
        return beans;
    }

    private <T> Bean<T> addBean(final Bean<T> bean) {
        if (enabledBeans.add(bean)) {
            addBeanTypes(bean);
            if (!bean.isDependentScoped() && !isSystemBean(bean)) {
                sharedBeans.add(bean);
            }
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
        beansByType.computeIfAbsent(type, ignored -> new HashSet<>()).add(bean);
    }

    private boolean isSystemBean(final Bean<?> bean) {
        return bean instanceof SystemBean<?>;
    }

    private <T> void loadDisposerMethods(final MetaClass<T> metaClass, final Bean<T> bean) {
        for (final MetaMethod<T, ?> method : metaClass.getMethods()) {
            for (final MetaParameter<?> parameter : method.getParameters()) {
                if (parameter.isAnnotationPresent(Disposes.class)) {
                    disposesMethods.add(new DisposesMethod<>(
                            parameter.getBaseType(), elementManager.getQualifiers(parameter), bean, method));
                }
            }
        }
    }

    private <T> Collection<Bean<?>> loadProducerBeans(final MetaClass<T> metaClass, final Bean<T> bean) {
        final Collection<Bean<?>> beans = new HashSet<>();
        for (final MetaMethod<T, ?> method : metaClass.getMethods()) {
            if (method.isAnnotationPresent(Produces.class)) {
                beans.add(loadProducerBean(method, bean));
            }
        }
        for (final MetaField<T, ?> field : metaClass.getFields()) {
            if (field.isAnnotationPresent(Produces.class)) {
                beans.add(loadProducerBean(field, bean));
            }
        }
        return beans;
    }

    private <D, T> Bean<T> loadProducerBean(final MetaMember<D, T> member, final Bean<D> bean) {
        final Variable<T> variable = elementManager.createVariable(member);
        final MetaClass<D> declaringType = member.getDeclaringClass();
        final ProducerFactory<D> factory = getProducerFactory(member, bean);
        return addBean(new ProducerBean<>(variable, declaringType, factory));
    }

    private <D> ProducerFactory<D> getProducerFactory(final MetaMember<D, ?> member, final Bean<D> declaringBean) {
        final Variable<?> variable = elementManager.createVariable(member);
        final MetaMethod<D, ?> disposerMethod = resolveDisposerMethod(variable, declaringBean);
        final Collection<InjectionPoint<?>> disposerIPs = disposerMethod == null ? Collections.emptySet() :
                elementManager.createExecutableInjectionPoints(disposerMethod, declaringBean);
        if (member instanceof MetaField<?, ?>) {
            final MetaField<D, ?> field = TypeUtil.cast(member);
            return new FieldProducerFactory<>(this, declaringBean, field, disposerMethod, disposerIPs);
        } else {
            final MetaMethod<D, ?> method = TypeUtil.cast(member);
            final Collection<InjectionPoint<?>> producerIPs =
                    elementManager.createExecutableInjectionPoints(method, declaringBean);
            return new MethodProducerFactory<>(this, declaringBean, method, producerIPs, disposerMethod, disposerIPs);
        }
    }

    private <D, T> MetaMethod<D, ?> resolveDisposerMethod(final Variable<T> variable, final Bean<D> bean) {
        final List<MetaMethod<?, ?>> methods = disposesMethods.stream()
                .filter(method -> method.matches(variable, bean))
                .map(method -> method.disposesMethod)
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() == 1) {
            return TypeUtil.cast(methods.get(0));
        }
        throw new ResolutionException("Ambiguous @Disposes methods for " + variable + ": " + methods);
    }

    @Override
    public void validateBeans(final Iterable<Bean<?>> beans) {
        final List<Throwable> errors = new ArrayList<>();
        for (final Bean<?> bean : beans) {
            for (final InjectionPoint<?> point : bean.getInjectionPoints()) {
                try {
                    validateInjectionPoint(point);
                } catch (final InjectionException e) {
                    errors.add(e);
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private <T> void validateInjectionPoint(final InjectionPoint<T> point) {
        final MetaElement<T> element = point.getElement();
        if (element.isAnnotationPresent(Produces.class)) {
            throw new InjectionException("Cannot inject into a @Produces element: " + element);
        }
        final Type type = point.getType();
        if (type instanceof TypeVariable<?>) {
            throw new InjectionException("Cannot inject into a TypeVariable: " + point);
        }
        final Class<?> rawType = TypeUtil.getRawType(type);
        if (rawType.equals(InjectionPoint.class)) {
            final Bean<?> bean = point.getBean()
                    .orElseThrow(() -> new InjectionException("Cannot inject " + point + " into a non-bean"));
            if (!bean.isDependentScoped()) {
                throw new InjectionException("Injection points can only be @Dependent scoped; got: " + point);
            }
        }
        if (rawType.equals(Bean.class)) {
            final Bean<?> bean = point.getBean().orElseThrow(() -> new UnsatisfiedBeanException(point));
            if (bean instanceof InjectionTargetBean<?>) {
                validateBeanInjectionPoint(point, bean.getDeclaringClass().getBaseType());
            } else if (bean instanceof ProducerBean<?, ?>) {
                validateBeanInjectionPoint(point, ((ProducerBean<?, ?>) bean).getType());
            }
        }
        final Optional<Bean<T>> bean = getInjectionPointBean(point);
        if (!bean.isPresent() && !rawType.equals(Optional.class)) {
            throw new UnsatisfiedBeanException(point);
        }
    }

    private void validateBeanInjectionPoint(final InjectionPoint<?> point, final Type expectedType) {
        final Type type = point.getType();
        if (!(type instanceof ParameterizedType)) {
            throw new InjectionException("Expected parameterized type for " + point + " but got " + expectedType);
        }
        final ParameterizedType parameterizedType = (ParameterizedType) type;
        final Type[] typeArguments = parameterizedType.getActualTypeArguments();
        if (typeArguments.length != 1) {
            throw new InjectionException("Expected one type parameter argument for " + point + " but got " +
                    Arrays.toString(typeArguments));
        }
        if (point.getQualifiers().hasDefaultQualifier()) {
            final Type typeArgument = typeArguments[0];
            if (!typeArgument.equals(expectedType)) {
                throw new InjectionException("Expected type " + expectedType + " but got " + typeArgument + " in " + point);
            }
        }
    }

    private <T> Optional<Bean<T>> getInjectionPointBean(final InjectionPoint<T> point) {
        // TODO: this will need to allow for TypeConverter usage somehow
        // first, look for an existing bean
        final Type type = point.getType();
        final Qualifiers qualifiers = point.getQualifiers();
        final Optional<Bean<T>> existingBean = getExistingOrProvidedBean(type, qualifiers,
                () -> elementManager.createVariable(point));
        if (existingBean.isPresent()) {
            return existingBean;
        }
        if (type instanceof ParameterizedType) {
            final Class<?> rawType = TypeUtil.getRawType(type);
            final Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (rawType.equals(Provider.class)) {
                // generics abuse ahoy
                final InjectionPoint<Provider<T>> ip = TypeUtil.cast(point);
                final Variable<Provider<T>> variable = elementManager.createVariable(ip);
                // if a Provider<T> is requested, we can convert an existing Bean<T> into a Bean<Provider<T>>
                return this.<T>getBean(actualType, qualifiers)
                        .map(bean -> new ProviderBean<>(variable, context -> getValue(bean, context)))
                        .map(this::addBean)
                        .map(TypeUtil::cast);
            } else if (rawType.equals(Optional.class)) {
                final InjectionPoint<Optional<T>> ip = TypeUtil.cast(point);
                final Variable<Optional<T>> optionalVariable = elementManager.createVariable(ip);
                final Optional<Bean<T>> actualExistingBean = getExistingOrProvidedBean(actualType, qualifiers,
                        // FIXME: remove need for DefaultVariable to be public
                        () -> DefaultVariable.newVariable(
                                TypeUtil.getTypeClosure(actualType), qualifiers, optionalVariable.getScopeType()));
                final Bean<Optional<T>> optionalBean = addBean(new OptionalBean<>(optionalVariable,
                        context -> actualExistingBean.map(bean -> getValue(bean, context))));
                return Optional.of(TypeUtil.cast(optionalBean));
            }
        }
        return Optional.empty();
    }

    private <T> Optional<Bean<T>> getExistingOrProvidedBean(final Type type, final Qualifiers qualifiers,
                                                            final Supplier<Variable<T>> variableSupplier) {
        final Optional<Bean<T>> existingBean = getBean(type, qualifiers);
        if (existingBean.isPresent()) {
            return existingBean;
        }
        final Variable<T> variable = variableSupplier.get();
        final Type providerType = new ParameterizedTypeImpl(null, Provider.class, type);
        final Optional<Bean<Provider<T>>> providerBean = getBean(providerType, qualifiers);
        return providerBean.map(bean -> new ProvidedBean<>(variable, context -> getValue(bean, context).get()))
                .map(this::addBean);
    }

    private <T> Optional<Bean<T>> getBean(final Type type, final Qualifiers qualifiers) {
        final Set<Bean<T>> beans = this.<T>streamBeansMatchingType(type)
                .filter(bean -> qualifiers.equals(bean.getQualifiers()))
                .collect(Collectors.toSet());
        if (beans.size() > 1) {
            throw new AmbiguousBeanException(beans, "type " + type + " and qualifiers " + qualifiers);
        }
        return beans.isEmpty() ? Optional.empty() : Optional.of(beans.iterator().next());
    }

    private <T> Stream<Bean<T>> streamBeansMatchingType(final Type requiredType) {
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
    public <T> InitializationContext<T> createInitializationContext(final Scoped<T> scoped) {
        return new DefaultInitializationContext<>(scoped);
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
            throw new InjectionException("No active scope context found for scope @" + scopeType.getName());
        }
        return scopeContext;
    }

    @Override
    public <T> Optional<T> getInjectableValue(final InjectionPoint<T> point, final InitializationContext<?> parentContext) {
        final Bean<T> resolvedBean = getInjectionPointBean(point)
                .orElseThrow(() -> new UnsatisfiedBeanException(point));
        final Optional<T> existingValue = point.getBean()
                .filter(bean -> !bean.equals(resolvedBean))
                .flatMap(bean -> getExistingInjectableValue(resolvedBean, bean, parentContext));
        if (existingValue.isPresent()) {
            return existingValue;
        }
        final InitializationContext<?> context =
                resolvedBean.isDependentScoped() ? parentContext : createInitializationContext(resolvedBean);
        return Optional.of(getValue(resolvedBean, context));
    }

    private <T> Optional<T> getExistingInjectableValue(final Bean<T> resolvedBean, final Bean<?> pointBean,
                                                       final InitializationContext<?> parentContext) {
        if (resolvedBean.getScopeType() != Singleton.class) {
            return Optional.empty();
        }
        final Optional<Bean<?>> bean;
        if (pointBean.isDependentScoped() && !resolvedBean.isDependentScoped()) {
            bean = parentContext.getNonDependentScopedDependent().filter(Bean.class::isInstance).map(TypeUtil::cast);
        } else {
            bean = Optional.of(pointBean);
        }
        return bean.filter(b -> Singleton.class == b.getScopeType())
                .flatMap(b -> {
                    final Optional<T> incompleteInstance = parentContext.getIncompleteInstance(resolvedBean);
                    if (incompleteInstance.isPresent()) {
                        return incompleteInstance;
                    }
                    return getScopeContext(resolvedBean.getScopeType()).getIfExists(resolvedBean);
                });
    }

    @Override
    public void close() {
        beansByType.clear();
        enabledBeans.clear();
        sharedBeans.clear();
        disposesMethods.clear();
        scopes.values().forEach(ScopeContext::close);
        scopes.clear();
    }

    private static class DisposesMethod<D> {
        private final Type type;
        private final Qualifiers qualifiers;
        private final Bean<D> declaringBean;
        private final MetaMethod<D, ?> disposesMethod;

        private DisposesMethod(final Type type, final Qualifiers qualifiers,
                               final Bean<D> declaringBean, final MetaMethod<D, ?> disposesMethod) {
            this.type = type;
            this.qualifiers = qualifiers;
            this.declaringBean = declaringBean;
            this.disposesMethod = disposesMethod;
        }

        boolean matches(final Variable<?> variable, final Bean<?> declaringBean) {
            return Objects.equals(declaringBean, this.declaringBean) &&
                    variable.hasMatchingType(type) &&
                    qualifiers.equals(variable.getQualifiers());
        }
    }

}
