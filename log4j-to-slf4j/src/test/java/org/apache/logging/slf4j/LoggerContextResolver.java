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
package org.apache.logging.slf4j;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import java.net.URL;
import org.apache.logging.log4j.test.junit.ExtensionContextAnchor;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContextException;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.slf4j.LoggerFactory;

class LoggerContextResolver extends TypeBasedParameterResolver<LoggerContext>
        implements BeforeAllCallback, BeforeEachCallback {

    private static final Object KEY = LoggerContextHolder.class;

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        final Class<?> testClass = extensionContext.getRequiredTestClass();
        if (AnnotationSupport.isAnnotated(testClass, LoggerContextSource.class)) {
            final LoggerContextHolder holder =
                    ExtensionContextAnchor.getAttribute(KEY, LoggerContextHolder.class, extensionContext);
            if (holder == null) {
                throw new IllegalStateException(
                        "Specified @LoggerContextSource but no LoggerContext found for test class "
                                + testClass.getCanonicalName());
            }
        }
        AnnotationSupport.findAnnotation(extensionContext.getRequiredTestMethod(), LoggerContextSource.class)
                .ifPresent(source -> {
                    final LoggerContextHolder holder = new LoggerContextHolder(source, extensionContext);
                    ExtensionContextAnchor.setAttribute(KEY, holder, extensionContext);
                });
        final LoggerContextHolder holder =
                ExtensionContextAnchor.getAttribute(KEY, LoggerContextHolder.class, extensionContext);
        if (holder != null) {
            ReflectionSupport.findFields(
                            extensionContext.getRequiredTestClass(),
                            f -> ModifierSupport.isNotStatic(f) && f.getType().equals(LoggerContext.class),
                            HierarchyTraversalMode.TOP_DOWN)
                    .forEach(f -> {
                        try {
                            f.setAccessible(true);
                            f.set(extensionContext.getRequiredTestInstance(), holder.getLoggerContext());
                        } catch (ReflectiveOperationException e) {
                            throw new ExtensionContextException("Failed to inject field " + f, e);
                        }
                    });
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        final Class<?> testClass = extensionContext.getRequiredTestClass();
        AnnotationSupport.findAnnotation(testClass, LoggerContextSource.class).ifPresent(testSource -> {
            final LoggerContextHolder holder = new LoggerContextHolder(testSource, extensionContext);
            ExtensionContextAnchor.setAttribute(KEY, holder, extensionContext);
        });
        final LoggerContextHolder holder =
                ExtensionContextAnchor.getAttribute(KEY, LoggerContextHolder.class, extensionContext);
        if (holder != null) {
            ReflectionSupport.findFields(
                            extensionContext.getRequiredTestClass(),
                            f -> ModifierSupport.isStatic(f) && f.getType().equals(LoggerContext.class),
                            HierarchyTraversalMode.TOP_DOWN)
                    .forEach(f -> {
                        try {
                            f.setAccessible(true);
                            f.set(null, holder.getLoggerContext());
                        } catch (ReflectiveOperationException e) {
                            throw new ExtensionContextException("Failed to inject field " + f, e);
                        }
                    });
        }
    }

    @Override
    public LoggerContext resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return ExtensionContextAnchor.getAttribute(KEY, LoggerContextHolder.class, extensionContext)
                .getLoggerContext();
    }

    static final class LoggerContextHolder implements Store.CloseableResource {

        private final LoggerContext context;
        private final Logger logger;

        private LoggerContextHolder(final LoggerContextSource source, final ExtensionContext extensionContext) {
            this.context = (LoggerContext) LoggerFactory.getILoggerFactory();
            Class<?> clazz = extensionContext.getRequiredTestClass();
            this.logger = context.getLogger(clazz);

            final JoranConfigurator configurator = new JoranConfigurator();
            final URL configLocation = getConfigLocation(source, extensionContext);
            configurator.setContext(context);
            try {
                configurator.doConfigure(configLocation);
            } catch (final JoranException e) {
                throw new ExtensionContextException("Failed to initialize Logback logger context for " + clazz, e);
            }
        }

        private static URL getConfigLocation(
                final LoggerContextSource source, final ExtensionContext extensionContext) {
            final String value = source.value();
            Class<?> clazz = extensionContext.getRequiredTestClass();
            URL url = null;
            if (value.isEmpty()) {
                while (clazz != null) {
                    url = clazz.getResource(clazz.getSimpleName() + ".xml");
                    if (url != null) {
                        break;
                    }
                    clazz = clazz.getSuperclass();
                }
            } else {
                url = clazz.getClassLoader().getResource(value);
            }
            if (url != null) {
                return url;
            }
            throw new ExtensionContextException("Failed to find a default configuration for " + clazz);
        }

        public LoggerContext getLoggerContext() {
            return context;
        }

        public Logger getLogger() {
            return logger;
        }

        @Override
        public void close() {
            context.stop();
        }
    }
}
