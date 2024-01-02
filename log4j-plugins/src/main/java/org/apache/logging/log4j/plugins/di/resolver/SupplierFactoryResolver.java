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
import java.util.Collection;
import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.spi.FactoryResolver;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;

/**
 * Blueprint for supporting generic injection points or keys with a type {@link Supplier Supplier&lt;T&gt;} for
 * some other type {@code T}.
 */
public class SupplierFactoryResolver<T> implements FactoryResolver<Supplier<T>> {
    @Override
    public boolean supportsKey(final Key<?> key) {
        return key.getType() instanceof ParameterizedType type
                && type.getRawType() == Supplier.class
                && type.getActualTypeArguments().length == 1;
    }

    @Override
    public Supplier<Supplier<T>> getFactory(
            final ResolvableKey<Supplier<T>> resolvableKey, final InstanceFactory instanceFactory) {
        final Key<T> key = resolvableKey.key().getSuppliedType();
        assert key != null; // already checked in supportsKey
        final Collection<String> aliases = resolvableKey.aliases();
        // dependencies ignored as this is a lazy binding
        return () -> instanceFactory.getFactory(key, aliases);
    }
}
