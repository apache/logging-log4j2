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
package org.apache.logging.log4j.core.async;

import java.net.URI;
import java.util.function.Consumer;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerContextNamingStrategy;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.plugins.ContextScoped;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.SimpleScope;
import org.apache.logging.log4j.util.PropertyResolver;

/**
 * {@code ContextSelector} that manages {@code AsyncLoggerContext} instances.
 * <p>
 * As of version 2.5, this class extends ClassLoaderContextSelector for better web app support.
 */
@Singleton
public class AsyncLoggerContextSelector extends ClassLoaderContextSelector {

    @Inject
    public AsyncLoggerContextSelector(final Injector injector, final PropertyResolver resolver,
                                      final LoggerContextNamingStrategy namingStrategy) {
        super(injector, resolver, namingStrategy);
    }

    @Override
    protected LoggerContext createContext(final String key, final String name, final URI configLocation, final Consumer<Injector> configurer) {
        final Injector loggerContextInjector = injector.copy();
        loggerContextInjector.registerScope(ContextScoped.class, new SimpleScope("AsyncLoggerContext; name=" + name));
        if (configurer != null) {
            configurer.accept(loggerContextInjector);
        }
        return AsyncLoggerContext.newBuilder()
                .setKey(key)
                .setName(name)
                .setConfigLocation(configLocation)
                .setInjector(loggerContextInjector)
                .get();
    }

    @Override
    protected String toContextMapKey(final ClassLoader loader) {
        // LOG4J2-666 ensure unique name across separate instances created by webapp classloaders
        return "AsyncContext@" + Integer.toHexString(System.identityHashCode(loader));
    }

    @Override
    protected String defaultContextName() {
        return "DefaultAsyncContext@" + Thread.currentThread().getName();
    }
}
