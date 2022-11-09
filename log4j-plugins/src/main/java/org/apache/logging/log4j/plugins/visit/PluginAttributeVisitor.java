/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.plugins.visit;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

// copied to log4j-core for backward compatibility
@SuppressWarnings("DuplicatedCode")
public class PluginAttributeVisitor implements NodeVisitor {
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
            Map.entry(Class.class, PluginAttribute::defaultClass)
    );

    private final Function<String, String> stringSubstitutionStrategy;
    private final Injector injector;

    @Inject
    public PluginAttributeVisitor(
            @Named(Keys.SUBSTITUTOR_NAME) final Function<String, String> stringSubstitutionStrategy,
            final Injector injector) {
        this.stringSubstitutionStrategy = stringSubstitutionStrategy;
        this.injector = injector;
    }

    @Override
    public Object visitField(final Field field, final Node node, final StringBuilder debugLog) {
        final String name = Keys.getName(field);
        final Collection<String> aliases = Keys.getAliases(field);
        final PluginAttribute annotation = field.getAnnotation(PluginAttribute.class);
        final boolean sensitive = annotation.sensitive();
        final Type targetType = field.getGenericType();
        final TypeConverter<?> converter = injector.getTypeConverter(targetType);
        final Object value = node.removeMatchingAttribute(name, aliases)
                .map(stringSubstitutionStrategy.andThen(s -> (Object) converter.convert(s, null, sensitive)))
                .orElseGet(() -> getDefaultValue(targetType, annotation));
        StringBuilders.appendKeyDqValueWithJoiner(debugLog, name, sensitive ? "(***)" : value, ", ");
        return value;
    }

    @Override
    public Object visitParameter(final Parameter parameter, final Node node, final StringBuilder debugLog) {
        final String name = Keys.getName(parameter);
        final Collection<String> aliases = Keys.getAliases(parameter);
        final Type targetType = parameter.getParameterizedType();
        final TypeConverter<?> converter = injector.getTypeConverter(targetType);
        final PluginAttribute annotation = parameter.getAnnotation(PluginAttribute.class);
        final boolean sensitive = annotation.sensitive();
        final Object value = node.removeMatchingAttribute(name, aliases)
                .map(stringSubstitutionStrategy.andThen(s -> (Object) converter.convert(s, null, sensitive)))
                .orElseGet(() -> getDefaultValue(targetType, annotation));
        StringBuilders.appendKeyDqValueWithJoiner(debugLog, name, sensitive ? "(***)" : value, ", ");
        return value;
    }

    private Object getDefaultValue(final Type targetType, final PluginAttribute annotation) {
        final Function<PluginAttribute, ?> extractor = DEFAULT_VALUE_EXTRACTORS.get(targetType);
        if (extractor != null) {
            return extractor.apply(annotation);
        }
        final TypeConverter<?> converter = injector.getTypeConverter(targetType);
        final var value = stringSubstitutionStrategy.apply(annotation.defaultString());
        return Strings.isEmpty(value) ? null : converter.convert(value, null);
    }
}
