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

class LoggerContextResolver extends TypeBasedParameterResolver<LoggerContext> implements BeforeAllCallback,
        AfterAllCallback, BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final LoggerContextSource testSource = testClass.getAnnotation(LoggerContextSource.class);
        if (testSource != null) {
            final LoggerContext loggerContext =
                    Configurator.initialize(context.getDisplayName(), testClass.getClassLoader(), testSource.value());
            getTestClassStore(context).put(LoggerContext.class, loggerContext);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        final LoggerContext loggerContext = getTestClassStore(context).get(LoggerContext.class, LoggerContext.class);
        if (loggerContext != null) {
            loggerContext.close();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        final Class<?> testClass = context.getRequiredTestClass();
        final LoggerContextSource testSource = testClass.getAnnotation(LoggerContextSource.class);
        if (testSource != null && testSource.reconfigure() == ReconfigurationPolicy.BEFORE_EACH) {
            final LoggerContext loggerContext = getTestClassStore(context).get(LoggerContext.class, LoggerContext.class);
            if (loggerContext == null) {
                throw new IllegalStateException(
                        "Specified test class reconfiguration policy of BEFORE_EACH, but no LoggerContext found for test class " +
                                testClass.getCanonicalName());
            }
            loggerContext.reconfigure();
        }
        final LoggerContextSource source = context.getRequiredTestMethod().getAnnotation(LoggerContextSource.class);
        if (source != null) {
            final LoggerContext loggerContext = Configurator
                    .initialize(context.getDisplayName(), testClass.getClassLoader(), source.value());
            getTestInstanceStore(context).put(LoggerContext.class, loggerContext);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        // method-annotated variant
        final LoggerContext testInstanceContext = getTestInstanceStore(context).get(LoggerContext.class, LoggerContext.class);
        if (testInstanceContext != null) {
            testInstanceContext.close();
        }
        // reloadable variant
        final Class<?> testClass = context.getRequiredTestClass();
        final LoggerContextSource source = testClass.getAnnotation(LoggerContextSource.class);
        if (source != null && source.reconfigure() == ReconfigurationPolicy.AFTER_EACH) {
            final LoggerContext loggerContext = getTestClassStore(context).get(LoggerContext.class, LoggerContext.class);
            if (loggerContext == null) {
                throw new IllegalStateException(
                        "Specified test class reconfiguration policy of AFTER_EACH, but no LoggerContext found for test class " +
                                testClass.getCanonicalName());
            }
            loggerContext.reconfigure();
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
            final LoggerContext loggerContext =
                    getTestInstanceStore(extensionContext).get(LoggerContext.class, LoggerContext.class);
            return loggerContext != null ? loggerContext :
                    getTestClassStore(extensionContext).get(LoggerContext.class, LoggerContext.class);
        }
        return getTestClassStore(extensionContext).get(LoggerContext.class, LoggerContext.class);
    }

}
