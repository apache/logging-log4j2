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
package org.apache.logging.log4j.core.config.plugins.validation.validators;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.NoUnresolvedVariables;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Validator that checks if the given string contains unresolved variables.
 *
 * @since 3.0
 */
public class NoUnresolvedVariablesValidator implements ConstraintValidator<NoUnresolvedVariables> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private NoUnresolvedVariables annotation;

    @Override
    public void initialize(final NoUnresolvedVariables anAnnotation) {
        this.annotation = anAnnotation;
    }

    @Override
    public boolean isValid(final String name, final Object value) {
        if(value instanceof String) {
            String val = (String)value;

            if( val.contains( StrSubstitutor.DEFAULT_ESCAPE + "{" ) ) {
                LOGGER.error(annotation.message() + ": " + name);
                return false;
            }
        }

        return true;
    }
}
