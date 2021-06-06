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
import org.apache.logging.log4j.core.config.di.api.model.MetaMember;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;

abstract class AbstractMetaMember<D, T> implements MetaMember<D> {
    private final String name;
    private final Collection<MetaAnnotation> annotations;
    private final MetaClass<D> declaringClass;
    private final MetaClass<T> type;
    private final boolean isStatic;

    AbstractMetaMember(final MetaClass<D> declaringClass, final Member member, final MetaClass<T> type) {
        this.name = member.getName();
        this.annotations = DefaultMetaAnnotation.fromAnnotations(((AnnotatedElement) member).getAnnotations());
        this.declaringClass = declaringClass;
        this.type = type;
        this.isStatic = Modifier.isStatic(member.getModifiers());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<MetaAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public Type getType() {
        return type.getType();
    }

    @Override
    public Collection<Type> getTypeClosure() {
        return type.getTypeClosure();
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
