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

import static org.apache.logging.log4j.core.test.junit.LoggerContextResolver.getParameterLoggerContext;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.test.junit.TypeBasedParameterResolver;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

class ConfigurationResolver extends TypeBasedParameterResolver<Configuration> {

    public ConfigurationResolver() {
        super(Configuration.class);
    }

    @Override
    public Configuration resolveParameter(
            final ParameterContext parameterContext, final ExtensionContext extensionContext)
            throws ParameterResolutionException {
        final LoggerContext loggerContext = getParameterLoggerContext(parameterContext, extensionContext);
        if (loggerContext == null) {
            throw new ParameterResolutionException("No LoggerContext defined");
        }
        return loggerContext.getConfiguration();
    }
}
