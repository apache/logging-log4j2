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

package org.apache.logging.log4j.plugins.test;

import org.apache.logging.log4j.plugins.defaults.bean.DefaultBeanManager;
import org.apache.logging.log4j.plugins.defaults.bean.DefaultInjectionFactory;
import org.apache.logging.log4j.plugins.defaults.model.DefaultElementManager;
import org.apache.logging.log4j.plugins.spi.InjectionException;
import org.apache.logging.log4j.plugins.spi.UnsatisfiedBeanException;
import org.apache.logging.log4j.plugins.spi.bean.Bean;
import org.apache.logging.log4j.plugins.spi.bean.BeanManager;
import org.apache.logging.log4j.plugins.spi.bean.InjectionFactory;
import org.apache.logging.log4j.plugins.spi.model.ElementManager;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaClass;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;
import org.apache.logging.log4j.plugins.spi.bean.InitializationContext;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.junit.Test;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * JUnit 4 test runner that integrates with {@link BeanManager} to create test instances and inject test parameters.
 * Beans to load can be specified with the {@link WithBeans} annotation on the class or method. The test class itself
 * must be a bean. Each test method creates a new BeanManager, loads beans annotated on the test class, creates a
 * test instance, loads beans annotated on the test method, then injects values into the test method parameters, invoking
 * that method.
 */
public class BeanJUnit4Runner extends BlockJUnit4ClassRunner {
    private ElementManager elementManager;
    private MetaClass<?> testMetaClass;

    private BeanManager beanManager;
    private InjectionFactory injectionFactory;
    private Bean<?> testClassBean;
    private InitializationContext<?> initializationContext;

    public BeanJUnit4Runner(final Class<?> clazz) throws InitializationError {
        super(clazz);
    }

    @Override
    protected void collectInitializationErrors(final List<Throwable> errors) {
        // this must come before call to super to allow use in validateConstructor()
        elementManager = new DefaultElementManager();
        testMetaClass = elementManager.getMetaClass(getTestClass().getJavaClass());
        super.collectInitializationErrors(errors);
    }

    @Override
    protected void validateConstructor(final List<Throwable> errors) {
        if (!elementManager.isInjectable(testMetaClass)) {
            errors.add(new InjectionException(testMetaClass + " does not have any injectable constructors"));
        }
    }

    @Override
    protected void validateTestMethods(final List<Throwable> errors) {
        for (final FrameworkMethod method : getTestClass().getAnnotatedMethods(Test.class)) {
            method.validatePublicVoid(false, errors);
        }
    }

    @Override
    protected Object createTest() throws Exception {
        return createTestInstance();
    }

    private <T> T createTestInstance() {
        beanManager = new DefaultBeanManager(elementManager);
        injectionFactory = new DefaultInjectionFactory(beanManager);
        final WithBeans testClassBeans = getTestClass().getAnnotation(WithBeans.class);
        if (testClassBeans != null) {
            beanManager.loadBeans(testClassBeans.value());
        }
        final Class<T> testClass = TypeUtil.cast(getTestClass().getJavaClass());
        final Optional<Bean<T>> testBean = beanManager.loadBeans(testClass).stream()
                .filter(bean -> bean.hasMatchingType(testClass))
                .findAny()
                .map(TypeUtil::cast);
        final Bean<T> testClassBean = testBean
                .orElseThrow(() -> new UnsatisfiedBeanException(testClass));
        this.testClassBean = testClassBean;
        // FIXME: should we use a null bean or not? I'm still confused by the init context thing
        initializationContext = beanManager.createInitializationContext(testClassBean);
        return beanManager.getValue(testClassBean, initializationContext);
    }

    @Override
    protected Statement methodInvoker(final FrameworkMethod method, final Object test) {
        return genericMethodInvoker(method, test);
    }

    private <T> Statement genericMethodInvoker(final FrameworkMethod method, final T testInstance) {
        final Bean<T> testClassBean = TypeUtil.cast(this.testClassBean);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (final BeanManager manager = beanManager;
                     final InitializationContext<T> context = TypeUtil.cast(initializationContext)) {
                    beanManager = null;
                    initializationContext = null;
                    final WithBeans methodBeans = method.getAnnotation(WithBeans.class);
                    if (methodBeans != null) {
                        manager.loadBeans(methodBeans.value());
                    }
                    final Class<T> testClass = TypeUtil.cast(getTestClass().getJavaClass());
                    final MetaClass<T> metaClass = elementManager.getMetaClass(testClass);
                    final MetaMethod<T, Void> metaMethod = metaClass.getMetaMethod(method.getMethod());
                    final Collection<InjectionPoint<?>> points =
                            elementManager.createExecutableInjectionPoints(metaMethod, testClassBean);
                    injectionFactory.forMethod(metaMethod, testInstance, points).accept(context);
                }
            }
        };
    }
}
