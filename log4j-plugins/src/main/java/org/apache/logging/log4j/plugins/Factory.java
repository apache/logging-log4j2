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
 * Marks a method as a factory for supplying sources of instances of the method's return type.
 * Factory methods are comparable to bean factories from other dependency injection frameworks such as {@code @Bean}
 * in the Spring Framework or {@code @Provides} in the Guice framework. There are a few rules related to factory methods:
 *
 * <ul>
 *     <li>Factory methods must be non-abstract methods of a bean class (static or not).</li>
 *     <li>If the bean class's scope is different from the provider bean, then it must be a larger scope.</li>
 *     <li>If factory methods return a nullable value, then it must be unscoped.</li>
 *     <li>Factory method return type must not be a type variable.</li>
 *     <li>Factory method return type may be parameterized only if it specifies actual types
 *     or a type variable for each parameter, the latter case also required to be unscoped.</li>
 *     <li>Classes may declare multiple factory methods to create different instance types or qualified instances.</li>
 *     <li>Factory methods are <em>not inherited</em> by subclasses.</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@FactoryType
public @interface Factory {
}
