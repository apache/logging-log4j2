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
package org.apache.logging.log4j.core.test.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies one or more pairs of {@linkplain org.apache.logging.log4j.plugins.di.Key binding keys} with
 * injectable classes to bind in a test. The binding key corresponds to {@link #api()} and the factory will
 * use the provided {@link #implementation()} class (or {@link #implementationClassName()} to load reflectively).
 *
 * @see ConfigurationFactoryType
 * @see ContextSelectorType
 */
@Repeatable(TestBinding.Group.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Log4jTest
public @interface TestBinding {
    /**
     * Specifies the {@linkplain org.apache.logging.log4j.plugins.di.Key#forClass(Class) class to use as a key} for this binding.
     */
    Class<?> api();

    /**
     * Specifies the implementation class to use as a binding. If left as the default value of {@code Object.class},
     * then the implementation class must be specified as a fully qualified class name in {@link #implementationClassName()}.
     */
    Class<?> implementation() default Object.class;

    /**
     * Specifies the fully qualified class name to use as a binding. If left blank, then the implementation class must
     * be specified in {@link #implementation()}.
     */
    String implementationClassName() default "";

    /**
     * Annotation container for multiple {@link TestBinding} annotations.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @interface Group {
        TestBinding[] value();
    }
}
