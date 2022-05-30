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

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.plugins.di.Keys;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;

import static org.apache.logging.log4j.core.test.junit.LoggerContextResolver.getLoggerContext;

class LoggerResolver implements ParameterResolver {
    @Override
    public boolean supportsParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().isAssignableFrom(Logger.class);
    }

    @Override
    public Logger resolveParameter(final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final LoggerContext loggerContext = getLoggerContext(extensionContext);
        if (loggerContext == null) {
            throw new ParameterResolutionException("No LoggerContext defined");
        }
        final String loggerName;
        final Parameter parameter = parameterContext.getParameter();
        if (Keys.hasName(parameter)) {
            loggerName = Keys.getName(parameter);
        } else {
            loggerName = extensionContext.getRequiredTestClass().getCanonicalName();
        }
        return loggerContext.getLogger(loggerName);
    }
}
