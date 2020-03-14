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

package org.apache.logging.log4j.plugins.spi.model;

import org.apache.logging.log4j.plugins.api.AliasFor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

public interface MetaElement<T> {

    /**
     * Returns the source code name of this element.
     */
    String getName();

    /**
     * Returns all the annotations present on this element.
     */
    Collection<Annotation> getAnnotations();

    /**
     * Indicates whether or not an annotation is present on this element taking into account
     * {@linkplain AliasFor annotation aliasing}.
     *
     * @param annotationType type of annotation to look for
     * @return whether or not the annotation is directly or indirectly present on this element
     */
    default boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
        for (final Annotation annotation : getAnnotations()) {
            if (annotationType.equals(annotation.annotationType())) {
                return true;
            }
            if (annotation instanceof AliasFor) {
                return annotationType.equals(((AliasFor) annotation).value());
            }
        }
        return false;
    }

    Type getType();

    Collection<Type> getTypeClosure();

}
