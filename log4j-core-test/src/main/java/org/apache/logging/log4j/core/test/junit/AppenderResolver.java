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

import java.lang.reflect.Parameter;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.plugins.di.Keys;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

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
