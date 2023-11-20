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
package org.apache.logging.log4j.plugins.condition;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.status.StatusLogger;

public class OnPropertyCondition implements Condition {
    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedElement element) {
        return conditionals(element).allMatch(annotation -> {
            final String name = annotation.name();
            final String value = annotation.value();
            final String property = context.getEnvironment().getStringProperty(name);
            final boolean matchIfMissing = annotation.matchIfMissing();
            final boolean result = propertyMatches(property, value, matchIfMissing);
            LOGGER.debug(
                    "ConditionalOnProperty {} for name='{}', value='{}'; property='{}', matchIfMissing={}",
                    result,
                    name,
                    value,
                    property,
                    matchIfMissing);
            return result;
        });
    }

    private static Stream<ConditionalOnProperty> conditionals(final AnnotatedElement element) {
        final Stream<ConditionalOnProperty> elementAnnotations =
                AnnotationUtil.findLogicalAnnotations(element, ConditionalOnProperty.class);
        if (element instanceof Method) {
            final Class<?> declaringClass = ((Method) element).getDeclaringClass();
            final Stream<ConditionalOnProperty> declaringClassAnnotations =
                    AnnotationUtil.findLogicalAnnotations(declaringClass, ConditionalOnProperty.class);
            return Stream.concat(elementAnnotations, declaringClassAnnotations);
        }
        return elementAnnotations;
    }

    private static boolean propertyMatches(final String property, final String value, final boolean matchIfMissing) {
        if (property == null) {
            return matchIfMissing;
        }
        return value.isEmpty() || value.equalsIgnoreCase(property);
    }
}
