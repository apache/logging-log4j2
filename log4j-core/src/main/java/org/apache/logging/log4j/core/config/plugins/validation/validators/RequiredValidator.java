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
package org.apache.logging.log4j.core.config.plugins.validation.validators;

import java.util.Collection;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Assert;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Validator that checks an object for emptiness. Emptiness is defined here as:
 * <ul>
 * <li>The value {@code null}</li>
 * <li>An object of type {@link CharSequence} with length 0</li>
 * <li>An empty array</li>
 * <li>An empty {@link Collection}</li>
 * <li>An empty {@link Map}</li>
 * </ul>
 *
 * @since 2.1
 */
public class RequiredValidator implements ConstraintValidator<Required> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private Required annotation;

    @Override
    public void initialize(final Required anAnnotation) {
        this.annotation = anAnnotation;
    }

    @Override
    public boolean isValid(final String name, final Object value) {
        return Assert.isNonEmpty(value) || err(name);
    }

    private boolean err(final String name) {
        LOGGER.error(annotation.message(), name);
        return false;
    }
}
