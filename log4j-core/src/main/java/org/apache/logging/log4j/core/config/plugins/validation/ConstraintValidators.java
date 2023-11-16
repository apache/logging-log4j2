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
package org.apache.logging.log4j.core.config.plugins.validation;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.logging.log4j.core.util.ReflectionUtil;

/**
 * Utility class to locate an appropriate {@link ConstraintValidator} implementation for an annotation.
 *
 * @since 2.1
 */
public final class ConstraintValidators {

    private ConstraintValidators() {}

    /**
     * Finds all relevant {@link ConstraintValidator} objects from an array of annotations. All validators will be
     * {@link ConstraintValidator#initialize(java.lang.annotation.Annotation) initialized} before being returned.
     *
     * @param annotations the annotations to find constraint validators for
     * @return a collection of ConstraintValidators for the given annotations
     */
    public static Collection<ConstraintValidator<?>> findValidators(final Annotation... annotations) {
        final Collection<ConstraintValidator<?>> validators = new ArrayList<>();
        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> type = annotation.annotationType();
            if (type.isAnnotationPresent(Constraint.class)) {
                final ConstraintValidator<?> validator = getValidator(annotation, type);
                if (validator != null) {
                    validators.add(validator);
                }
            }
        }
        return validators;
    }

    private static <A extends Annotation> ConstraintValidator<A> getValidator(
            final A annotation, final Class<? extends A> type) {
        final Constraint constraint = type.getAnnotation(Constraint.class);
        final Class<? extends ConstraintValidator<?>> validatorClass = constraint.value();
        if (type.equals(getConstraintValidatorAnnotationType(validatorClass))) {
            @SuppressWarnings("unchecked") // I don't think we could be any more thorough in validation here
            final ConstraintValidator<A> validator =
                    (ConstraintValidator<A>) ReflectionUtil.instantiate(validatorClass);
            validator.initialize(annotation);
            return validator;
        }
        return null;
    }

    private static Type getConstraintValidatorAnnotationType(final Class<? extends ConstraintValidator<?>> type) {
        for (final Type parentType : type.getGenericInterfaces()) {
            if (parentType instanceof ParameterizedType) {
                final ParameterizedType parameterizedType = (ParameterizedType) parentType;
                if (ConstraintValidator.class.equals(parameterizedType.getRawType())) {
                    return parameterizedType.getActualTypeArguments()[0];
                }
            }
        }
        return Void.TYPE;
    }
}
