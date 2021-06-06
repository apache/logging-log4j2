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

package org.apache.logging.log4j.core.config.di.impl.bean;

import org.apache.logging.log4j.core.test.junit.BeanJUnit4Runner;
import org.apache.logging.log4j.core.test.junit.WithBeans;
import org.apache.logging.log4j.plugins.api.Default;
import org.apache.logging.log4j.plugins.api.Inject;
import org.apache.logging.log4j.plugins.api.Named;
import org.apache.logging.log4j.plugins.api.PostConstruct;
import org.apache.logging.log4j.plugins.api.Produces;
import org.apache.logging.log4j.plugins.api.Provider;
import org.apache.logging.log4j.plugins.api.QualifierType;
import org.apache.logging.log4j.plugins.api.SingletonScoped;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(BeanJUnit4Runner.class)
public class DefaultBeanManagerTest {

    @Retention(RetentionPolicy.RUNTIME)
    @QualifierType
    public @interface Run {
    }

    @Produces
    @SingletonScoped
    public String globalString = "global string value";

    @Produces
    @SingletonScoped
    @Run
    public String testString() {
        return "test string value";
    }

    @Test
    public void testParameterInjection(final String unqualified, @Run final String qualified) {
        assertEquals(globalString, unqualified);
        assertEquals(testString(), qualified);
    }

    public static class FieldInjection {
        @Inject
        private String unqualified;
        @Run
        private String implicitQualified;
        @Default
        private String implicitDefault;
    }

    @WithBeans(FieldInjection.class)
    @Test
    public void testFieldInjection(final FieldInjection instance) {
        assertEquals(globalString, instance.unqualified);
        assertEquals(testString(), instance.implicitQualified);
        assertEquals(globalString, instance.implicitDefault);
    }

    public static class ExplicitConstructorInjection {
        private final String first;
        private final String second;

        @Inject
        public ExplicitConstructorInjection(@Default final String first, @Run final String second) {
            this.first = first;
            this.second = second;
        }
    }

    @WithBeans(ExplicitConstructorInjection.class)
    @Test
    public void testExplicitConstructorInjection(final ExplicitConstructorInjection instance) {
        assertEquals(globalString, instance.first);
        assertEquals(testString(), instance.second);
    }

    public static class ImplicitConstructorInjection {
        private final String first;
        private final String second;

        public ImplicitConstructorInjection(final String first, @Run final String second) {
            this.first = first;
            this.second = second;
        }
    }

    @WithBeans(ImplicitConstructorInjection.class)
    @Test
    public void testImplicitConstructorInjection(final ImplicitConstructorInjection instance) {
        assertEquals(globalString, instance.first);
        assertEquals(testString(), instance.second);
    }

    public static class DefaultConstructorInjection {
    }

    @WithBeans(DefaultConstructorInjection.class)
    @Test
    public void testNoArgsConstructorInjection(final DefaultConstructorInjection instance) {
        assertNotNull(instance);
    }

    @WithBeans(DefaultConstructorInjection.class)
    @Test
    public void testDependentScopeDifferentInstances(final DefaultConstructorInjection first,
                                                     final DefaultConstructorInjection second) {
        assertNotSame(first, second);
    }

    @SingletonScoped
    public static class SingletonInjection {
    }

    @WithBeans(SingletonInjection.class)
    @Test
    public void testSingletonScopeSameInstances(final SingletonInjection first, final SingletonInjection second) {
        assertSame(first, second);
    }

    public static class StaticMethodProduction {
        private final String first;
        private final String second;

        private StaticMethodProduction(final String first, final String second) {
            this.first = first;
            this.second = second;
        }

        @Produces
        public static StaticMethodProduction produce(final String first, @Run final String second) {
            return new StaticMethodProduction(first, second);
        }
    }

    @WithBeans(StaticMethodProduction.class)
    @Test
    public void testStaticMethodProduction(final StaticMethodProduction instance) {
        assertEquals(globalString, instance.first);
        assertEquals(testString(), instance.second);
    }

