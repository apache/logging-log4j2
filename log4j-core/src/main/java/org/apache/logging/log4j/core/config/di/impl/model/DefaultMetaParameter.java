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

package org.apache.logging.log4j.core.config.di.impl.model;

import org.apache.logging.log4j.core.config.di.api.model.MetaAnnotation;
import org.apache.logging.log4j.core.config.di.api.model.MetaClass;
import org.apache.logging.log4j.core.config.di.api.model.MetaParameter;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;

class DefaultMetaParameter implements MetaParameter {
    private final String name;
    private final MetaClass<?> parameterClass;
    private final String toString;

    DefaultMetaParameter(final Parameter parameter) {
        name = parameter.getName();
        parameterClass = DefaultMetaClass.newMetaClass(
                parameter.getParameterizedType(), parameter.getType(), parameter.getAnnotations());
        toString = parameter.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<MetaAnnotation> getAnnotations() {
        return parameterClass.getAnnotations();
    }

    @Override
    public Type getType() {
        return parameterClass.getType();
    }

    @Override
    public Collection<Type> getTypeClosure() {
        return parameterClass.getTypeClosure();
    }

    @Override
    public String toString() {
        return toString;
    }
}
