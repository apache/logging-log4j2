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
package org.apache.logging.log4j.core.config.plugins.validation;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Utility class to locate an appropriate {@link ConstraintValidator} implementation for an annotation.
 *
 * @since 2.1
 */
public final class ConstraintValidators {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private ConstraintValidators() {
    }

    /**
     * Finds all relevant {@link ConstraintValidator} objects from an array of annotations. All validators will be
     * {@link ConstraintValidator#initialize(java.lang.annotation.Annotation) initialized} before being returned.
     *
     * @param annotations the annotations to find constraint validators for
     * @return a collection of ConstraintValidators for the given annotations
     */
    public static Collection<ConstraintValidator<Annotation, Object>> findValidators(
        final Annotation... annotations) {
        final Collection<ConstraintValidator<Annotation, Object>> validators =
            new ArrayList<ConstraintValidator<Annotation, Object>>();
        for (final Annotation annotation : annotations) {
            final Constraint constraint = annotation.annotationType().getAnnotation(Constraint.class);
            if (constraint != null) {
                final ConstraintValidator<Annotation, Object> validator = getValidatorFor(annotation, constraint);
                if (validator != null) {
                    validators.add(validator);
                }
            }
        }
        return validators;
    }

    private static ConstraintValidator<Annotation, Object> getValidatorFor(final Annotation annotation,
                                                                           final Constraint constraint) {
        try {
            // TODO: may want to cache these validator instances
            @SuppressWarnings("unchecked")
            final ConstraintValidator<Annotation, Object> validator =
                (ConstraintValidator<Annotation, Object>) constraint.value().newInstance();
            validator.initialize(annotation);
            return validator;
        } catch (final Exception e) {
            LOGGER.error("Error loading ConstraintValidator [{}].", constraint.value(), e);
            return null;
        }
    }
}
