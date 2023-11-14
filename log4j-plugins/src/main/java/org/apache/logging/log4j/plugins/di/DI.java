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
package org.apache.logging.log4j.plugins.di;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;

import org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor;
import org.apache.logging.log4j.plugins.util.OrderedComparator;
import org.apache.logging.log4j.util.ServiceRegistry;

/**
 * Factory for {@linkplain InstanceFactory instance factories}.
 */
public final class DI {
    private DI() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a new {@linkplain #initializeFactory(ConfigurableInstanceFactory) initialized} instance factory.
     *
     * @return the initialized instance factory
     */
    public static ConfigurableInstanceFactory createInitializedFactory() {
        final var factory = createFactory();
        initializeFactory(factory);
        return factory;
    }

    /**
     * Creates a new instance factory with the provided initial bindings and subsequently
     * {@linkplain #initializeFactory(ConfigurableInstanceFactory) initializes} it.
     *
     * @param bindings the bindings to register before initializing the factory
     * @return the initialized instance factory
     */
    public static ConfigurableInstanceFactory createInitializedFactory(final Binding<?>... bindings) {
        final var factory = createFactory();
        for (final Binding<?> binding : bindings) {
            factory.registerBinding(binding);
        }
        initializeFactory(factory);
        return factory;
    }

    /**
     * Creates a new instance factory. This should be {@linkplain #initializeFactory(ConfigurableInstanceFactory)
     * initialized} after setup.
     *
     * @return a new instance factory
     */
    public static ConfigurableInstanceFactory createFactory() {
        return new DefaultInstanceFactory();
    }

    /**
     * Initializes the given instance factory with all registered {@link ConfigurableInstanceFactoryPostProcessor}
     * services.
     *
     * @param factory the instance factory to initialize
     */
    public static void initializeFactory(final ConfigurableInstanceFactory factory) {
        ServiceRegistry.getInstance()
                .getServices(ConfigurableInstanceFactoryPostProcessor.class, MethodHandles.lookup(), null)
                .stream()
                .sorted(Comparator.comparing(ConfigurableInstanceFactoryPostProcessor::getClass, OrderedComparator.INSTANCE))
                .forEachOrdered(processor -> processor.postProcessFactory(factory));
    }
}
