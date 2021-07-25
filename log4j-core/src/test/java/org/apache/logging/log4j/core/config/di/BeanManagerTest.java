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

package org.apache.logging.log4j.core.config.di;

import org.apache.logging.log4j.core.config.di.impl.DefaultBeanManager;
import org.apache.logging.log4j.plugins.di.Inject;
import org.apache.logging.log4j.plugins.di.Named;
import org.apache.logging.log4j.plugins.di.PostConstruct;
import org.apache.logging.log4j.plugins.di.PreDestroy;
import org.apache.logging.log4j.plugins.di.Produces;
import org.apache.logging.log4j.plugins.di.Provider;
import org.apache.logging.log4j.plugins.di.SingletonScoped;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class BeanManagerTest {

    final BeanManager beanManager = new DefaultBeanManager();

    @AfterEach
    void tearDown() {
        beanManager.close();
    }

    @SingletonScoped
    static class SingletonBean {
    }

    @Test
    void defaultConstructorInjection() {
        beanManager.loadAndValidateBeans(SingletonBean.class);
        final Bean<SingletonBean> bean = beanManager.getDefaultBean(SingletonBean.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final SingletonBean value = beanManager.getValue(bean, context);
            assertNotNull(value);
        }
    }

    @Produces
    @Named
    static SingletonBean getSingleton() {
        return new SingletonBean();
    }

    static class ParameterInjectionBean {
        final SingletonBean first;
        SingletonBean second;
        SingletonBean singletonBean;

        @Inject
        ParameterInjectionBean(@Named("singleton") final SingletonBean first) {
            this.first = first;
        }

        @Inject
        void parameterInjection(@Named("singleton") final SingletonBean second, final SingletonBean singletonBean) {
            this.second = second;
            this.singletonBean = singletonBean;
        }
    }

    @Test
    void parameterInjection() {
        beanManager.loadAndValidateBeans(getClass(), SingletonBean.class, ParameterInjectionBean.class);
        final Bean<ParameterInjectionBean> bean = beanManager.getDefaultBean(ParameterInjectionBean.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final ParameterInjectionBean value = beanManager.getValue(bean, context);
            assertNotNull(value);
            assertNotNull(value.first);
            assertNotNull(value.second);
            assertNotNull(value.singletonBean);
            assertSame(value.first, value.second);
            assertNotSame(value.first, value.singletonBean);
        }
    }

    static class FieldInjectionBean {
        @Inject
        @Named("singleton")
        private SingletonBean singletonBean;

        @Inject
        private SingletonBean singleton;

        @Named("primary name")
        @Named("first alias")
        @Named("singleton")
        @Named("one more alias")
        private SingletonBean bean;
    }

    @Test
    void fieldInjection() {
        beanManager.loadAndValidateBeans(getClass(), SingletonBean.class, FieldInjectionBean.class);
        final Bean<FieldInjectionBean> bean = beanManager.getDefaultBean(FieldInjectionBean.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final FieldInjectionBean value = beanManager.getValue(bean, context);
            assertNotNull(value.singletonBean);
            assertNotNull(value.singleton);
            assertNotNull(value.bean);
            assertNotSame(value.singletonBean, value.singleton);
            assertSame(value.singletonBean, value.bean);
        }
    }

    static class ImplicitConstructorBean {
        private final SingletonBean singleton;

        ImplicitConstructorBean(@Named final SingletonBean singleton) {
            this.singleton = singleton;
        }
    }

    @Test
    void implicitConstructorInjection() {
        beanManager.loadAndValidateBeans(getClass(), SingletonBean.class, ImplicitConstructorBean.class);
        final Bean<ImplicitConstructorBean> bean = beanManager.getDefaultBean(ImplicitConstructorBean.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final ImplicitConstructorBean value = beanManager.getValue(bean, context);
            assertNotNull(value.singleton);
        }
    }

    static class ExplicitConstructorBean {
        private final SingletonBean singleton;

        @Inject
        ExplicitConstructorBean(final SingletonBean singleton) {
            this.singleton = singleton;
        }
    }

    @Test
    void explicitConstructorInjection() {
        beanManager.loadAndValidateBeans(getClass(), SingletonBean.class, ExplicitConstructorBean.class);
        final Bean<ExplicitConstructorBean> bean = beanManager.getDefaultBean(ExplicitConstructorBean.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final ExplicitConstructorBean value = beanManager.getValue(bean, context);
            assertNotNull(value.singleton);
        }
    }

    @Named("dep")
    static class DependentBean {
    }

    static class ScopeTestingBean {
        @Named("dep")
        DependentBean field;

        DependentBean first;
        DependentBean second;

        ScopeTestingBean(@Named("dep") DependentBean first, @Named("dep") DependentBean second) {
            this.first = first;
            this.second = second;
        }
    }

    @Test
    void dependentScope() {
        beanManager.loadAndValidateBeans(DependentBean.class, ScopeTestingBean.class);
        final Bean<ScopeTestingBean> bean = beanManager.getDefaultBean(ScopeTestingBean.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final ScopeTestingBean value = beanManager.getValue(bean, context);
            assertNotSame(value.field, value.first);
            assertNotSame(value.field, value.second);
            assertNotSame(value.first, value.second);
        }
    }

    static class StaticMethodProducerBean {
        final DependentBean first;
        final DependentBean second;
        final SingletonBean singletonBean;

        private StaticMethodProducerBean(final DependentBean first, final DependentBean second, final SingletonBean singletonBean) {
            this.first = first;
            this.second = second;
            this.singletonBean = singletonBean;
        }

        @Produces
        @Named("static")
        static StaticMethodProducerBean create(@Named("dep") final DependentBean first,
                                               @Named("dep") final DependentBean second,
                                               @Named final SingletonBean singleton) {
            return new StaticMethodProducerBean(first, second, singleton);
        }
    }

    @Test
    void staticMethodProducer() {
        beanManager.loadAndValidateBeans(getClass(), SingletonBean.class, DependentBean.class, StaticMethodProducerBean.class);
        final Bean<StaticMethodProducerBean> bean = beanManager.getNamedBean(StaticMethodProducerBean.class, "static").orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final StaticMethodProducerBean value = beanManager.getValue(bean, context);
            assertNotNull(value.first);
            assertNotNull(value.second);
            assertNotSame(value.first, value.second);
            assertNotNull(value.singletonBean);
        }
    }

    static class ProviderClassProducerBean {
        final DependentBean first;
        final DependentBean second;
        final SingletonBean singletonBean;

        private ProviderClassProducerBean(final DependentBean first, final DependentBean second, final SingletonBean singletonBean) {
            this.first = first;
            this.second = second;
            this.singletonBean = singletonBean;
        }

        @Named("builder")
        static class Builder implements Provider<ProviderClassProducerBean> {
            private DependentBean dep;
            private DependentBean bean;
            private SingletonBean singleton;

            @Inject
            public Builder setDep(@Named final DependentBean dep) {
                this.dep = dep;
                return this;
            }

            @Inject
            public Builder setBean(@Named("dep") final DependentBean bean) {
                this.bean = bean;
                return this;
            }

            @Inject
            public Builder setSingleton(@Named final SingletonBean singleton) {
                this.singleton = singleton;
                return this;
            }

            @Override
            public ProviderClassProducerBean get() {
                return new ProviderClassProducerBean(dep, bean, singleton);
            }
        }
    }

    @Test
    void providerClassProducer() {
        beanManager.loadAndValidateBeans(getClass(), SingletonBean.class, DependentBean.class, ProviderClassProducerBean.Builder.class);
        final Bean<ProviderClassProducerBean> bean = beanManager.getNamedBean(ProviderClassProducerBean.class, "builder").orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final ProviderClassProducerBean value = beanManager.getValue(bean, context);
            assertNotNull(value.first);
            assertNotNull(value.second);
            assertNotSame(value.first, value.second);
            assertNotNull(value.singletonBean);
        }
    }

    static class ProviderParameterInjectionBean {
        final DependentBean alpha;
        final DependentBean beta;
        final DependentBean gamma;

        @Inject
        ProviderParameterInjectionBean(@Named("dep") final Provider<DependentBean> beanProvider) {
            alpha = beanProvider.get();
            beta = beanProvider.get();
            gamma = beanProvider.get();
        }
    }

    @Test
    void providerParameterInjection() {
        beanManager.loadAndValidateBeans(DependentBean.class, ProviderParameterInjectionBean.class);
        final Bean<ProviderParameterInjectionBean> bean = beanManager.getDefaultBean(ProviderParameterInjectionBean.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final ProviderParameterInjectionBean value = beanManager.getValue(bean, context);
            assertNotSame(value.alpha, value.beta);
            assertNotSame(value.beta, value.gamma);
            assertNotSame(value.gamma, value.alpha);
        }
    }

    static class SimpleBean {
    }

    static class OptionalInjection {
        @Inject
        private Optional<SimpleBean> field;
        private final SimpleBean arg;
        private SimpleBean methodArg;

        @Inject
        OptionalInjection(final Optional<SimpleBean> arg) {
            this.arg = arg.orElse(null);
        }

        @Inject
        void setMethodArg(final Optional<SimpleBean> methodArg) {
            this.methodArg = methodArg.orElse(null);
        }
    }

    @Test
    void optionalInjectionWhenBeanIsAbsent() {
        beanManager.loadAndValidateBeans(OptionalInjection.class);
        final Bean<OptionalInjection> bean = beanManager.getDefaultBean(OptionalInjection.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final OptionalInjection value = beanManager.getValue(bean, context);
            assertTrue(value.field.isEmpty());
            assertNull(value.arg);
            assertNull(value.methodArg);
        }
    }

    @Test
    void optionalInjectionWhenBeanIsPresent() {
        beanManager.loadAndValidateBeans(SimpleBean.class, OptionalInjection.class);
        final Bean<OptionalInjection> bean = beanManager.getDefaultBean(OptionalInjection.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final OptionalInjection value = beanManager.getValue(bean, context);
            assertTrue(value.field.isPresent());
            assertNotNull(value.arg);
            assertNotNull(value.methodArg);
        }
    }

    @SingletonScoped
    static class IdGenerator {
        private final AtomicInteger current = new AtomicInteger();

        @Produces
        public int nextId() {
            return current.incrementAndGet();
        }

        public int getCurrent() {
            return current.get();
        }
    }

    static class PostConstructInjection {
        private int one;
        private int two;
        @Inject
        private int three;
        private int four;
        private int five;
        private int six;
        @Inject
        private IdGenerator idGenerator;

        @Inject
        public PostConstructInjection(final int a, final int b) {
            one = a;
            two = b;
        }

        @PostConstruct
        public void init() {
            six = idGenerator.nextId();
        }

        @Inject
        public void setValue(final int value, final Integer otherValue) {
            four = value;
            five = otherValue;
        }

        @PreDestroy
        public void destroy() {
            idGenerator.nextId();
            one = -1;
        }
    }

    @Test
    void postConstructPreDestroy() {
        beanManager.loadAndValidateBeans(IdGenerator.class, PostConstructInjection.class);
        final Bean<IdGenerator> idGeneratorBean = beanManager.getDefaultBean(IdGenerator.class).orElseThrow();
        try (var context = beanManager.createInitializationContext(null)) {
            final IdGenerator idGenerator = beanManager.getValue(idGeneratorBean, context);
            assertEquals(0, idGenerator.getCurrent());
            final Bean<PostConstructInjection> bean = beanManager.getDefaultBean(PostConstructInjection.class).orElseThrow();
            final PostConstructInjection value = beanManager.getValue(bean, context);
            assertEquals(1, value.one);
            assertEquals(2, value.two);
            assertEquals(3, value.three);
            assertEquals(4, value.four);
            assertEquals(5, value.five);
            assertEquals(6, value.six);
            bean.destroy(value, context.createDependentContext(bean));
            assertEquals(7, idGenerator.getCurrent());
            assertEquals(-1, value.one);
        }
    }

    @SingletonScoped
    static class DeferredSingleton {
        private final int id;

        @Inject
        public DeferredSingleton(final int id) {
            this.id = id;
        }
    }

    static class DeferredDependent {
        private final int id;

        @Inject
        public DeferredDependent(final int id) {
            this.id = id;
        }
    }

    static class DeferredProviderBean {
        @Inject
        IdGenerator generator;

        @Inject
        Provider<DeferredSingleton> singletonProvider;

        @Inject
        Provider<DeferredDependent> dependentProvider;
    }

    @Test
    void testDeferredProviderNotInvokedUntilInitiallyProvided() {
        beanManager.loadAndValidateBeans(IdGenerator.class, DeferredSingleton.class, DeferredDependent.class, DeferredProviderBean.class);
        final Bean<DeferredProviderBean> bean = beanManager.getDefaultBean(DeferredProviderBean.class).orElseThrow();
        try (var context = beanManager.createInitializationContext(null)) {
            final DeferredProviderBean value = beanManager.getValue(bean, context);
            final IdGenerator generator = value.generator;
            final Provider<DeferredSingleton> singletonProvider = value.singletonProvider;
            final Provider<DeferredDependent> dependentProvider = value.dependentProvider;
            assertEquals(0, generator.getCurrent());
            assertEquals(1, singletonProvider.get().id);
            assertEquals(1, generator.getCurrent());
            assertEquals(1, singletonProvider.get().id);
            assertEquals(1, generator.getCurrent());
            assertEquals(2, dependentProvider.get().id);
            assertEquals(2, generator.getCurrent());
            assertEquals(1, singletonProvider.get().id);
            assertEquals(2, generator.getCurrent());
            assertEquals(3, dependentProvider.get().id);
            assertEquals(3, generator.getCurrent());
            assertEquals(1, singletonProvider.get().id);
            assertEquals(3, generator.getCurrent());
            assertEquals(4, dependentProvider.get().id);
            assertEquals(4, generator.getCurrent());
        }
    }

    static class PostConstructBaseBean {
        final int baseConstructorParameterA;
        final int baseConstructorParameterB;
        @Inject
        int baseInjectedField;
        int basePostConstructValue;
        int baseInjectedParameterA;
        int baseInjectedParameterB;
        @Inject
        IdGenerator idGenerator;

        @Inject
        PostConstructBaseBean(int baseConstructorParameterA, int baseConstructorParameterB) {
            this.baseConstructorParameterA = baseConstructorParameterA;
            this.baseConstructorParameterB = baseConstructorParameterB;
        }

        @PostConstruct
        void setupBase() {
            basePostConstructValue = idGenerator.nextId();
        }

        @Inject
        void setValues(int e, int f) {
            this.baseInjectedParameterA = e;
            this.baseInjectedParameterB = f;
        }
    }

    static class PostConstructBean extends PostConstructBaseBean {
        final int implConstructorParameter;
        int implPostConstructValue;
        int implInjectedParameter;
        @Inject
        int implInjectedField;

        @Inject
        PostConstructBean(final int a, final int b, final int implConstructorParameter) {
            super(a, b);
            this.implConstructorParameter = implConstructorParameter;
        }

        @PostConstruct
        void setupBean() {
            implPostConstructValue = idGenerator.nextId();
        }

        @Inject
        void setImplInjectedParameter(int implInjectedParameter) {
            this.implInjectedParameter = implInjectedParameter;
        }
    }

    @Test
    void postConstructInheritanceOrdering() {
        beanManager.loadAndValidateBeans(IdGenerator.class, PostConstructBean.class);
        final Bean<PostConstructBean> bean = beanManager.getDefaultBean(PostConstructBean.class).orElseThrow();
        try (final var context = beanManager.createInitializationContext(null)) {
            final PostConstructBean value = beanManager.getValue(bean, context);
            int[] values = {
                    value.baseConstructorParameterA,
                    value.baseConstructorParameterB,
                    value.implConstructorParameter,
                    value.implInjectedField,
                    value.baseInjectedField,
                    value.implInjectedParameter,
                    value.baseInjectedParameterA,
                    value.baseInjectedParameterB,
                    value.basePostConstructValue,
                    value.implPostConstructValue
            };
            assertAll(IntStream.range(0, values.length).mapToObj(i -> () -> assertEquals(i + 1, values[i])));
        }
    }

    static class PreDestroyBaseBean {
        @Inject
        IdGenerator idGenerator;
        int baseValue;

        @PreDestroy
        void basePreDestroy() {
            baseValue = idGenerator.nextId();
        }
    }

    static class PreDestroyIntermediateBean extends PreDestroyBaseBean {
        int intermediateValue;

        @PreDestroy
        void intermediatePreDestroy() {
            intermediateValue = idGenerator.nextId();
        }
    }

    static class PreDestroyImplBean extends PreDestroyIntermediateBean {
        int value;

        @PreDestroy
        void tearDown() {
            value = idGenerator.nextId();
        }
    }

    @Test
    void preDestroyInheritanceOrdering() {
        beanManager.loadAndValidateBeans(IdGenerator.class, PreDestroyImplBean.class);
        final Bean<PreDestroyImplBean> bean = beanManager.getDefaultBean(PreDestroyImplBean.class).orElseThrow();
        final PreDestroyImplBean value;
        try (final var context = beanManager.createInitializationContext(bean)) {
            value = beanManager.getValue(bean, context);
            assertAll(
                    () -> assertEquals(0, value.value),
                    () -> assertEquals(0, value.intermediateValue),
                    () -> assertEquals(0, value.baseValue)
            );
        }
        assertAll(
                () -> assertEquals(1, value.value),
                () -> assertEquals(2, value.intermediateValue),
                () -> assertEquals(3, value.baseValue)
        );
    }

    // TODO: add tests for other supported injection scenarios
    // TODO: add tests for hierarchical scopes
    // TODO: add tests for @Disposes
    // TODO: add tests for injecting more specific types than the available ones
}
