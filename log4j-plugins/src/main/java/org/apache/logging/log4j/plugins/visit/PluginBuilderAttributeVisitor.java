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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.function.Function;

import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.util.StringBuilders;

public class PluginBuilderAttributeVisitor implements NodeVisitor {
    private final Function<String, String> stringSubstitutionStrategy;
    private final Injector injector;

    @Inject
    public PluginBuilderAttributeVisitor(
            @Named(Keys.SUBSTITUTOR_NAME) final Function<String, String> stringSubstitutionStrategy,
            final Injector injector) {
        this.stringSubstitutionStrategy = stringSubstitutionStrategy;
        this.injector = injector;
    }

    protected boolean isSensitive(final AnnotatedElement element) {
        return element.getAnnotation(PluginBuilderAttribute.class).sensitive();
    }

    @Override
    public Object visitField(final Field field, final Node node, final StringBuilder debugLog) {
        final String name = Keys.getName(field);
        final Collection<String> aliases = Keys.getAliases(field);
        final Type targetType = field.getGenericType();
        final TypeConverter<?> converter = injector.getTypeConverter(targetType);
        final boolean sensitive = isSensitive(field);
        final Object value = node.removeMatchingAttribute(name, aliases)
                .map(stringSubstitutionStrategy.andThen(s -> converter.convert(s, null, sensitive)))
                .orElse(null);
        StringBuilders.appendKeyDqValueWithJoiner(debugLog, name, sensitive ? "(***)" : value, ", ");
        return value;
    }

    @Override
    public Object visitParameter(final Parameter parameter, final Node node, final StringBuilder debugLog) {
        final String name = Keys.getName(parameter);
        final Collection<String> aliases = Keys.getAliases(parameter);
        final Type targetType = parameter.getParameterizedType();
        final TypeConverter<?> converter = injector.getTypeConverter(targetType);
        final boolean sensitive = isSensitive(parameter);
        final Object value = node.removeMatchingAttribute(name, aliases)
                .map(stringSubstitutionStrategy.andThen(s -> converter.convert(s, null, sensitive)))
                .orElse(null);
        StringBuilders.appendKeyDqValueWithJoiner(debugLog, name, sensitive ? "(***)" : value, ", ");
        return value;
    }
}
