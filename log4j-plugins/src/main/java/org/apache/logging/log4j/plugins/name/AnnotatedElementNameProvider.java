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

package org.apache.logging.log4j.plugins.name;

import org.apache.logging.log4j.plugins.internal.util.BeanUtils;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.apache.logging.log4j.util.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * Extracts a specified name for some configurable annotated element. A specified name is one given in a non-empty
 * string in an annotation as opposed to relying on the default name taken from the annotated element itself.
 *
 * @param <A> plugin configuration annotation
 */
public interface AnnotatedElementNameProvider<A extends Annotation> {

    /**
     * Indicates if an annotated element has a named annotation present. Named annotations are annotations that have a
     * {@link NameProvider} specified.
     *
     * @param element annotated element to check for a name
     * @return true if the annotated element has a named annotation
     */
    static boolean hasName(final AnnotatedElement element) {
        return AnnotationUtil.isMetaAnnotationPresent(element, NameProvider.class);
    }

    /**
     * Gets the name of the annotated field using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the field. If no named annotations are present, then an empty string
     * is returned. If no {@linkplain #getSpecifiedName(Annotation) specified name} is given by the name provider,
     * the {@linkplain Field#getName() field name} is returned.
     *
     * @param field annotated field to find name for
     * @return annotated name of field
     */
    static String getName(final Field field) {
        return hasName(field) ? getSpecifiedName(field).orElseGet(field::getName) : Strings.EMPTY;
    }

    /**
     * Gets the name of the given annotated parameter using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the parameter. If no named annotations are present, then an empty string
     * is returned. If no {@linkplain #getSpecifiedName(Annotation) specified name} is given by the name provider,
     * the {@linkplain Parameter#getName() parameter name} is returned.
     *
     * @param parameter annotated parameter to find name for
     * @return annotated name of parameter
     */
    static String getName(final Parameter parameter) {
        return hasName(parameter) ? getSpecifiedName(parameter).orElseGet(parameter::getName) : Strings.EMPTY;
    }

    /**
     * Gets the name of the given annotated method using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the method. If no named annotations are present, then an empty string
     * is returned. If no {@linkplain #getSpecifiedName(Annotation) specified name} is given by the name provider,
     * the {@linkplain Method#getName() method name} is used with {@code is}, {@code set}, {@code get}, and {@code with}
     * prefixes removed and the result being de-capitalized.
     *
     * @param method annotated method to find name for
     * @return annotated name of method without is/set/get/with prefix
     */
    static String getName(final Method method) {
        return hasName(method) ? getSpecifiedName(method).orElseGet(() -> {
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
        }) : Strings.EMPTY;
    }

    /**
     * Gets the name of the given annotated type using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the type. If no named annotations are present or if no
     * {@linkplain #getSpecifiedName(Annotation) specified name} is given by the name provider, then an empty
     * string is returned.
     *
     * @param annotatedType annotated type to find name for
     * @return annotated name of annotated type
     */
    static String getName(final AnnotatedType annotatedType) {
        return hasName(annotatedType) ? getSpecifiedName(annotatedType).orElse(Strings.EMPTY) : Strings.EMPTY;
    }

    /**
     * Gets the name of the given annotated class using the corresponding {@link AnnotatedElementNameProvider}
     * strategy for the named annotation on the class. If no named annotations are present or if no
     * {@linkplain #getSpecifiedName(Annotation) specified name} is given by the name provider, then an empty
     * string is returned.
     *
     * @param type annotated class to find name for
     * @return annotated name of class
     */
    static String getName(final Class<?> type) {
        return hasName(type) ? getSpecifiedName(type).orElseGet(type::getName) : Strings.EMPTY;
    }

    private static Optional<String> getSpecifiedName(final AnnotatedElement element) {
        for (final Annotation annotation : element.getAnnotations()) {
            final Optional<String> name = getSpecifiedNameForAnnotation(annotation);
            if (name.isPresent()) {
                return name;
            }
        }
        return Optional.empty();
    }

    private static <A extends Annotation> Optional<String> getSpecifiedNameForAnnotation(final A annotation) {
        return Optional.ofNullable(annotation.annotationType().getAnnotation(NameProvider.class))
                .map(NameProvider::value)
                .map(TypeUtil::<Class<? extends AnnotatedElementNameProvider<A>>>cast)
                .map(ReflectionUtil::instantiate)
                .flatMap(provider -> provider.getSpecifiedName(annotation));
    }

    /**
     * Returns the specified name from this annotation if given or {@code Optional.empty()} if none given.
     *
     * @param annotation annotation value of configuration element
     * @return specified name of configuration element or empty if none specified
     */
    Optional<String> getSpecifiedName(final A annotation);
}
