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

import org.apache.logging.log4j.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Collection;
import java.util.List;

public interface AnnotatedElementAliasesProvider<A extends Annotation> {

    static Collection<String> getAliases(final AnnotatedElement element) {
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

    Collection<String> getAliases(final A annotation);
}
