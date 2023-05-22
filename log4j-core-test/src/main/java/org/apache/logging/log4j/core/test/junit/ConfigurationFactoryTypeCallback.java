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

import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.plugins.di.Binding;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.spi.LoggingSystem;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

class ConfigurationFactoryTypeCallback implements BeforeAllCallback, AfterAllCallback {
    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        AnnotationSupport.findAnnotation(context.getTestClass(), ConfigurationFactoryType.class)
                .map(ConfigurationFactoryType::value)
                .ifPresent(configurationFactoryType -> {
                    final ConfigurableInstanceFactory factory = DI.createInitializedFactory();
                    factory.registerBinding(Binding.from(ConfigurationFactory.KEY).to(factory.getFactory(configurationFactoryType)));
                    final Log4jContextFactory contextFactory = factory.getInstance(Log4jContextFactory.class);
                    LoggingSystem.getInstance().setLoggerContextFactory(contextFactory);
                });
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        if (AnnotationSupport.isAnnotated(context.getTestClass(), ConfigurationFactoryType.class)) {
            LoggingSystem.getInstance().setLoggerContextFactory(null);
        }
    }
}
