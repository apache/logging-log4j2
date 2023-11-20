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

import java.util.List;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;

/**
 * Base post-processor for registering one or more {@link FactoryResolver} instances to a
 * {@link ConfigurableInstanceFactory}.
 *
 * @see FactoryResolver
 */
public abstract class FactoryResolversPostProcessor implements ConfigurableInstanceFactoryPostProcessor {
    private final List<FactoryResolver<?>> factoryResolvers;

    protected FactoryResolversPostProcessor(final List<FactoryResolver<?>> factoryResolvers) {
        this.factoryResolvers = factoryResolvers;
    }

    @Override
    public final void postProcessFactory(final ConfigurableInstanceFactory factory) {
        factoryResolvers.forEach(factory::registerFactoryResolver);
    }
}
