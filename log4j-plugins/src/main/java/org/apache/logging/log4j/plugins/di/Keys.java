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
import java.util.function.Function;

import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.internal.util.BeanUtils;
import org.apache.logging.log4j.plugins.name.AliasesProvider;
import org.apache.logging.log4j.plugins.name.AnnotatedElementAliasesProvider;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;
import org.apache.logging.log4j.plugins.name.NameProvider;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.ReflectionUtil;
import org.apache.logging.log4j.util.Strings;

public final class Keys {
    private Keys() {
        throw new IllegalStateException("Utility class");
    }

    public static final String SUBSTITUTOR_NAME = "StringSubstitutor";
    public static final Key<Function<String, String>> SUBSTITUTOR_KEY = new @Named(SUBSTITUTOR_NAME) Key<>() {};

    public static final String PLUGIN_PACKAGES_NAME = "PluginPackages";
    public static final Key<List<String>> PLUGIN_PACKAGES_KEY = new @Named(PLUGIN_PACKAGES_NAME) Key<>() {};

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
                .map(Cast::<Class<? extends AnnotatedElementNameProvider<A>>>cast)
                .map(ReflectionUtil::instantiate)
                .flatMap(provider -> provider.getSpecifiedName(annotation));
    }

    public static Collection<String> getAliases(final AnnotatedElement element) {
        for (final Annotation annotation : element.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(AliasesProvider.class)) {
                return getAliasesForAnnotation(annotation);
            }
        }
        return List.of();
    }

    private static <A extends Annotation> Collection<String> getAliasesForAnnotation(final A annotation) {
        @SuppressWarnings("unchecked") final var providerType = (Class<AnnotatedElementAliasesProvider<A>>)
                annotation.annotationType().getAnnotation(AliasesProvider.class).value();
        final AnnotatedElementAliasesProvider<A> provider = ReflectionUtil.instantiate(providerType);
        return provider.getAliases(annotation);
    }
}
