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

import org.apache.logging.log4j.plugins.spi.impl.DefaultBeanManager;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.SingletonScoped;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InjectionPointTest {

    @SingletonScoped
    static class SingletonInstance {
    }

    static class FieldBean {
        @Inject
        static SingletonInstance instance;

        @Inject
        SingletonInstance singletonInstance;
    }

    static class GenericBean<T> {
        @Inject
        SingletonInstance instance;

        @Inject
        T value;
    }

    static class ConstructedBean {
        private final String alpha;
        private final String beta;
        private final String gamma;

        ConstructedBean(@Named final String alpha, @Named final String beta, @Named final String gamma) {
            this.alpha = alpha;
            this.beta = beta;
            this.gamma = gamma;
        }
    }

    static class MethodBean {
        private SingletonInstance instance;

        @Inject
        public void setInstance(final SingletonInstance instance) {
            this.instance = instance;
        }
    }

    private final BeanManager beanManager = new DefaultBeanManager();

    @AfterEach
    void tearDown() {
        beanManager.close();
    }

    @Test
    void fieldInjectionPoints() throws Exception {
        final Field staticField = FieldBean.class.getDeclaredField("instance");
        final InjectionPoint staticInjectionPoint = beanManager.createFieldInjectionPoint(staticField, null);
        final Field instanceField = FieldBean.class.getDeclaredField("singletonInstance");
        final InjectionPoint instanceInjectionPoint = beanManager.createFieldInjectionPoint(instanceField, null);
        assertAll(
                () -> assertEquals(Strings.EMPTY, staticInjectionPoint.getName()),
                () -> assertEquals(SingletonInstance.class, staticInjectionPoint.getType()),
                () -> assertEquals(staticField, staticInjectionPoint.getElement()),
                () -> assertEquals(staticField, staticInjectionPoint.getMember()),
                () -> assertTrue(staticInjectionPoint.getBean().isEmpty()),
                () -> assertEquals(Strings.EMPTY, instanceInjectionPoint.getName()),
                () -> assertEquals(SingletonInstance.class, instanceInjectionPoint.getType()),
                () -> assertEquals(instanceField, instanceInjectionPoint.getElement()),
                () -> assertEquals(instanceField, instanceInjectionPoint.getMember()),
                () -> assertTrue(instanceInjectionPoint.getBean().isEmpty())
        );
    }

    @Test
    void constructorInjectionPoints() throws Exception {
        final Constructor<ConstructedBean> constructor = ConstructedBean.class.getDeclaredConstructor(String.class, String.class, String.class);
        final Collection<InjectionPoint> injectionPoints = beanManager.createExecutableInjectionPoints(constructor, null);
        final List<InjectionPoint> sorted = injectionPoints.stream()
                .sorted(Comparator.comparing(InjectionPoint::getName))
                .collect(Collectors.toList());
        assertEquals(3, sorted.size());
        final InjectionPoint alpha = sorted.get(0);
        final InjectionPoint beta = sorted.get(1);
        final InjectionPoint gamma = sorted.get(2);
        assertAll(
                () -> assertEquals("alpha", alpha.getName()),
                () -> assertEquals("beta", beta.getName()),
                () -> assertEquals("gamma", gamma.getName())
        );
    }

    @Test
    void methodInjectionPoints() throws Exception {
        final Method method = MethodBean.class.getDeclaredMethod("setInstance", SingletonInstance.class);
        final Collection<InjectionPoint> points = beanManager.createExecutableInjectionPoints(method, null);
        assertEquals(1, points.size());
        final InjectionPoint injectionPoint = points.iterator().next();
        assertEquals(Strings.EMPTY, injectionPoint.getName());
    }
}
