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
import org.apache.logging.log4j.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
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

    static String getName(final AnnotatedElement element) {
        for (final Annotation annotation : element.getAnnotations()) {
            final Optional<String> specifiedName = getSpecifiedNameForAnnotation(annotation);
            if (specifiedName.isPresent()) {
                return specifiedName.get();
            }
        }

        if (element instanceof Field) {
            return ((Field) element).getName();
        }

        if (element instanceof Method) {
            final Method method = (Method) element;
            final String methodName = method.getName();
            if (methodName.startsWith("set")) {
                return BeanUtils.decapitalize(methodName.substring(3));
            }
            if (methodName.startsWith("with")) {
                return BeanUtils.decapitalize(methodName.substring(4));
            }
            return methodName;
        }

        if (element instanceof Parameter) {
            return ((Parameter) element).getName();
        }

        throw new IllegalArgumentException("Unknown element type for naming: " + element.getClass());
    }

    static <A extends Annotation> Optional<String> getSpecifiedNameForAnnotation(final A annotation) {
        return Optional.ofNullable(annotation.annotationType().getAnnotation(NameProvider.class))
                .map(NameProvider::value)
                .flatMap(clazz -> {
                    @SuppressWarnings("unchecked") final AnnotatedElementNameProvider<A> factory =
                            (AnnotatedElementNameProvider<A>) ReflectionUtil.instantiate(clazz);
                    return factory.getSpecifiedName(annotation);
                });
    }

    /**
     * Returns the specified name from this annotation if given or {@code Optional.empty()} if none given.
     *
     * @param annotation annotation value of configuration element
     * @return specified name of configuration element or empty if none specified
     */
    Optional<String> getSpecifiedName(final A annotation);
}
