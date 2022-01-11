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

package org.apache.logging.log4j.plugins.validation.constraints;

import org.apache.logging.log4j.plugins.validation.Constraint;
import org.apache.logging.log4j.plugins.validation.validators.RequiredValidator;

import java.lang.annotation.*;

/**
 * Marks a plugin builder field or plugin factory parameter as required.
 *
 * @since 2.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Constraint(RequiredValidator.class)
public @interface Required {

    /**
     * The message to be logged if this constraint is violated. This should normally be overridden.
     * @return the message to be logged if the constraint is violated.
     */
    String message() default "The parameter is null or empty";
}
