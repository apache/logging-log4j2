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

/**
 * Interface that {@link Constraint} annotations must implement to perform validation logic.
 *
 * @param <A> the {@link Constraint} annotation this interface validates.
 * @since 2.1
 */
public interface ConstraintValidator<A extends Annotation> {

    /**
     * Called before this validator is used with the constraint annotation value.
     *
     * @param annotation the annotation value this validator will be validating.
     */
    void initialize(A annotation);

    /**
     * Indicates if the given value is valid.
     *
     * @param value the value to validate.
     * @return {@code true} if the given value is valid.
     */
    boolean isValid(Object value);
}
