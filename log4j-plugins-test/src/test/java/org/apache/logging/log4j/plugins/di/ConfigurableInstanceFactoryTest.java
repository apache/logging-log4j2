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
package org.apache.logging.log4j.plugins.di;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.test.validation.di.AlphaBean;
import org.apache.logging.log4j.plugins.test.validation.di.BaseBean;
import org.apache.logging.log4j.plugins.test.validation.di.BetaBean;
import org.apache.logging.log4j.plugins.test.validation.di.GammaBean;
import org.apache.logging.log4j.plugins.test.validation.di.PrototypeBean;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.junit.jupiter.api.Test;

class ConfigurableInstanceFactoryTest {

    static class CircularBean {
        final CircularSupplierBean bean;

        @Inject
        CircularBean(final CircularSupplierBean bean) {
            this.bean = bean;
        }
    }

    static class CircularSupplierBean {
        final Supplier<CircularBean> beanSupplier;

        @Inject
        CircularSupplierBean(final Supplier<CircularBean> beanSupplier) {
            this.beanSupplier = beanSupplier;
        }
    }

    @Test
    void circularDependenciesWithSupplier() {
        final CircularSupplierBean bean = DI.createInitializedFactory().getInstance(CircularSupplierBean.class);
        assertThat(bean.beanSupplier.get()).isNotNull();
    }

    static class UnknownInstance {
        UnknownInstance(@Named String ignored) {}
    }

    @Test
    void unknownInstanceError() {
        final Key<UnknownInstance> key = new Key<>() {};
        assertThatThrownBy(() -> DI.createInitializedFactory().getInstance(key))
                .hasMessage("No @Inject constructor or default constructor found for " + key);
    }

    @Test
    void optionalUnknownInstance() {
        final Key<Optional<UnknownInstance>> key = new Key<>() {};
        assertThat(DI.createInitializedFactory().getInstance(key)).isEmpty();
    }

    @Singleton
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

    static class DeferredSupplierBean {
        @Inject
        Supplier<DeferredSingleton> singletonSupplier;

        @Inject
        Supplier<DeferredDependent> dependentSupplier;

        Supplier<DeferredDependent> methodInjectedSupplier;

        @Inject
        void setMethodInjectedSupplier(final Supplier<DeferredDependent> supplier) {
            methodInjectedSupplier = supplier;
        }
    }

    @Test
    void testDeferredSupplierNotInvokedUntilInitiallyProvided() {
        final AtomicInteger counter = new AtomicInteger();
        final DeferredSupplierBean bean = DI.builder()
                .addInitialBindingFrom(int.class)
                .toUnscoped(counter::incrementAndGet)
                .build()
                .getInstance(DeferredSupplierBean.class);
        assertThat(counter.get()).isEqualTo(0);
        assertThat(bean.singletonSupplier.get().id).isEqualTo(1);
        assertThat(bean.singletonSupplier.get().id).isEqualTo(1);
        assertThat(counter.get()).isEqualTo(1);
        assertThat(bean.dependentSupplier.get().id).isEqualTo(2);
        assertThat(bean.singletonSupplier.get().id).isEqualTo(1);
        assertThat(counter.get()).isEqualTo(2);
        assertThat(bean.dependentSupplier.get().id).isEqualTo(3);
        assertThat(bean.singletonSupplier.get().id).isEqualTo(1);
        assertThat(counter.get()).isEqualTo(3);
        assertThat(bean.dependentSupplier.get().id).isEqualTo(4);
        assertThat(bean.singletonSupplier.get().id).isEqualTo(1);
        assertThat(counter.get()).isEqualTo(4);
        assertThat(bean.methodInjectedSupplier).isNotNull();
        assertThat(bean.methodInjectedSupplier.get().id).isEqualTo(5);
        assertThat(counter.get()).isEqualTo(5);
    }

    static class AliasBundle {
        @Factory
        @Named({"foo", "bar"})
        String foo() {
            return "bar";
        }
    }

    static class Aliases {
        @Named
        String foo;

        @Named
        String bar;

        @Named({"invalid", "foo"})
        String baz;

        final String constructed;

        String methodInjected;

        @Inject
        Aliases(@Named({"", "foo"}) final String constructed) {
            this.constructed = constructed;
        }

        @Inject
        void setMethodInjected(@Named({"baz", "bar"}) final String methodInjected) {
            this.methodInjected = methodInjected;
        }
    }

    @Test
    void supplierAliases() {
        final Aliases aliases =
                DI.builder().addBundle(AliasBundle.class).build().getInstance(Aliases.class);
        assertThat(List.of(aliases.foo, aliases.bar, aliases.baz, aliases.constructed, aliases.methodInjected))
                .allMatch("bar"::equals);
    }

    static class ValidatedInjectionPoints {
        @Required
        @Named
        String foo;

        final String bar;

        @Inject
        ValidatedInjectionPoints(@Required @Named final String bar) {
            this.bar = bar;
        }
    }

    @Test
    void injectionPointValidation() {
        final var injector = DI.createInitializedFactory();
        assertThatThrownBy(() -> injector.getInstance(ValidatedInjectionPoints.class))
                .isInstanceOf(NoQualifiedBindingException.class);
    }

