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

package org.apache.logging.log4j.core.config.di;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;

/**
 * Represents metadata about an element in a program where a value should be injected.
 */
public interface InjectionPoint {

    /**
     * Gets the generic type information of this point.
     */
    Type getType();

    String getName();

    Collection<String> getAliases();

    /**
     * Gets the bean where this injection point is defined or empty for static methods and fields.
     */
    Optional<Bean<?>> getBean();

    /**
     * Gets the field, method, or constructor where injection takes place.
     */
    Member getMember();

    /**
     * Gets the program element corresponding to this injection point.
     */
    AnnotatedElement getElement();

}
