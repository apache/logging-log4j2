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
package org.apache.logging.log4j.core.test.junit;

import java.util.function.Consumer;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.test.junit.TypeBasedParameterResolver;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.support.AnnotationSupport;

class LoggerContextResolver extends TypeBasedParameterResolver<LoggerContext> implements BeforeAllCallback,
        BeforeEachCallback, AfterEachCallback {
    private static final Namespace NAMESPACE = Namespace.create(LoggingTestContext.class);

    public LoggerContextResolver() {
        super(LoggerContext.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        AnnotationSupport.findAnnotation(testClass, LoggerContextSource.class)
                .ifPresent(testSource -> initializeTestContext(testSource, context));
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (AnnotationSupport.isAnnotated(context.getRequiredTestClass(), LoggerContextSource.class)) {
            final LoggingTestContext testContext = getTestContext(context);
            testContext.beforeEachTest();
        }
        AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), LoggerContextSource.class)
                .ifPresent(source -> {
                    initializeTestContext(source, context);
                    final LoggingTestContext testContext = getTestContext(context);
                    testContext.beforeEachTest();
                });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        if (AnnotationSupport.isAnnotated(testClass, LoggerContextSource.class)) {
            final LoggingTestContext testContext = getTestContext(context);
            testContext.afterEachTest();
        }
    }

    @Override
    public LoggerContext resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return getLoggerContext(extensionContext);
    }

    private static void initializeTestContext(final LoggerContextSource source, final ExtensionContext extensionContext) {
        final LoggingTestContext context = LoggingTestContext.configurer()
                .setBootstrap(source.bootstrap())
                .setConfigurationLocation(source.value())
                .setContextName(source.name())
                .setReconfigurationPolicy(source.reconfigure())
                .setTimeout(source.timeout(), source.unit())
                .setV1Config(source.v1config())
                .setClassLoader(extensionContext.getRequiredTestClass().getClassLoader())
                .build();
        final Consumer<Injector> configurer = extensionContext.getTestInstance()
                .map(instance -> (Consumer<Injector>) injector -> injector.registerBundle(instance))
                .orElse(null);
        context.init(configurer);
        setTestContext(extensionContext, context);
    }

    static void setTestContext(final ExtensionContext context, final LoggingTestContext testContext) {
        context.getStore(NAMESPACE).put(LoggingTestContext.class, testContext);
    }

    static LoggingTestContext getTestContext(final ExtensionContext context) {
        final LoggingTestContext testContext = context.getStore(NAMESPACE).get(LoggingTestContext.class, LoggingTestContext.class);
        if (testContext == null) {
            throw new ParameterResolutionException("No LoggingTestContext defined");
        }
        return testContext;
    }

    static LoggerContext getLoggerContext(ExtensionContext context) {
        final LoggingTestContext testContext = getTestContext(context);
        final LoggerContext loggerContext = testContext.getLoggerContext();
        if (loggerContext == null) {
            throw new ParameterResolutionException("No LoggerContext defined");
        }
        return loggerContext;
    }

}
