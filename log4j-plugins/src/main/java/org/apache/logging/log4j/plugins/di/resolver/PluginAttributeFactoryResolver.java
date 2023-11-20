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

import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Function;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.di.spi.StringValueResolver;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.Strings;

/**
 * Factory resolver for {@link PluginAttribute}-annotated keys. This injects configuration parameters to a
 * configurable plugin.
 */
public class PluginAttributeFactoryResolver<T> extends AbstractAttributeFactoryResolver<T, PluginAttribute> {
    private static final Map<Type, Function<PluginAttribute, ?>> DEFAULT_VALUE_EXTRACTORS = Map.ofEntries(
            Map.entry(int.class, PluginAttribute::defaultInt),
            Map.entry(Integer.class, PluginAttribute::defaultInt),
            Map.entry(long.class, PluginAttribute::defaultLong),
            Map.entry(Long.class, PluginAttribute::defaultLong),
            Map.entry(boolean.class, PluginAttribute::defaultBoolean),
            Map.entry(Boolean.class, PluginAttribute::defaultBoolean),
            Map.entry(float.class, PluginAttribute::defaultFloat),
            Map.entry(Float.class, PluginAttribute::defaultFloat),
            Map.entry(double.class, PluginAttribute::defaultDouble),
            Map.entry(Double.class, PluginAttribute::defaultDouble),
            Map.entry(byte.class, PluginAttribute::defaultByte),
            Map.entry(Byte.class, PluginAttribute::defaultByte),
            Map.entry(char.class, PluginAttribute::defaultChar),
            Map.entry(Character.class, PluginAttribute::defaultChar),
            Map.entry(short.class, PluginAttribute::defaultShort),
            Map.entry(Short.class, PluginAttribute::defaultShort),
            Map.entry(Class.class, PluginAttribute::defaultClass));

    public PluginAttributeFactoryResolver() {
        super(PluginAttribute.class);
    }

    @Override
    protected boolean isSensitive(final PluginAttribute annotation) {
        return annotation.sensitive();
    }

    @Override
    protected T getDefaultValue(
            final PluginAttribute annotation,
            final StringValueResolver resolver,
            final Type type,
            final TypeConverter<T> typeConverter) {
        final Function<PluginAttribute, T> extractor = Cast.cast(DEFAULT_VALUE_EXTRACTORS.get(type));
        if (extractor != null) {
            return extractor.apply(annotation);
        }
        final String defaultString = resolver.resolve(annotation.defaultString());
        return Strings.isEmpty(defaultString) ? null : typeConverter.convert(defaultString, null);
    }
}
