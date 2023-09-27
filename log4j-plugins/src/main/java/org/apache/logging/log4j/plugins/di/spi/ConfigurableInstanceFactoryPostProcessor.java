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
package org.apache.logging.log4j.plugins.di.spi;

import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.util.OrderedComparator;
import org.apache.logging.log4j.util.ServiceRegistry;

/**
 * Provides post-processing capabilities to the initialization of a {@link ConfigurableInstanceFactory}.
 * Implementations should be registered as {@link ServiceLoader} services for this interface and may include an
 * {@link Ordered} annotation on the class for overriding the order it will be invoked.
 */
@FunctionalInterface
public interface ConfigurableInstanceFactoryPostProcessor {

    /**
     * Runs post-processing on the provided factory.
     *
     * @param factory the instance factory to post-process
     */
    void postProcessFactory(final ConfigurableInstanceFactory factory);

    /**
     * Gets the list of registered post processor services and sorts them using {@link OrderedComparator}.
     *
     * @return list of post processor services
     */
    static List<ConfigurableInstanceFactoryPostProcessor> getPostProcessors() {
        final List<ConfigurableInstanceFactoryPostProcessor> services = ServiceRegistry.getInstance()
                .getServices(ConfigurableInstanceFactoryPostProcessor.class, MethodHandles.lookup(), null);
        services.sort(Comparator.comparing(ConfigurableInstanceFactoryPostProcessor::getClass, OrderedComparator.INSTANCE));
        return services;
    }
}
