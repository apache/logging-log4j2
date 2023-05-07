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
package org.apache.logging.log4j.plugins.condition;

import java.lang.reflect.AnnotatedElement;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.util.AnnotationUtil;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

@Singleton
public class OnPropertyCondition implements Condition {
    private static final Logger LOGGER = StatusLogger.getLogger();

    @Override
    public boolean matches(final Key<?> key, final AnnotatedElement element) {
        final ConditionalOnProperty annotation =
                AnnotationUtil.getLogicalAnnotation(element, ConditionalOnProperty.class);
        if (annotation == null) {
            return false;
        }
        final String name = annotation.name();
        final String value = annotation.value();
        final String property = PropertiesUtil.getProperties().getStringProperty(name);
        final boolean result = property != null && (value.isEmpty() || value.equalsIgnoreCase(property));
        LOGGER.debug("ConditionalOnProperty {} for name='{}', value='{}'; property='{}'", result, name, value, property);
        return result;
    }
}
