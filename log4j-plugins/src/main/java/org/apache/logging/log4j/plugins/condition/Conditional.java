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
package org.apache.logging.log4j.plugins.condition;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for conditional annotations to reference the implementing {@link Condition} class to handle
 * conditional checks for whether the bindings present in a bundle instance should be registered. When a bundle class is
 * annotated with one or more conditional annotations, then these conditions are applied to all
 * {@linkplain org.apache.logging.log4j.plugins.Factory factory methods} from that class. When a bundle method is
 * annotated with one or more conditional annotations, then these conditions are applied to that method in addition to
 * any conditions defined on the bundle class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
public @interface Conditional {
    /**
     * Implementation class to use for performing conditional checks when a bundle class or bundle method is annotated
     * with the conditional annotation this annotation is present on.
     */
    Class<? extends Condition> value();
}
