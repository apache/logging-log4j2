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

package org.apache.logging.log4j.plugins.util;

import org.apache.logging.log4j.plugins.di.AnnotationAlias;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

public final class AnnotationUtil {

    public static boolean isAnnotationPresent(final AnnotatedElement element, final Class<? extends Annotation> annotationType) {
        if (element.isAnnotationPresent(annotationType)) {
            return true;
        }
        for (final Annotation annotation : element.getAnnotations()) {
            final AnnotationAlias alias = annotation.annotationType().getAnnotation(AnnotationAlias.class);
            if (alias != null && annotationType.equals(alias.value())) {
                return true;
            }
        }
        return false;
    }

    private AnnotationUtil() {
    }
}
