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
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.util.OrderedComparator;
import org.apache.logging.log4j.plugins.util.TypeUtil;

class Plugins {

    static <T> Key<T> pluginKey(final PluginType<T> pluginType) {
        return Key.builder(pluginType.getPluginClass())
                .setNamespace(pluginType.getNamespace())
                .setName(pluginType.getName())
                .get();
    }

    static Stream<PluginType<?>> streamPluginTypesMatching(final InstanceFactory instanceFactory,
                                                           final String namespace, final Type type) {
        return instanceFactory.getInstance(Key.forClass(PluginNamespace.class).withNamespace(namespace))
                .stream()
                .filter(pluginType -> TypeUtil.isAssignable(type, pluginType.getPluginClass()))
                .sorted(Comparator.comparing(PluginType::getPluginClass, OrderedComparator.INSTANCE));
    }

    static Stream<?> streamPluginsMatching(final InstanceFactory instanceFactory,
                                           final String namespace, final Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == Supplier.class) {
                final Type suppliedType = parameterizedType.getActualTypeArguments()[0];
                return streamPluginFactoriesMatching(instanceFactory, namespace, suppliedType);
            }
        }
        return streamPluginInstancesMatching(instanceFactory, namespace, type);
    }

    private static Stream<Supplier<?>> streamPluginFactoriesMatching(final InstanceFactory instanceFactory,
                                                                     final String namespace, final Type type) {
        return streamPluginTypesMatching(instanceFactory, namespace, type)
                .map(pluginType -> instanceFactory.getFactory(pluginKey(pluginType)));
    }

    private static Stream<?> streamPluginInstancesMatching(final InstanceFactory instanceFactory,
                                                           final String namespace, final Type type) {
        return streamPluginTypesMatching(instanceFactory, namespace, type)
                .map(pluginType -> instanceFactory.getInstance(pluginKey(pluginType)));
    }
}
