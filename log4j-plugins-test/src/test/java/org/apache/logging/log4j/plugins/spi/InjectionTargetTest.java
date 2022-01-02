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

package org.apache.logging.log4j.plugins.spi;

import org.apache.logging.log4j.plugins.di.Inject;
import org.apache.logging.log4j.plugins.di.Named;
import org.apache.logging.log4j.plugins.di.Produces;
import org.apache.logging.log4j.plugins.di.SingletonScoped;
import org.apache.logging.log4j.plugins.spi.impl.DefaultBeanManager;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InjectionTargetTest {

    @SingletonScoped
    static class SingletonBean {
    }

    @Produces
    @Named
    @SingletonScoped
    static SingletonBean foo() {
        return new SingletonBean();
    }

    static class InjectedSingletonBeans {
        private final SingletonBean foo;
        private SingletonBean bar;
        private SingletonBean bean;

        @Inject
        InjectedSingletonBeans(@Named final SingletonBean foo) {
            this.foo = foo;
        }

        @Inject
        void setBar(final SingletonBean bar) {
            this.bar = bar;
        }

        @Inject
        void setBean(@Named("foo") final SingletonBean bean) {
            this.bean = bean;
        }
    }

    @Test
    void singletonBeans() {
        final BeanManager beanManager = new DefaultBeanManager();
        beanManager.loadAndValidateBeans(SingletonBean.class, getClass(), InjectedSingletonBeans.class);
        final Optional<Bean<InjectedSingletonBeans>> bean = beanManager.getDefaultBean(InjectedSingletonBeans.class);
        assertTrue(bean.isPresent());
        try (InitializationContext<InjectedSingletonBeans> context = beanManager.createInitializationContext(null)) {
            final InjectedSingletonBeans beans = beanManager.getValue(bean.orElseThrow(), context);
            assertNotNull(beans);
            assertNotNull(beans.foo);
            assertNotNull(beans.bar);
            assertNotSame(beans.foo, beans.bar);
            assertSame(beans.foo, beans.bean);
        }
    }

    @Named("empty")
    static class EmptyBean {
    }

    static class DependentBeans {
        private final EmptyBean first;
        private final EmptyBean second;

        DependentBeans(@Named("empty") final EmptyBean first, @Named("empty") final EmptyBean second) {
            this.first = first;
            this.second = second;
        }
    }

    @Test
    void dependentBeans() {
        final BeanManager beanManager = new DefaultBeanManager();
        beanManager.loadAndValidateBeans(DependentBeans.class, EmptyBean.class);
        final Optional<Bean<DependentBeans>> bean = beanManager.getDefaultBean(DependentBeans.class);
        try (InitializationContext<DependentBeans> context = beanManager.createInitializationContext(null)) {
            final DependentBeans beans = beanManager.getValue(bean.orElseThrow(), context);
            assertNotNull(beans);
            assertNotSame(beans.first, beans.second);
        }
    }
}
