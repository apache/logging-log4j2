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
     * @param name the name to use for error reporting
     * @param value the value to validate.
     * @return {@code true} if the given value is valid.
     */
    boolean isValid(String name, Object value);
}
