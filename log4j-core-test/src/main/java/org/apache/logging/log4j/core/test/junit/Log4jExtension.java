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
package org.apache.logging.log4j.core.test.junit;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.Provider;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * Test extension for setting up various Log4j subsystems. This extension is enabled by using one or more
 * test extension annotations that have been annotated with {@link Log4jTest}. When Log4j test extensions are
 * present on test classes, then these extensions apply to all tests within the class and are set up before
 * all tests (via {@link BeforeAllCallback}). When Log4j test extensions are present on test methods, then
 * these extensions apply to the individually-annotated tests.
 *
 * @see LoggerContextSource
 * @see LegacyLoggerContextSource
 * @see Log4jTest
 * @see LoggingResolvers
 */
class Log4jExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
    private static final String FQCN = Log4jExtension.class.getName();
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(Log4jExtension.class);
    private static final List<String> EXTENSIONS = List.of(".xml", ".json", ".properties");

    @Override
    public void beforeAll(final ExtensionContext context) {
        final Class<?> testClass = context.getRequiredTestClass();
        if (AnnotationSupport.isAnnotated(testClass, Log4jTest.class)) {
            final ExtensionContext.Store store = context.getStore(NAMESPACE);
            final DI.FactoryBuilder builder = store.getOrComputeIfAbsent(DI.FactoryBuilder.class);
            configure(builder, store, testClass, testClass);
        }
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        final Class<?> testClass = context.getRequiredTestClass();
        final Method testMethod = context.getRequiredTestMethod();
        final ExtensionContext.Store store = context.getStore(NAMESPACE);
        if (AnnotationSupport.isAnnotated(testMethod, Log4jTest.class)) {
            final DI.FactoryBuilder builder;
            if (AnnotationSupport.isAnnotated(testClass, Log4jTest.class)) {
                builder = store.get(DI.FactoryBuilder.class, DI.FactoryBuilder.class)
                        .copy();
            } else {
                builder = store.getOrComputeIfAbsent(DI.FactoryBuilder.class);
            }
            configure(builder, store, testMethod, testClass);
        }
        AnnotationSupport.findAnnotation(testClass, LoggerContextSource.class)
                .map(LoggerContextSource::reconfigure)
                .filter(ReconfigurationPolicy.BEFORE_EACH::equals)
                .ifPresent(ignored -> getRequiredLoggerContext(store).reconfigure());
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        AnnotationSupport.findAnnotation(context.getTestClass(), LoggerContextSource.class)
                .map(LoggerContextSource::reconfigure)
                .filter(ReconfigurationPolicy.AFTER_EACH::equals)
                .ifPresent(ignored -> getRequiredLoggerContext(context).reconfigure());
    }

    private static void configure(
            final DI.FactoryBuilder builder,
            final ExtensionContext.Store store,
            final AnnotatedElement element,
            final Class<?> testClass) {
        final ConfigurableInstanceFactory instanceFactory = configureInstanceFactory(builder, element);
        final Provider provider = instanceFactory.getInstance(Provider.class);
        final Log4jContextFactory factory = (Log4jContextFactory) provider.getLoggerContextFactory();
        store.put(LoggerContextFactoryHolder.class, new LoggerContextFactoryHolder(factory));
        if (AnnotationSupport.isAnnotated(element, LoggingResolvers.class)) {
            AnnotationSupport.findAnnotation(element, LoggerContextSource.class)
                    .map(source -> configureLoggerContextSource(source, testClass, factory))
                    .or(() -> AnnotationSupport.findAnnotation(element, LegacyLoggerContextSource.class)
                            .map(source -> configureLegacyLoggerContextSource(source, testClass, factory)))
                    .ifPresent(p -> store.put(LoggerContextProvider.class, p));
        }
    }

    private static ConfigurableInstanceFactory configureInstanceFactory(
            final DI.FactoryBuilder builder, final AnnotatedElement element) {
        AnnotationSupport.findRepeatableAnnotations(element, TestBinding.class)
                .forEach(testBinding -> registerTestBinding(builder, testBinding));
        AnnotationSupport.findAnnotation(element, ContextSelectorType.class)
                .map(ContextSelectorType::value)
                .ifPresent(clazz -> builder.addInitialBindingFrom(ContextSelector.KEY)
                        .toFunction(instanceFactory -> instanceFactory.getFactory(clazz)));
        AnnotationSupport.findAnnotation(element, ConfigurationFactoryType.class)
                .map(ConfigurationFactoryType::value)
                .ifPresent(clazz -> builder.addBindingFrom(AbstractConfigurationFactory.KEY)
                        .toFunction(instanceFactory -> instanceFactory.getFactory(clazz)));
        return builder.build();
    }

    private static void registerTestBinding(final DI.FactoryBuilder builder, final TestBinding testBinding) {
        final Class<?> apiClass = testBinding.api();
        final Class<?> implementation = testBinding.implementation();
        final String implementationClassName = testBinding.implementationClassName();
        final Class<?> implementationClass;
        if (!implementationClassName.isEmpty()) {
            implementationClass = ReflectionSupport.tryToLoadClass(implementationClassName)
                    .getOrThrow(e -> new IllegalArgumentException(
                            String.format(
                                    "Unable to configure test binding apiClassName=%s, implementationClassName=%s",
                                    apiClass, implementationClassName),
                            e));
        } else {
            implementationClass = implementation;
        }
        register(builder, apiClass, implementationClass);
    }

    private static <T> void register(
            final DI.FactoryBuilder builder, final Class<T> apiClass, final Class<?> implementationClass) {
        final Class<? extends T> implementation = implementationClass.asSubclass(apiClass);
        builder.addInitialBindingFrom(apiClass)
                .toFunction(instanceFactory -> instanceFactory.getFactory(implementation));
    }

    private static LoggerContextProvider configureLoggerContextSource(
            final LoggerContextSource source, final Class<?> testClass, final Log4jContextFactory factory) {
        final String configLocation = source.value();
        final URI configUri;
        if (configLocation.isEmpty()) {
            final URL configUrl = findTestConfiguration(testClass);
            if (configUrl != null) {
                try {
                    configUri = configUrl.toURI();
                } catch (final URISyntaxException e) {
                    throw new ExtensionConfigurationException("Invalid configuration location", e);
                }
            } else {
                configUri = null;
            }
        } else {
            configUri = NetUtils.toURI(configLocation);
        }
        final LoggerContext loggerContext =
                factory.getContext(FQCN, testClass.getClassLoader(), null, false, configUri, testClass.getSimpleName());
        if (loggerContext == null) {
            throw new ExtensionConfigurationException("Unable to set up LoggerContext from config URI " + configUri);
        }
        return new LoggerContextResource(loggerContext, source.timeout(), source.unit());
    }

    private static LoggerContextProvider configureLegacyLoggerContextSource(
            final LegacyLoggerContextSource source, final Class<?> testClass, final Log4jContextFactory factory) {
        final String configLocation = source.value();
        System.setProperty(Log4jPropertyKey.CONFIG_V1_FILE_NAME.getSystemKey(), configLocation);
        System.setProperty(Log4jPropertyKey.CONFIG_V1_COMPATIBILITY_ENABLED.getSystemKey(), "true");
        final String contextName = testClass.getSimpleName();
        final LoggerContext context =
                factory.getContext(FQCN, testClass.getClassLoader(), null, false, (URI) null, contextName);
        if (context == null) {
            throw new ExtensionConfigurationException(
                    "Unable to set up LoggerContext from v1 config location " + configLocation);
        }
        final Runnable cleaner = () -> {
            context.close();
            System.clearProperty(Log4jPropertyKey.CONFIG_V1_FILE_NAME.getSystemKey());
            System.clearProperty(Log4jPropertyKey.CONFIG_V1_COMPATIBILITY_ENABLED.getSystemKey());
        };
        return new LoggerContextResource(context, cleaner);
    }

    private static URL findTestConfiguration(final Class<?> testClass) {
        for (Class<?> clazz = testClass; clazz != null; clazz = clazz.getSuperclass()) {
            final List<String> baseFileNames =
                    List.of(clazz.getSimpleName(), clazz.getName().replaceAll("[.$]", "/"));
            for (final String baseFileName : baseFileNames) {
                for (final String extension : EXTENSIONS) {
                    final URL url = clazz.getResource(baseFileName + extension);
                    if (url != null) {
                        return url;
                    }
                }
            }
        }
        return null;
    }

    static LoggerContext getRequiredLoggerContext(final ExtensionContext context) {
        return getRequiredLoggerContext(context.getStore(NAMESPACE));
    }

    private static LoggerContext getRequiredLoggerContext(final ExtensionContext.Store store) {
        final LoggerContextProvider provider = store.get(LoggerContextProvider.class, LoggerContextProvider.class);
        if (provider == null) {
            throw new PreconditionViolationException("Unable to find instance of " + LoggerContextProvider.class);
        }
        return provider.loggerContext();
    }

    private record LoggerContextFactoryHolder(LoggerContextFactory factory)
            implements ExtensionContext.Store.CloseableResource {
        LoggerContextFactoryHolder {
            LogManager.setFactory(factory);
        }

        @Override
        public void close() {
            LogManager.setFactory(null);
        }
    }

    private record LoggerContextResource(LoggerContext loggerContext, Runnable cleaner)
            implements LoggerContextProvider, ExtensionContext.Store.CloseableResource {
        LoggerContextResource(LoggerContext loggerContext, long shutdownTimeout, TimeUnit unit) {
            this(loggerContext, () -> loggerContext.stop(shutdownTimeout, unit));
        }

        @Override
        public void close() {
            cleaner.run();
        }
    }
}
