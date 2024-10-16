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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.plugins.di.Keys;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver;
import org.junit.platform.commons.support.ReflectionSupport;

/**
 * Annotates a test extension annotation to indicate that it sets up Log4j and can resolve parameters of various kinds.
 * These supported types include {@link LoggerContext}, {@link Configuration}, configured {@link Appender} plugins
 * with a {@linkplain org.apache.logging.log4j.plugins.Named name}, named {@link AbstractManager} instances, and
 * named {@link Logger} instances.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Log4jTest
@ExtendWith({
    LoggerContextResolver.class,
    ConfigurationResolver.class,
    AppenderResolver.class,
    AppenderManagerResolver.class,
    LoggerResolver.class
})
public @interface LoggingResolvers {}

/**
 * Handles parameter resolution of a {@link LoggerContext}.
 */
class LoggerContextResolver extends TypeBasedParameterResolver<LoggerContext> {
    @Override
    public LoggerContext resolveParameter(final ParameterContext parameterContext, final ExtensionContext context)
            throws ParameterResolutionException {
        return Log4jExtension.getRequiredLoggerContext(context);
    }
}

/**
 * Handles parameter resolution of a {@link Configuration}.
 */
class ConfigurationResolver extends TypeBasedParameterResolver<Configuration> {
    @Override
    public Configuration resolveParameter(
            final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return Log4jExtension.getRequiredLoggerContext(extensionContext).getConfiguration();
    }
}

/**
 * Resolves parameters that implement {@link Appender} and have a {@link org.apache.logging.log4j.plugins.Named}
 * value of the name of the appender.
 */
class AppenderResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Parameter parameter = parameterContext.getParameter();
        return Appender.class.isAssignableFrom(parameter.getType()) && Keys.hasName(parameter);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final LoggerContext loggerContext = Log4jExtension.getRequiredLoggerContext(extensionContext);
        final String name = Keys.getName(parameterContext.getParameter());
        if (name.isEmpty()) {
            throw new ParameterResolutionException("No named annotation present after checking earlier");
        }
        final Appender appender = loggerContext.getConfiguration().getAppender(name);
        if (appender == null) {
            throw new ParameterResolutionException("No appender named " + name);
        }
        return appender;
    }
}

/**
 * Resolves parameters that extend {@link AbstractManager} and have a {@link org.apache.logging.log4j.plugins.Named}
 * parameter of the corresponding appender that uses the manager.
 */
class AppenderManagerResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Parameter parameter = parameterContext.getParameter();
        return AbstractManager.class.isAssignableFrom(parameter.getType()) && Keys.hasName(parameter);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Configuration configuration =
                Log4jExtension.getRequiredLoggerContext(extensionContext).getConfiguration();
        final Parameter parameter = parameterContext.getParameter();
        final String name = Keys.getName(parameter);
        final Appender appender = configuration.getAppender(name);
        if (appender == null) {
            throw new ParameterResolutionException("No appender named " + name);
        }
        final Class<? extends Appender> appenderClass = appender.getClass();
        final Object manager = ReflectionSupport.findMethod(appenderClass, "getManager")
                .map(method -> ReflectionSupport.invokeMethod(method, appender))
                .orElseThrow(() ->
                        new ParameterResolutionException("Cannot find getManager() on appender " + appenderClass));
        final Class<?> parameterType = parameter.getType();
        if (!parameterType.isInstance(manager)) {
            throw new ParameterResolutionException(
                    "Expected type " + parameterType + " but got type " + manager.getClass());
        }
        return manager;
    }
}

/**
 * Handles parameter resolution for named {@link Logger} instances. These parameters must be either {@link Logger} or
 * a supertype. Parameters must have a {@linkplain org.apache.logging.log4j.plugins.Named naming annotation}.
 */
class LoggerResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().isAssignableFrom(Logger.class);
    }

    @Override
    public Logger resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final Parameter parameter = parameterContext.getParameter();
        final String loggerName = Keys.hasName(parameter)
                ? Keys.getName(parameter)
                : extensionContext.getRequiredTestClass().getCanonicalName();
        return Log4jExtension.getRequiredLoggerContext(extensionContext).getLogger(loggerName);
    }
}
