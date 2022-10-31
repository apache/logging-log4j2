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

package org.apache.logging.log4j.core.impl;

import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.spi.DefaultContextMapFactory;
import org.apache.logging.log4j.spi.DefaultContextStackFactory;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.spi.LoggingSystemProvider;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextStack;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util3.PropertiesUtil;

/**
 * Binding for the Log4j API.
 */
public class Log4jSystemProvider implements LoggingSystemProvider {
    private final Injector injector;

    public Log4jSystemProvider() {
        injector = DI.createInjector();
        injector.init();
        injector.registerBundle(new Object() {
            @Factory
            PropertyEnvironment environment() {
                return PropertiesUtil.getProperties();
            }

            @Factory
            ThreadContextMap.Factory contextMapFactory(PropertyEnvironment environment) {
                return new DefaultContextMapFactory(environment);
            }

            @Factory
            ThreadContextStack.Factory contextStackFactory(PropertyEnvironment environment) {
                return new DefaultContextStackFactory(environment);
            }
        });
        injector.registerBinding(Key.forClass(LoggerContextFactory.class), injector.getFactory(Log4jContextFactory.class));
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String getVersion() {
        return "2.6.0";
    }

    @Override
    public <T> T getInstance(final Class<T> clazz) {
        return injector.getInstance(clazz);
    }
}
