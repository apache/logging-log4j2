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
 * Marks an annotation as a scope type. The scope of a bean determines its lifecycle and the visibility of its
 * instances. In particular, this controls:
 * <ul>
 *     <li>when new instances of a bean are created,</li>
 *     <li>when existing instances of a bean are destroyed, and</li>
 *     <li>which injected references refer to any instance of the bean.</li>
 * </ul>
 */
@Target(ElementType.ANNOTATION_TYPE) // on an annotation typically with @Target(TYPE), but we use static factory methods
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScopeType {
}
