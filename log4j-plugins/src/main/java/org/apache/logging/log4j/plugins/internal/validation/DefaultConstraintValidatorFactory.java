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
package org.apache.logging.log4j.plugins.internal.validation;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.validation.ConstraintValidator;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.plugins.validation.constraints.RequiredClass;
import org.apache.logging.log4j.plugins.validation.constraints.RequiredProperty;
import org.apache.logging.log4j.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.plugins.validation.constraints.ValidPort;
import org.apache.logging.log4j.plugins.validation.spi.ConstraintValidatorFactory;

public class DefaultConstraintValidatorFactory implements ConstraintValidatorFactory {

    private final Map<Class<? extends Annotation>, Supplier<ConstraintValidator<?>>> validators;

    public DefaultConstraintValidatorFactory(final TypeConverter<Integer> integerTypeConverter) {
        validators = Map.of(
                RequiredClass.class,
                RequiredClassValidator::new,
                RequiredProperty.class,
                RequiredPropertyValidator::new,
                Required.class,
                RequiredValidator::new,
                ValidHost.class,
                ValidHostValidator::new,
                ValidPort.class,
                () -> new ValidPortValidator(integerTypeConverter));
    }

    @Override
    public <A extends Annotation> ConstraintValidator<A> createValidator(final A annotation) {
        final Supplier<ConstraintValidator<?>> supplier = validators.get(annotation.getClass());
        if (supplier == null) {
            throw new IllegalArgumentException("Didn't find a constraint validator for constraint " + annotation);
        }
        return (ConstraintValidator<A>) supplier.get();
    }
}