    public static class ProviderProvidedProduction {
        private final String first;
        private final String second;

        private ProviderProvidedProduction(final String first, final String second) {
            this.first = first;
            this.second = second;
        }

        public static class Builder implements Provider<ProviderProvidedProduction> {
            private String first;
            private String second;

            @Inject
            public Builder withFirst(final String first) {
                this.first = first;
                return this;
            }

            @Inject
            public Builder withSecond(@Run final String second) {
                this.second = second;
                return this;
            }

            @Override
            public ProviderProvidedProduction get() {
                return new ProviderProvidedProduction(first, second);
            }
        }
    }

    @WithBeans(ProviderProvidedProduction.Builder.class)
    @Test
    public void testProviderProvidedProduction(final ProviderProvidedProduction instance) {
        assertEquals(globalString, instance.first);
        assertEquals(testString(), instance.second);
    }

    @Test
    public void testProviderInjection(final Provider<String> stringProvider) {
        assertEquals(globalString, stringProvider.get());
    }

    @Test
    public void testQualifiedProviderInjection(@Run final Provider<String> stringProvider) {
        assertEquals(testString(), stringProvider.get());
    }

    @Test
    public void testOptionalInjectionWhenNoBeanProvided(final Optional<DefaultConstructorInjection> instance) {
        assertFalse(instance.isPresent());
    }

    @WithBeans(DefaultConstructorInjection.class)
    @Test
    public void testOptionalInjectionWhenBeanProvided(final Optional<DefaultConstructorInjection> instance) {
        assertTrue(instance.isPresent());
    }

    @SingletonScoped
    public static class IdGenerator {
        private final AtomicInteger current = new AtomicInteger();

        @Produces
        public int nextId() {
            return current.incrementAndGet();
        }

        public int getCurrent() {
            return current.get();
        }
    }

    public static class PostConstructInjection {
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
    }

    @WithBeans({IdGenerator.class, PostConstructInjection.class})
    @Test
    public void testPostConstructInjection(final PostConstructInjection instance) {
        assertEquals(1, instance.one);
        assertEquals(2, instance.two);
        assertEquals(3, instance.three);
        assertEquals(4, instance.four);
        assertEquals(5, instance.five);
        assertEquals(6, instance.six);
    }

    public static class DefaultNamedQualifier {
        @Produces
        @Named
        public String methodProducer() {
            return "foobar";
        }

        @Produces
        @Named
        public short getAnswer() {
            return 42;
        }
    }

    @Named
    public static class FooBar {
    }

    @WithBeans({DefaultNamedQualifier.class, FooBar.class})
    @Test
    public void testDefaultNamedQualifier(@Named final String methodProducer,
                                          @Named("methodProducer") final String alternative,
                                          @Named final short answer, @Named final FooBar fooBar) {
        assertEquals("foobar", methodProducer);
        assertEquals(methodProducer, alternative);
        assertEquals(42, answer);
        assertNotNull(fooBar);
    }

    @SingletonScoped
    public static class DeferredSingleton {
        private final int id;

        @Inject
        public DeferredSingleton(final int id) {
            this.id = id;
        }
    }

    public static class DeferredDependent {
        private final int id;

        @Inject
        public DeferredDependent(final int id) {
            this.id = id;
        }
    }

    @WithBeans({IdGenerator.class, DeferredSingleton.class, DeferredDependent.class})
    @Test
    public void testDeferredProviderNotInvokedUntilInitiallyProvided(final IdGenerator generator,
                                                                     final Provider<DeferredSingleton> singletonProvider,
                                                                     final Provider<DeferredDependent> dependentProvider) {
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

    // TODO: add tests for other supported injection scenarios
    // TODO: add tests for hierarchical scopes
    // TODO: add tests for @Named alias annotations like @PluginAttribute == @Named
}