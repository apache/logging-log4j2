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
package org.apache.logging.log4j.core.config;

import java.net.URI;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;

public interface ConfigurationFactory {

    Key<ConfigurationFactory> KEY = new Key<>() {};

    /**
     * Required for Spring Boot.
     * @param props PropertiesUtil.
     * @return the AuthorizationProvider, if any.
     */
    static AuthorizationProvider authorizationProvider(final PropertiesUtil props) {
        return AuthorizationProvider.getAuthorizationProvider((PropertyEnvironment) props);
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    static ConfigurationFactory getInstance() {
        return LoggerContext.getContext(false).getInstanceFactory().getInstance(KEY);
    }

    String getDefaultPrefix();

    String getTestPrefix();

    String[] getSupportedTypes();

    String getVersion();

    Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source);

    Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation);

    Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation, ClassLoader loader);
}
