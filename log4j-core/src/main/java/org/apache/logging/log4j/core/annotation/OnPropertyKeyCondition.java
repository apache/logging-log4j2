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
package org.apache.logging.log4j.core.annotation;

import java.lang.reflect.AnnotatedElement;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.condition.Condition;
import org.apache.logging.log4j.plugins.condition.ConditionContext;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertyKey;

@Singleton
public class OnPropertyKeyCondition implements Condition {
    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public boolean matches(final ConditionContext context, final AnnotatedElement element) {
        final ConditionalOnPropertyKey annotation =
                AnnotationUtil.getLogicalAnnotation(element, ConditionalOnPropertyKey.class);
        if (annotation == null) {
            return false;
        }
        final PropertyKey propertyKey = annotation.key();
        final String value = annotation.value();
        final String property = context.getEnvironment().getStringProperty(propertyKey);
        final boolean result = property != null && (value.isEmpty() || value.equalsIgnoreCase(property));
        LOGGER.debug("ConditionalOnPropertyKey {} for key='{}', value='{}'; property='{}'", result,
                propertyKey.getName(), value, property);
        return result;
    }
}
