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

package org.apache.logging.log4j.junit;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.LoggerContextAccessor;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

class LoggerContextResolver extends TypeBasedParameterResolver<LoggerContext> implements BeforeAllCallback,
        AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final LoggerContextSource testSource = testClass.getAnnotation(LoggerContextSource.class);
        if (testSource != null) {
            final LoggerContextConfig config = new LoggerContextConfig(testSource, context);
            getTestClassStore(context).put(LoggerContext.class, config);
        }
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
        if (testClass.isAnnotationPresent(LoggerContextSource.class)) {
            final LoggerContextConfig config = getTestClassStore(context).get(LoggerContext.class, LoggerContextConfig.class);
            if (config == null) {
                throw new IllegalStateException(
                        "Specified @LoggerContextSource but no LoggerContext found for test class " +
                                testClass.getCanonicalName());
            }
            if (config.reconfigurationPolicy == ReconfigurationPolicy.BEFORE_EACH) {
                config.reconfigure();
            }
        }
        final LoggerContextSource source = context.getRequiredTestMethod().getAnnotation(LoggerContextSource.class);
        if (source != null) {
            final LoggerContextConfig config = new LoggerContextConfig(source, context);
            if (config.reconfigurationPolicy == ReconfigurationPolicy.BEFORE_EACH) {
                config.reconfigure();
            }
            getTestInstanceStore(context).put(LoggerContext.class, config);
        }
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
        if (testClass.isAnnotationPresent(LoggerContextSource.class)) {
            final LoggerContextConfig config = getTestClassStore(context).get(LoggerContext.class, LoggerContextConfig.class);
            if (config == null) {
                throw new IllegalStateException(
                        "Specified @LoggerContextSource but no LoggerContext found for test class " +
                                testClass.getCanonicalName());
            }
            if (config.reconfigurationPolicy == ReconfigurationPolicy.AFTER_EACH) {
                config.reconfigure();
            }
        }
    }

    @Override
    public LoggerContext resolveParameter(
            ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return getParameterLoggerContext(parameterContext, extensionContext);
    }

    private static ExtensionContext.Store getTestClassStore(final ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(LoggerContext.class, context.getRequiredTestClass()));
    }

    private static ExtensionContext.Store getTestInstanceStore(final ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(LoggerContext.class, context.getRequiredTestInstance()));
    }

    static LoggerContext getParameterLoggerContext(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (parameterContext.getDeclaringExecutable() instanceof Method) {
            final LoggerContextAccessor accessor =
                    getTestInstanceStore(extensionContext).get(LoggerContext.class, LoggerContextAccessor.class);
            return accessor != null ? accessor.getLoggerContext() :
                    getTestClassStore(extensionContext).get(LoggerContext.class, LoggerContextAccessor.class).getLoggerContext();
        }
        return getTestClassStore(extensionContext).get(LoggerContext.class, LoggerContextAccessor.class).getLoggerContext();
    }

    private static class LoggerContextConfig implements AutoCloseable, LoggerContextAccessor {
        private final LoggerContext context;
        private final ReconfigurationPolicy reconfigurationPolicy;
        private final long shutdownTimeout;
        private final TimeUnit unit;

        private LoggerContextConfig(final LoggerContextSource source, final ExtensionContext extensionContext) {
            final String displayName = extensionContext.getDisplayName();
            final ClassLoader classLoader = extensionContext.getRequiredTestClass().getClassLoader();
            context = Configurator.initialize(displayName, classLoader, source.value());
            reconfigurationPolicy = source.reconfigure();
            shutdownTimeout = source.timeout();
            unit = source.unit();
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
