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

package org.apache.logging.log4j.plugins.defaults.model;

import org.apache.logging.log4j.plugins.spi.model.MetaClass;
import org.apache.logging.log4j.plugins.spi.model.MetaParameter;
import org.apache.logging.log4j.plugins.util.TypeUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;

class DefaultMetaParameter<T> implements MetaParameter<T> {
    private final String name;
    private final MetaClass<T> parameterClass;
    private final String toString;

    DefaultMetaParameter(final Parameter parameter) {
        name = parameter.getName();
        final Type type = parameter.getParameterizedType();
        final Class<T> javaClass = TypeUtil.cast(parameter.getType());
        parameterClass = DefaultMetaClass.newMetaClass(type, javaClass, parameter.getAnnotations());
        toString = parameter.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return parameterClass.getAnnotations();
    }

    @Override
    public Type getBaseType() {
        return parameterClass.getBaseType();
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
