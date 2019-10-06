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

package org.apache.logging.log4j.plugins.bind;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.convert.TypeConverterRegistry;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.plugins.validation.ConstraintValidators;
import org.apache.logging.log4j.status.StatusLogger;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

/**
 * Generic configuration binder for an {@link AnnotatedElement}. This provides automatic
 * {@linkplain TypeConverter string conversion} and {@linkplain ConstraintValidator constraint validation} support.
 *
 * @param <E> element type being bound
 */
public abstract class AbstractConfigurationBinder<E extends AnnotatedElement> implements ConfigurationBinder {
    protected static final Logger LOGGER = StatusLogger.getLogger();

    final E element;
    final String name;
    private final Type injectionType;
    private final Collection<ConstraintValidator<?>> validators;

    AbstractConfigurationBinder(final E element, final Function<E, Type> injectionTypeExtractor) {
        this.element = Objects.requireNonNull(element);
        this.name = AnnotatedElementNameProvider.getName(element);
        Objects.requireNonNull(injectionTypeExtractor);
        this.injectionType = Objects.requireNonNull(injectionTypeExtractor.apply(element));
        this.validators = ConstraintValidators.findValidators(element.getAnnotations());
    }

    @Override
    public void bindString(final Object factory, final String value) {
        Object convertedValue = null;
        if (value != null) {
            final TypeConverter<?> converter = TypeConverterRegistry.getInstance().findCompatibleConverter(injectionType);
            try {
                convertedValue = converter.convert(value);
            } catch (final Exception e) {
                throw new ConfigurationBindingException(name, value, e);
            }
        }
        bindObject(factory, convertedValue);
    }

    void validate(final Object value) {
        boolean valid = true;
        for (ConstraintValidator<?> validator : validators) {
            valid &= validator.isValid(name, value);
        }
        // FIXME: this doesn't seem to work properly with primitive types
//        if (valid && value != null && !TypeUtil.isAssignable(injectionType, value.getClass())) {
//            LOGGER.error("Cannot bind value of type {} to option {} with type {}", value.getClass(), name, injectionType);
//            valid = false;
//        }
        if (!valid) {
            throw new ConfigurationBindingException(name, value);
        }
    }

}
