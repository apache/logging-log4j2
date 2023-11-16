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

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggerContextAccessor;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.test.junit.TypeBasedParameterResolver;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.commons.support.AnnotationSupport;

class LoggerContextResolver extends TypeBasedParameterResolver<LoggerContext>
        implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    public LoggerContextResolver() {
        super(LoggerContext.class);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        AnnotationSupport.findAnnotation(testClass, LoggerContextSource.class).ifPresent(testSource -> {
            final LoggerContextConfig config = new LoggerContextConfig(testSource, context);
            getTestClassStore(context).put(LoggerContext.class, config);
        });
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        final LoggerContextConfig config =
                getTestClassStore(context).get(LoggerContext.class, LoggerContextConfig.class);
        if (config != null) {
            config.close();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        if (AnnotationSupport.isAnnotated(testClass, LoggerContextSource.class)) {
            final LoggerContextConfig config =
                    getTestClassStore(context).get(LoggerContext.class, LoggerContextConfig.class);
            if (config == null) {
                throw new IllegalStateException(
                        "Specified @LoggerContextSource but no LoggerContext found for test class "
                                + testClass.getCanonicalName());
            }
            if (config.reconfigurationPolicy == ReconfigurationPolicy.BEFORE_EACH) {
                config.reconfigure();
            }
        }
        AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), LoggerContextSource.class)
                .ifPresent(source -> {
                    final LoggerContextConfig config = new LoggerContextConfig(source, context);
                    if (config.reconfigurationPolicy == ReconfigurationPolicy.BEFORE_EACH) {
                        config.reconfigure();
                    }
                    getTestInstanceStore(context).put(LoggerContext.class, config);
                });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // method-annotated variant
        final LoggerContextConfig testInstanceConfig =
                getTestInstanceStore(context).get(LoggerContext.class, LoggerContextConfig.class);
        if (testInstanceConfig != null) {
            testInstanceConfig.close();
        }
        // reloadable variant
        final Class<?> testClass = context.getRequiredTestClass();
        if (AnnotationSupport.isAnnotated(testClass, LoggerContextSource.class)) {
            final LoggerContextConfig config =
                    getTestClassStore(context).get(LoggerContext.class, LoggerContextConfig.class);
            if (config == null) {
                throw new IllegalStateException(
                        "Specified @LoggerContextSource but no LoggerContext found for test class "
                                + testClass.getCanonicalName());
            }
            if (config.reconfigurationPolicy == ReconfigurationPolicy.AFTER_EACH) {
                config.reconfigure();
            }
        }
    }

    @Override
    public LoggerContext resolveParameter(
            final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return getParameterLoggerContext(parameterContext, extensionContext);
    }

    private static ExtensionContext.Store getTestClassStore(final ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(LoggerContext.class, context.getRequiredTestClass()));
    }

    private static ExtensionContext.Store getTestInstanceStore(final ExtensionContext context) {
        return context.getStore(
                ExtensionContext.Namespace.create(LoggerContext.class, context.getRequiredTestInstance()));
    }

    static LoggerContext getParameterLoggerContext(
            final ParameterContext parameterContext, final ExtensionContext extensionContext) {
        if (parameterContext.getDeclaringExecutable() instanceof Method) {
            final LoggerContextAccessor accessor =
                    getTestInstanceStore(extensionContext).get(LoggerContext.class, LoggerContextAccessor.class);
            return accessor != null
                    ? accessor.getLoggerContext()
                    : getTestClassStore(extensionContext)
                            .get(LoggerContext.class, LoggerContextAccessor.class)
                            .getLoggerContext();
        }
        return getTestClassStore(extensionContext)
                .get(LoggerContext.class, LoggerContextAccessor.class)
                .getLoggerContext();
    }

    private static final class LoggerContextConfig implements AutoCloseable, LoggerContextAccessor {
        private final LoggerContext context;
        private final ReconfigurationPolicy reconfigurationPolicy;
        private final long shutdownTimeout;
        private final TimeUnit unit;

        private LoggerContextConfig(final LoggerContextSource source, final ExtensionContext extensionContext) {
            final String displayName = extensionContext.getDisplayName();
            final ClassLoader classLoader =
                    extensionContext.getRequiredTestClass().getClassLoader();
            context = Configurator.initialize(displayName, classLoader, getConfigLocation(source, extensionContext));
            reconfigurationPolicy = source.reconfigure();
            shutdownTimeout = source.timeout();
            unit = source.unit();
        }

        private static String getConfigLocation(
                final LoggerContextSource source, final ExtensionContext extensionContext) {
            final String value = source.value();
            if (value.isEmpty()) {
                Class<?> clazz = extensionContext.getRequiredTestClass();
                while (clazz != null) {
                    final URL url = clazz.getResource(clazz.getSimpleName() + ".xml");
                    if (url != null) {
                        try {
                            return url.toURI().toString();
                        } catch (URISyntaxException e) {
                            throw new ExtensionContextException("An error occurred accessing the configuration.", e);
                        }
                    }
                    clazz = clazz.getSuperclass();
                }
                return extensionContext.getRequiredTestClass().getName().replaceAll("[.$]", "/") + ".xml";
            }
            return value;
        }

        @Override
        public LoggerContext getLoggerContext() {
            return context;
        }

        public void reconfigure() {
            context.reconfigure();
        }

        @Override
        public void close() {
            context.stop(shutdownTimeout, unit);
        }
    }
}
