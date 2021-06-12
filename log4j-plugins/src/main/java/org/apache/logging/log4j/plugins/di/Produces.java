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

package org.apache.logging.log4j.plugins.di;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or field as a producer for bean instances. Producer methods are essentially bean factories, while
 * producer fields are bean factories that only depend on its containing bean state. This annotation works similarly
 * to the {@code @Produces} annotation in CDI or the {@code @Bean} annotation in Spring.
 * There are a few rules related to producers:
 *
 * <ul>
 *     <li>Producer methods must be non-abstract methods of a bean class (static or not).</li>
 *     <li>Producer fields must be from a bean class.</li>
 *     <li>If the bean class's scope is different from the produced bean, then it must be a larger scope.</li>
 *     <li>If producer methods return a nullable value, then it must be {@link DependentScoped}.</li>
 *     <li>Producer method return type and producer field type must not be a type variable.</li>
 *     <li>Producer method return type and producer field type may be parameterized only if it specifies actual types
 *     or a type variable for each parameter, the latter case also required to be {@link DependentScoped}.</li>
 *     <li>Beans may declare multiple producer methods and fields.</li>
 *     <li>Producers are <em>not inherited</em> by subclasses.</li>
 * </ul>
 *
 * @see Disposes
 * @see <a href="https://docs.jboss.org/cdi/api/2.0/javax/enterprise/inject/Produces.html">CDI @Produces API Docs</a>
 * @see <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Bean.html">Spring @Bean API Docs</a>
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Produces {
}
