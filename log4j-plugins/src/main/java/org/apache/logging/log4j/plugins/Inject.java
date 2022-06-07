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

package org.apache.logging.log4j.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks a constructor, field, or method for dependency injection. Constructors are injected first, followed by fields,
 * then methods. Superclasses are injected before subclasses. Note that zero-arg methods with this annotation are
 * considered initialization methods which are also invoked during dependency injection.</p>
 *
 * <h2>Constructors</h2>
 * <p>A class can have at most one constructor annotated with {@code @Inject}. This constructor can have zero or more
 * dependencies as arguments. If a class has no constructors annotated with {@code @Inject}, then its default
 * zero args constructor is used if available.</p>
 *
 * <h2>Fields</h2>
 * <p>Only non-final, non-static fields may be annotated with {@code @Inject}.</p>
 *
 * <h2>Methods</h2>
 * <p>Non-abstract, non-static methods may be annotated with {@code @Inject}. These methods must not
 * declare any type parameters of their own, take zero or more dependencies as arguments, and may return a value which
 * is ignored (e.g., for builder method chaining).</p>
 *
 * @see Named
 * @see Factory
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface Inject {
}
