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

package org.apache.logging.log4j.spi;

import org.apache.logging.log4j.internal.BindingMap;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.apache.logging.log4j.util3.PropertiesUtil;

/**
 * Abstract base class for logging system providers.
 *
 * @param <F> type of LoggerContextFactory this provider uses
 */
public abstract class AbstractLoggingSystemProvider<F extends LoggerContextFactory> implements LoggingSystemProvider {
    private final BindingMap bindings;

    protected AbstractLoggingSystemProvider() {
        bindings = new BindingMap();
        bindings.bind(PropertyEnvironment.class, PropertiesUtil::getProperties)
                .bind(LoggerContextFactory.class, Lazy.lazy(this::createLoggerContextFactory))
                .bind(ThreadContextMap.Factory.class, this::createContextMapFactory)
                .bind(ThreadContextStack.Factory.class, this::createContextStackFactory);
    }

    protected abstract F createLoggerContextFactory();

    protected ThreadContextMap.Factory createContextMapFactory() {
        return new DefaultContextMapFactory(getEnvironment());
    }

    protected ThreadContextStack.Factory createContextStackFactory() {
        return new DefaultContextStackFactory(getEnvironment());
    }

    protected <T> T newInstance(final Class<T> type) {
        return ReflectionUtil.instantiate(type);
    }

    @Override
    public <T> T getInstance(final Class<T> clazz) {
        return bindings.getOrBindSupplier(clazz, () -> newInstance(clazz)).get();
    }
}
