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
package org.apache.logging.log4j.layout.template.json.util;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverter;
import org.apache.logging.log4j.core.config.plugins.convert.TypeConverters;

/**
 * A custom {@link RecyclerFactory} type converter that always returns the same instance if the specification is {@value RecyclerFactoryCustomConverter#NAME}; otherwise, falls back to {@link RecyclerFactories#ofSpec(String) the default}.
 */
@SuppressWarnings("ComparableType")
@Plugin(name = "RecyclerFactoryCustomConverter", category = TypeConverters.CATEGORY)
public final class RecyclerFactoryCustomConverter
        implements TypeConverter<RecyclerFactory>, Comparable<TypeConverter<RecyclerFactory>> {

    static final String NAME = "魔法";

    static final RecyclerFactory RECYCLER_FACTORY = new RecyclerFactory() {

        @Override
        public <V> Recycler<V> create(final Supplier<V> supplier, final Consumer<V> cleaner) {
            return new Recycler<V>() {

                @Override
                public V acquire() {
                    return supplier.get();
                }

                @Override
                public void release(V value) {
                    // Do nothing;
                }
            };
        }
    };

    @Override
    public int compareTo(TypeConverter<RecyclerFactory> typeConverter) {
        return -1;
    }

    @Override
    public RecyclerFactory convert(final String recyclerFactorySpec) {
        return NAME.equals(recyclerFactorySpec) ? RECYCLER_FACTORY : RecyclerFactories.ofSpec(recyclerFactorySpec);
    }
}
