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

package org.apache.logging.log4j.core.test.junit;

import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.util.Strings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * Specifies the name of an {@link org.apache.logging.log4j.core.Appender} to inject into JUnit 5 tests from the specified
 * configuration.
 *
 * @see LoggerContextSource
 * @since 2.14.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
@NameProvider(Named.Provider.class)
public @interface Named {
    /**
     * Specifies the name of the configuration item to inject. If blank, uses the name of the annotated parameter.
     */
    String value() default "";

    class Provider implements AnnotatedElementNameProvider<Named> {
        @Override
        public Optional<String> getSpecifiedName(final Named annotation) {
            return Strings.trimToOptional(annotation.value());
        }
    }
}
