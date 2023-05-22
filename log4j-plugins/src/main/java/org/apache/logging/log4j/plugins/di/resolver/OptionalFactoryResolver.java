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
package org.apache.logging.log4j.plugins.di.resolver;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.logging.log4j.plugins.PluginException;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.NotInjectableException;
import org.apache.logging.log4j.plugins.di.spi.DependencyChain;
import org.apache.logging.log4j.plugins.di.spi.FactoryResolver;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.status.StatusLogger;

public class OptionalFactoryResolver implements FactoryResolver {
    @Override
    public boolean supportsKey(final Key<?> key) {
        final Type type = key.getType();
        return type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() == Optional.class;
    }

    @Override
    public Supplier<?> getFactory(final ResolvableKey<?> resolvableKey, final InstanceFactory instanceFactory) {
        final Key<?> key = resolvableKey.getKey();
        final Key<?> itemKey = key.getParameterizedTypeArgument(0);
        final Collection<String> aliases = resolvableKey.getAliases();
        final DependencyChain dependencyChain = resolvableKey.getDependencyChain();
        if (itemKey == null) {
            throw new NotInjectableException(resolvableKey);
        }
        return () -> {
            try {
                return Optional.ofNullable(instanceFactory.getInstance(itemKey, aliases, dependencyChain));
            } catch (final PluginException e) {
                StatusLogger.getLogger().debug("Error while getting instance for {} with dependencies {}",
                        itemKey, dependencyChain, e);
                return Optional.empty();
            }
        };
    }
}
