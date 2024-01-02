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
package org.apache.logging.log4j.plugins.di;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.internal.util.BeanUtils;
import org.apache.logging.log4j.plugins.name.AliasesProvider;
import org.apache.logging.log4j.plugins.name.AnnotatedElementAliasesProvider;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.util.AnnotatedAnnotation;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.apache.logging.log4j.util.Strings;

public final class Keys {
    private Keys() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Gets the {@linkplain Namespace namespace} of the annotated element.
     *
     * @param element annotated element to find namespace for
     * @return annotated namespace of element
     */
    public static String getNamespace(final AnnotatedElement element) {
        return Optional.ofNullable(AnnotationUtil.getLogicalAnnotation(element, Namespace.class))
                .map(Namespace::value)
                .orElse(Strings.EMPTY);
    }

    /**
     * Indicates if an annotated element has a named annotation present. Named annotations are annotations that have a
     * {@link NameProvider} specified.
     *
     * @param element annotated element to check for a name
     * @return true if the annotated element has a named annotation
     */
    public static boolean hasName(final AnnotatedElement element) {
        return AnnotationUtil.isMetaAnnotationPresent(element, NameProvider.class);
    }

    /**
     * Gets the name of the annotated field using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the field. If no named annotations are present, then an empty string
     * is returned. If no {@linkplain AnnotatedElementNameProvider#getSpecifiedName(Annotation) specified name} is given
     * by the name provider, the {@linkplain Field#getName() field name} is returned.
     *
     * @param field annotated field to find name for
     * @return annotated name of field
     */
    public static String getName(final Field field) {
        return hasName(field) ? getSpecifiedName(field).orElseGet(field::getName) : Strings.EMPTY;
    }

    /**
     * Gets the name of the given annotated parameter using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the parameter. If no named annotations are present, then an empty string
     * is returned. If no {@linkplain AnnotatedElementNameProvider#getSpecifiedName(Annotation) specified name} is given
     * by the name provider, the {@linkplain Parameter#getName() parameter name} is returned.
     *
     * @param parameter annotated parameter to find name for
     * @return annotated name of parameter
     */
    public static String getName(final Parameter parameter) {
        return hasName(parameter) ? getSpecifiedName(parameter).orElseGet(parameter::getName) : Strings.EMPTY;
    }

    /**
     * Gets the name of the given annotated method using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the method. If no named annotations are present, then an empty string
     * is returned. If no {@linkplain AnnotatedElementNameProvider#getSpecifiedName(Annotation) specified name} is given by
     * the name provider, the {@linkplain Method#getName() method name} is used with {@code is}, {@code set}, {@code get},
     * and {@code with} prefixes removed and the result being de-capitalized.
     *
     * @param method annotated method to find name for
     * @return annotated name of method without is/set/get/with prefix
     */
    public static String getName(final Method method) {
        return hasName(method)
                ? getSpecifiedName(method).orElseGet(() -> {
                    final String methodName = method.getName();
                    if (methodName.startsWith("is")) {
                        return BeanUtils.decapitalize(methodName.substring(2));
                    }
                    if (methodName.startsWith("set") || methodName.startsWith("get")) {
                        return BeanUtils.decapitalize(methodName.substring(3));
                    }
                    if (methodName.startsWith("with")) {
                        return BeanUtils.decapitalize(methodName.substring(4));
                    }
                    return methodName;
                })
                : Strings.EMPTY;
    }

    /**
     * Gets the name of the given annotated type using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the type. If no named annotations are present or if no
     * {@linkplain AnnotatedElementNameProvider#getSpecifiedName(Annotation) specified name} is given by the name provider,
     * then an empty string is returned.
     *
     * @param annotatedType annotated type to find name for
     * @return annotated name of annotated type
     */
    public static String getName(final AnnotatedType annotatedType) {
        return hasName(annotatedType) ? getSpecifiedName(annotatedType).orElse(Strings.EMPTY) : Strings.EMPTY;
    }

    /**
     * Gets the name of the given annotated class using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the class. If no named annotations are present, then an empty
     * string is returned. If no {@linkplain AnnotatedElementNameProvider#getSpecifiedName(Annotation) specified name} is given
     * by the name provider, then the {@linkplain Class#getSimpleName() simple name} of the annotated class is returned.
     *
     * @param type annotated class to find name for
     * @return annotated name of class
     */
    public static String getName(final Class<?> type) {
        return hasName(type) ? getSpecifiedName(type).orElseGet(type::getSimpleName) : Strings.EMPTY;
    }

    // TODO(ms): these name and alias providers should work via DI
    private static Optional<String> getSpecifiedName(final AnnotatedElement element) {
        return AnnotationUtil.findAnnotatedAnnotations(element, NameProvider.class)
                .findFirst()
                .flatMap(Keys::getSpecifiedName);
    }

    private static <A extends Annotation> Optional<String> getSpecifiedName(
            final AnnotatedAnnotation<A, NameProvider> annotatedAnnotation) {
        final Class<? extends AnnotatedElementNameProvider<A>> providerType =
                Cast.cast(annotatedAnnotation.metaAnnotation().value());
        final AnnotatedElementNameProvider<A> provider = ReflectionUtil.instantiate(providerType);
        return provider.getSpecifiedName(annotatedAnnotation.annotation());
    }

    public static Collection<String> getAliases(final AnnotatedElement element) {
        return AnnotationUtil.findAnnotatedAnnotations(element, AliasesProvider.class)
                .findFirst()
                .map(Keys::getAliases)
                .orElseGet(List::of);
    }

    private static <A extends Annotation> Collection<String> getAliases(
            final AnnotatedAnnotation<A, AliasesProvider> annotatedAnnotation) {
        final Class<? extends AnnotatedElementAliasesProvider<A>> providerType =
                Cast.cast(annotatedAnnotation.metaAnnotation().value());
        final AnnotatedElementAliasesProvider<A> provider = ReflectionUtil.instantiate(providerType);
        return provider.getAliases(annotatedAnnotation.annotation());
    }
}
