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
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.plugins.model.PluginType;

public class PluginStreamPluginTypeFactoryResolver<T>
        extends AbstractPluginFactoryResolver<Stream<? extends PluginType<? extends T>>> {
    @Override
    protected boolean supportsType(final Type rawType, final Type... typeArguments) {
        if (rawType != Stream.class) {
            return false;
        }
        // have Stream<T>
        final Type streamTypeArgument = typeArguments[0];
        if (!(streamTypeArgument instanceof ParameterizedType)) {
            return false;
        }
        // have Stream<T<...>>
        final ParameterizedType streamParameterizedTypeArgument = (ParameterizedType) streamTypeArgument;
        if (streamParameterizedTypeArgument.getRawType() != PluginType.class) {
            return false;
        }
        // have Stream<PluginType>
        final Type[] streamPluginTypeArguments = streamParameterizedTypeArgument.getActualTypeArguments();
        return streamPluginTypeArguments.length == 1;
        // have Stream<PluginType<...>>
    }

    @Override
    public Supplier<Stream<? extends PluginType<? extends T>>> getFactory(
            final ResolvableKey<Stream<? extends PluginType<? extends T>>> resolvableKey,
            final InstanceFactory instanceFactory) {
        final String namespace = resolvableKey.namespace();
        final ParameterizedType streamType = (ParameterizedType) resolvableKey.type();
        final ParameterizedType supplierType = (ParameterizedType) streamType.getActualTypeArguments()[0];
        final ParameterizedType pluginType = (ParameterizedType) supplierType.getActualTypeArguments()[0];
        final Type componentType = pluginType.getActualTypeArguments()[0];
        return () -> Plugins.streamPluginTypesMatching(instanceFactory, namespace, componentType);
    }
}
