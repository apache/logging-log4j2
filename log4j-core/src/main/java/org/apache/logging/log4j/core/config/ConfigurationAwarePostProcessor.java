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

import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.di.spi.InstancePostProcessor;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;

public class ConfigurationAwarePostProcessor implements InstancePostProcessor {
    private final Supplier<? extends Configuration> configurationSupplier;

    public ConfigurationAwarePostProcessor(final Supplier<? extends Configuration> configurationSupplier) {
        this.configurationSupplier = configurationSupplier;
    }

    @Override
    public <T> T postProcessBeforeInitialization(final ResolvableKey<T> resolvableKey, final T instance) {
        if (instance instanceof ConfigurationAware) {
            ((ConfigurationAware) instance).setConfiguration(configurationSupplier.get());
        }
        return instance;
    }
}
