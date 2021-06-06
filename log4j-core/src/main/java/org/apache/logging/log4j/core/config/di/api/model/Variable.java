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

package org.apache.logging.log4j.core.config.di.api.model;

import org.apache.logging.log4j.plugins.api.DependentScoped;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

public interface Variable {
    Collection<Type> getTypes();

    default boolean hasMatchingType(final Type requiredType) {
        for (final Type type : getTypes()) {
            if (TypeUtil.typesMatch(requiredType, type)) {
                return true;
            }
        }
        return false;
    }

    Qualifiers getQualifiers();

    Class<? extends Annotation> getScopeType();

    default boolean isDependentScoped() {
        return getScopeType() == DependentScoped.class;
    }
}
