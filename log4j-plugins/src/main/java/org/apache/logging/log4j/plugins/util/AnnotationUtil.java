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
package org.apache.logging.log4j.plugins.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.spi.InjectionPoint;

public final class AnnotationUtil {

    public static boolean isMetaAnnotationPresent(
            final AnnotatedElement element, final Class<? extends Annotation> metaAnnotation) {
        return findAnnotatedAnnotations(element, metaAnnotation).findAny().isPresent();
    }

    /**
     * Gets an {@linkplain AnnotatedElement#getAnnotations() element annotation} that is
     * {@linkplain AnnotatedElement#isAnnotationPresent(Class) annotated with} the provided meta annotation.
     * Meta annotations should be annotated with {@link Target} containing
     * {@link ElementType#ANNOTATION_TYPE} and {@link ElementType#TYPE_USE} in order to annotate annotations and
     * be used in anonymous class construction using the protected constructors in {@link Key} and {@link InjectionPoint}.
     * For example, if {@code Scope} is an annotation and {@code WebScope} is an annotation that is annotated with
     * {@code @Scope}, then if an element is annotated with {@code @WebScope}, then calling
     * {@code getElementAnnotationHavingMetaAnnotation(element, Scope.class)} will return the {@code @WebScope}
     * annotation present on the annotated element.
     *
     * @param element        element to search for annotations
     * @param metaAnnotation expected annotation to find present on annotation
     * @return the annotation with meta-annotation if present or {@code null} if none match
     */
    public static Annotation getElementAnnotationHavingMetaAnnotation(
            final AnnotatedElement element, final Class<? extends Annotation> metaAnnotation) {
        return findAnnotatedAnnotations(element, metaAnnotation)
                .map(AnnotatedAnnotation::annotation)
                .findFirst()
                .orElse(null);
    }

    public static <M extends Annotation> Stream<AnnotatedAnnotation<? extends Annotation, M>> findAnnotatedAnnotations(
            final AnnotatedElement element, final Class<M> metaAnnotation) {
        final Stream.Builder<AnnotatedAnnotation<? extends Annotation, M>> matched = Stream.builder();
        scanElementForMetaAnnotations(element, metaAnnotation, new HashSet<>(), matched);
        return matched.build();
    }

    private static <M extends Annotation> void scanElementForMetaAnnotations(
            final AnnotatedElement element,
            final Class<M> metaAnnotationType,
            final Set<Class<? extends Annotation>> visitedAnnotations,
            final Stream.Builder<AnnotatedAnnotation<? extends Annotation, M>> matched) {
        for (final Annotation annotation : element.getAnnotations()) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            final String packageName = annotationType.getPackageName();
            if (packageName.startsWith("java.lang.") || packageName.startsWith("org.apache.logging.log4j.lang.")) {
                continue;
            }
            if (visitedAnnotations.add(annotationType)) {
                final M metaAnnotation = annotationType.getAnnotation(metaAnnotationType);
                if (metaAnnotation != null) {
                    matched.add(new AnnotatedAnnotation<>(annotation, metaAnnotation));
                }
                scanElementForMetaAnnotations(annotationType, metaAnnotationType, visitedAnnotations, matched);
            }
        }
    }

    /**
     * Gets an annotation from an annotated element where said annotation may be indirectly present by means of a stereotype
     * annotation or directly present by using the annotation directly. For example, if {@code Component} is an annotation and
     * {@code Service} is another annotation that is annotated with {@code @Component}, then if an element is annotated with
     * {@code @Service}, then calling {@code getLogicalAnnotation(element, Component.class)} will return the {@code @Component}
     * annotation present on the {@code Service} annotation.
     *
     * @param <A> type of logical annotation
     * @param element annotated element to scan for logical annotation
     * @param annotationType class of logical annotation
     * @return a logical annotation instance or {@code null} if none are found
     */
    public static <A extends Annotation> A getLogicalAnnotation(
            final AnnotatedElement element, final Class<A> annotationType) {
        return findLogicalAnnotations(element, annotationType).findFirst().orElse(null);
    }

    public static <A extends Annotation> List<A> getLogicalAnnotations(
            final AnnotatedElement element, final Class<A> annotationType) {
        return findLogicalAnnotations(element, annotationType).collect(Collectors.toList());
    }

    public static <A extends Annotation> Stream<A> findLogicalAnnotations(
            final AnnotatedElement element, final Class<A> annotationType) {
        final Stream.Builder<A> builder = Stream.builder();
        scanElementAnnotationsForLogicalAnnotation(element, annotationType, new HashSet<>(), builder);
        return builder.build();
    }

    private static <A extends Annotation> void scanElementAnnotationsForLogicalAnnotation(
            final AnnotatedElement element,
            final Class<A> logicalAnnotation,
            final Set<Class<? extends Annotation>> visitedAnnotations,
            final Stream.Builder<A> matched) {
        for (final Annotation annotation : element.getAnnotations()) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            final String packageName = annotationType.getPackageName();
            if (packageName.startsWith("java.lang.") || packageName.startsWith("org.apache.logging.log4j.lang.")) {
                continue;
            }
            if (annotationType == logicalAnnotation) {
                matched.add(logicalAnnotation.cast(annotation));
            } else if (visitedAnnotations.add(annotationType)) {
                scanElementAnnotationsForLogicalAnnotation(
                        annotationType, logicalAnnotation, visitedAnnotations, matched);
            }
        }
    }

    /**
     * Returns the list of all declared methods from the provided class and its superclasses that are meta-annotated
     * with the provided annotation type.
     */
    public static List<Method> getDeclaredMethodsMetaAnnotatedWith(
            final Class<?> type, final Class<? extends Annotation> metaAnnotation) {
        return Stream.<Class<?>>iterate(type, c -> c != Object.class, Class::getSuperclass)
                .flatMap(c -> Stream.of(c.getDeclaredMethods()))
                .filter(method -> isMetaAnnotationPresent(method, metaAnnotation))
                .collect(Collectors.toList());
    }

    private AnnotationUtil() {}

    public static OptionalInt getOrder(final AnnotatedElement element) {
        final Ordered ordered = getLogicalAnnotation(element, Ordered.class);
        return ordered == null ? OptionalInt.empty() : OptionalInt.of(ordered.value());
    }
}
