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
package org.apache.logging.log4j.core.async;

import java.net.URI;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;

/**
 * {@code ContextSelector} that manages {@code AsyncLoggerContext} instances.
 * <p>
 * As of version 2.5, this class extends ClassLoaderContextSelector for better web app support.
 */
@Singleton
public class AsyncLoggerContextSelector extends ClassLoaderContextSelector {

    @Inject
    public AsyncLoggerContextSelector(final ConfigurableInstanceFactory instanceFactory) {
        super(instanceFactory);
    }

    @Override
    protected LoggerContext createContext(
            final String name, final URI configLocation, final ConfigurableInstanceFactory instanceFactory) {
        return new AsyncLoggerContext(name, null, configLocation, instanceFactory);
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
