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
import org.apache.logging.log4j.plugins.spi.model.MetaMember;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

abstract class AbstractMetaMember<D, T> implements MetaMember<D, T> {
    private final String name;
    private final Collection<Annotation> annotations;
    private final MetaClass<D> declaringClass;
    private final MetaClass<T> type;
    private final boolean isStatic;

    AbstractMetaMember(final MetaClass<D> declaringClass, final Member member, final MetaClass<T> type) {
        this.name = member.getName();
        this.annotations = Arrays.asList(((AnnotatedElement) member).getAnnotations());
        this.declaringClass = declaringClass;
        this.type = type;
        this.isStatic = Modifier.isStatic(member.getModifiers());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return annotations;
    }

    @Override
    public Type getBaseType() {
        return type.getBaseType();
    }

    @Override
    public Collection<Type> getTypeClosure() {
        return type.getTypeClosure();
    }

    @Override
    public MetaClass<T> getType() {
        return type;
    }

    @Override
    public MetaClass<D> getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }
}