    @Test
    void injectionPointValidationPartial() {
        final ConfigurableInstanceFactory instanceFactory = DI.builder()
                .addInitialBindingFrom(new @Named("foo") Key<String>() {})
                .toInstance("hello")
                .build();
        assertThatThrownBy(() -> instanceFactory.getInstance(ValidatedInjectionPoints.class))
                .isInstanceOf(NoQualifiedBindingException.class);
    }

    @Test
    void injectionPointValidationFull() {
        final ConfigurableInstanceFactory instanceFactory = DI.builder()
                .addInitialBindingFrom(new @Named("foo") Key<String>() {})
                .toInstance("hello")
                .addInitialBindingFrom(new @Named("bar") Key<String>() {})
                .toInstance("world")
                .build();
        final ValidatedInjectionPoints instance = instanceFactory.getInstance(ValidatedInjectionPoints.class);
        assertThat(instance.foo).isEqualTo("hello");
        assertThat(instance.bar).isEqualTo("world");
    }

    static class OptionalInjection {
        @Inject
        Optional<AlphaBean> a;

        final BetaBean b;
        PrototypeBean c;

        @Inject
        OptionalInjection(final Optional<BetaBean> b) {
            this.b = b.orElse(null);
        }

        @Inject
        void setC(final Optional<PrototypeBean> c) {
            this.c = c.orElse(null);
        }
    }

    @Test
    void optionalInjection() {
        final var instanceFactory = DI.createInitializedFactory();
        final OptionalInjection first = instanceFactory.getInstance(OptionalInjection.class);
        final OptionalInjection second = instanceFactory.getInstance(OptionalInjection.class);
        assertThat(first.a).isPresent().isEqualTo(second.a);
        assertThat(first.b).isNotNull().isEqualTo(second.b);
        assertThat(first.c).isNotNull().isNotSameAs(second.c);
        assertThat(second.c).isNotNull();
    }

    /**
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-3496">LOG4J2-3496</a>
     */
    static class ContainerPluginBeanInjection {
        @Namespace("Bean")
        @Inject
        Optional<BaseBean> optional;

        @Namespace("Bean")
        @Inject
        Collection<BaseBean> collection;

        @Namespace("Bean")
        @Inject
        Collection<Supplier<BaseBean>> collectionFactory;

        @Namespace("Bean")
        @Inject
        Iterable<BaseBean> iterable;

        @Namespace("Bean")
        @Inject
        Iterable<Supplier<BaseBean>> iterableFactory;

        @Namespace("Bean")
        @Inject
        Set<BaseBean> set;

        @Namespace("Bean")
        @Inject
        Set<Supplier<BaseBean>> setFactory;

        @Namespace("Bean")
        @Inject
        Stream<BaseBean> stream;

        @Namespace("Bean")
        @Inject
        Stream<Supplier<BaseBean>> streamFactory;

        @Namespace("Bean")
        @Inject
        List<BaseBean> list;

        @Namespace("Bean")
        @Inject
        List<Supplier<BaseBean>> listFactory;

        @Namespace("Bean")
        @Inject
        Map<String, BaseBean> map;

        @Namespace("Bean")
        @Inject
        Map<String, Supplier<BaseBean>> mapFactory;
    }

    @Test
    void namespaceQualifierInjection() {
        final ContainerPluginBeanInjection instance =
                DI.createInitializedFactory().getInstance(ContainerPluginBeanInjection.class);
        assertThat(instance.list).hasSize(3).first().isInstanceOf(BetaBean.class);
        assertThat(instance.collection).containsExactlyElementsOf(instance.list);
        assertThat(instance.iterable).containsExactlyElementsOf(instance.list);
        assertThat(instance.set).containsExactlyElementsOf(instance.list);
        assertThat(instance.stream).containsExactlyElementsOf(instance.list);
        assertThat(instance.map).hasSize(3);
        assertThat(instance.map.get("gamma")).isInstanceOf(GammaBean.class);
        assertThat(instance.optional).get().isInstanceOf(BetaBean.class);
        assertThat(instance.collectionFactory).hasSize(3);
        assertThat(instance.collectionFactory.stream().map(Supplier::get).collect(Collectors.toList()))
                .containsExactlyElementsOf(instance.collection);
        assertThat(instance.iterableFactory).hasSize(3);
        assertThat(instance.setFactory).hasSize(3);
        assertThat(instance.setFactory.stream().map(Supplier::get).collect(Collectors.toList()))
                .containsExactlyElementsOf(instance.set);
        assertThat(instance.streamFactory.map(Supplier::get).collect(Collectors.toList()))
                .containsExactlyElementsOf(instance.list);
        assertThat(instance.listFactory.stream().map(Supplier::get).collect(Collectors.toList()))
                .containsExactlyElementsOf(instance.list);
        assertThat(instance.mapFactory).hasSize(3);
        assertThat(instance.mapFactory.get("alpha").get()).isInstanceOf(AlphaBean.class);
        assertThat(instance.mapFactory.get("beta").get()).isInstanceOf(BetaBean.class);
        assertThat(instance.mapFactory.get("gamma").get()).isInstanceOf(GammaBean.class);
    }
}
