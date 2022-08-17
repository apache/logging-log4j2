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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashSet;
import java.util.Set;

public final class AnnotationUtil {

    public static boolean isMetaAnnotationPresent(
            final AnnotatedElement element, final Class<? extends Annotation> metaAnnotation) {
        return getMetaAnnotation(element, metaAnnotation) != null;
    }

    public static Annotation getMetaAnnotation(
            final AnnotatedElement element, final Class<? extends Annotation> metaAnnotation) {
        return findMetaAnnotation(element, metaAnnotation, new HashSet<>());
    }

    private static Annotation findMetaAnnotation(
            final AnnotatedElement element, final Class<? extends Annotation> metaAnnotation,
            final Set<Class<? extends Annotation>> seen) {
        for (final Annotation annotation : element.getAnnotations()) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (seen.add(annotationType)) {
                if (annotationType.isAnnotationPresent(metaAnnotation)) {
                    return annotation;
                }
                final Annotation original = findMetaAnnotation(annotationType, metaAnnotation, seen);
                if (original != null) {
                    return original;
                }
            }
        }
        return null;
    }

    /**
     * Gets an annotation from an annotated element where said annotation may be indirectly present by means of a stereotype
     * annotation or directly present by using the annotation directly. For example, if {@code Foo} is an annotation and
     * {@code Bar} is another annotation that is annotated with {@code Foo}, then if an element is annotated with
     * {@code Bar}, then calling {@code getLogicalAnnotation(element, Foo.class)} will return the {@code Foo} annotation
     * present on the {@code Bar} annotation.
     */
    public static <A extends Annotation> A getLogicalAnnotation(final AnnotatedElement element, final Class<A> annotationType) {
        return findLogicalAnnotation(element, annotationType, new HashSet<>());
    }

    private static <A extends Annotation> A findLogicalAnnotation(
            final AnnotatedElement element, final Class<A> annotationType, final Set<Class<? extends Annotation>> seen) {
        final A elementAnnotation = element.getAnnotation(annotationType);
        if (elementAnnotation != null) {
            return elementAnnotation;
        }
        for (final Annotation annotation : element.getAnnotations()) {
            final Class<? extends Annotation> metaAnnotationType = annotation.annotationType();
            if (seen.add(metaAnnotationType)) {
                final A ann = findLogicalAnnotation(metaAnnotationType, annotationType, seen);
                if (ann != null) {
                    return ann;
                }
            }
        }
        return null;
    }

    private AnnotationUtil() {
    }
}
