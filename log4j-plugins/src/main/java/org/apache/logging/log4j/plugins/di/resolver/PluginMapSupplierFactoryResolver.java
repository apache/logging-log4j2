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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.util.TypeUtil;

public class PluginMapSupplierFactoryResolver<T>
        extends AbstractPluginFactoryResolver<Map<String, ? extends Supplier<? extends T>>> {
    @Override
    protected boolean supportsType(final Type rawType, final Type... typeArguments) {
        return rawType == Map.class
                && typeArguments.length == 2
                && typeArguments[0] == String.class
                && TypeUtil.isAssignable(Supplier.class, typeArguments[1])
                && typeArguments[1] instanceof ParameterizedType
                && ((ParameterizedType) typeArguments[1]).getActualTypeArguments().length == 1;
    }

    @Override
    public Supplier<Map<String, ? extends Supplier<? extends T>>> getFactory(
            final ResolvableKey<Map<String, ? extends Supplier<? extends T>>> resolvableKey,
            final InstanceFactory instanceFactory) {
        final String namespace = resolvableKey.namespace();
        final ParameterizedType mapType = (ParameterizedType) resolvableKey.type();
        final Type componentType = mapType.getActualTypeArguments()[1];
        final ParameterizedType parameterizedType = (ParameterizedType) componentType;
        final Type[] typeArguments = parameterizedType.getActualTypeArguments();
        final Type suppliedType = typeArguments[0];
        return () -> Plugins.<T>streamPluginTypesMatching(instanceFactory, namespace, suppliedType)
                .collect(Collectors.toMap(
                        PluginType::getKey,
                        pluginType -> instanceFactory.getFactory(Plugins.pluginKey(pluginType)),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }
}
