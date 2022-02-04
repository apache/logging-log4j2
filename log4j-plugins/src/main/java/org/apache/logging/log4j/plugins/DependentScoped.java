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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Dependent scoped beans are beans that belong to another bean. Beans with this scope are created and destroyed in
 * participation of the lifecycle of the owning bean. That is, when a bean is destroyed, any of its dependent objects
 * are destroyed; dependent beans injected into a {@linkplain Disposes disposer method} are destroyed after the method
 * is finished executing; dependent beans created to {@linkplain Produces produce} or {@linkplain Disposes dispose}
 * a bean are destroyed after the producer or disposer is finished executing; and any other dependent beans no longer
 * directly referenced by the application may be destroyed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Documented
@Inherited
@ScopeType
public @interface DependentScoped {
}
