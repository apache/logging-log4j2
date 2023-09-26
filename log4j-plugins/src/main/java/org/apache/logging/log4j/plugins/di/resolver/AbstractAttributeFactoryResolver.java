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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.di.InstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.spi.FactoryResolver;
import org.apache.logging.log4j.plugins.di.spi.InjectionPoint;
import org.apache.logging.log4j.plugins.di.spi.ResolvableKey;
import org.apache.logging.log4j.plugins.di.spi.StringValueResolver;
import org.apache.logging.log4j.status.StatusLogger;

public abstract class AbstractAttributeFactoryResolver<T, A extends Annotation> implements FactoryResolver<T> {
    protected static final Logger LOGGER = StatusLogger.getLogger();
    protected final Class<A> annotationType;

    protected AbstractAttributeFactoryResolver(final Class<A> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public boolean supportsKey(final Key<?> key) {
        return key.getQualifierType() == annotationType;
    }

    @Override
    public Supplier<T> getFactory(final ResolvableKey<T> resolvableKey, final InstanceFactory instanceFactory) {
        final InjectionPoint<?> injectionPoint = instanceFactory.getInstance(InjectionPoint.CURRENT_INJECTION_POINT);
        return () -> {
            final Key<?> key = resolvableKey.getKey();
            final Type type = key.getType();
            final TypeConverter<T> typeConverter = instanceFactory.getTypeConverter(type);
            final AnnotatedElement element = injectionPoint.getElement();
            final A annotation = element.getAnnotation(annotationType);
            final boolean sensitive = isSensitive(annotation);
            final Node node = instanceFactory.getInstance(Node.CURRENT_NODE);
            final StringValueResolver resolver;
            if (instanceFactory.hasBinding(StringValueResolver.KEY)) {
                resolver = instanceFactory.getInstance(StringValueResolver.KEY);
            } else {
                resolver = StringValueResolver.NOOP;
            }
            LOGGER.trace("Configuring node {} attribute {}", node.getName(), key);
            final String attribute = node.removeMatchingAttribute(key.getName(), resolvableKey.getAliases())
                    .map(resolver::resolve)
                    .orElse(null);
            if (attribute != null) {
                LOGGER.trace("Configured node {} {}={}", node.getName(), key, sensitive ? "(sensitive)" : attribute);
                return typeConverter.convert(attribute, null, sensitive);
            }
            final T defaultValue = getDefaultValue(annotation, resolver, type, typeConverter);
            LOGGER.trace("Configured node {} {}={} (default value)", node.getName(), key, defaultValue);
            return defaultValue;
        };
    }

    protected abstract boolean isSensitive(final A annotation);

    protected abstract T getDefaultValue(final A annotation, final StringValueResolver resolver,
                                         final Type type, final TypeConverter<T> typeConverter);
}
