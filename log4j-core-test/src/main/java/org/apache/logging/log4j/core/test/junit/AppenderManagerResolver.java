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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.plugins.di.Keys;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.ReflectionSupport;

import java.lang.reflect.Parameter;

import static org.apache.logging.log4j.core.test.junit.LoggerContextResolver.getLoggerContext;

/**
 * Resolves parameters that extend {@link AbstractManager} and have a {@link org.apache.logging.log4j.plugins.Named}
 * parameter of the corresponding appender that uses the manager.
 */
class AppenderManagerResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
        final Parameter parameter = parameterContext.getParameter();
        return AbstractManager.class.isAssignableFrom(parameter.getType()) && Keys.hasName(parameter);
    }

    @Override
    public Object resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext) throws ParameterResolutionException {
        final LoggerContext loggerContext = getLoggerContext(extensionContext);
        if (loggerContext == null) {
            throw new ParameterResolutionException("No LoggerContext defined");
        }
        final Configuration configuration = loggerContext.getConfiguration();
        final Parameter parameter = parameterContext.getParameter();
        final String name = Keys.getName(parameter);
        final Appender appender = configuration.getAppender(name);
        if (appender == null) {
            throw new ParameterResolutionException("No appender named " + name);
        }
        final Class<? extends Appender> appenderClass = appender.getClass();
        final Object manager = ReflectionSupport.findMethod(appenderClass, "getManager")
                .map(method -> ReflectionSupport.invokeMethod(method, appender))
                .orElseThrow(() -> new ParameterResolutionException("Cannot find getManager() on appender " + appenderClass));
        final Class<?> parameterType = parameter.getType();
        if (!parameterType.isInstance(manager)) {
            throw new ParameterResolutionException("Expected type " + parameterType + " but got type " + manager.getClass());
        }
        return manager;
    }
}
