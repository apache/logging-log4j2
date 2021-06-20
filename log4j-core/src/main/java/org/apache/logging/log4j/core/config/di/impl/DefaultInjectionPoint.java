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

package org.apache.logging.log4j.core.config.di.impl;

import org.apache.logging.log4j.core.config.di.Bean;
import org.apache.logging.log4j.core.config.di.InjectionPoint;
import org.apache.logging.log4j.plugins.name.AnnotatedElementAliasesProvider;
import org.apache.logging.log4j.plugins.name.AnnotatedElementNameProvider;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

class DefaultInjectionPoint implements InjectionPoint {
    static DefaultInjectionPoint forField(final Field field, final Bean<?> owner) {
        return new DefaultInjectionPoint(field.getGenericType(),
                AnnotatedElementNameProvider.getName(field),
                AnnotatedElementAliasesProvider.getAliases(field),
                owner, field, field);
    }

    static DefaultInjectionPoint forParameter(final Executable executable, final Parameter parameter, final Bean<?> owner) {
        return new DefaultInjectionPoint(parameter.getParameterizedType(),
                AnnotatedElementNameProvider.getName(parameter),
                AnnotatedElementAliasesProvider.getAliases(parameter),
                owner, executable, parameter);
    }

    private final Type type;
    private final String name;
    private final Collection<String> aliases;
    private final Bean<?> bean;
    private final Member member;
    private final AnnotatedElement element;

    private DefaultInjectionPoint(final Type type, final String name, final Collection<String> aliases,
                                  final Bean<?> bean, final Member member, final AnnotatedElement element) {
        this.type = type;
        this.name = name;
        this.aliases = aliases;
        this.bean = bean;
        this.member = member;
        this.element = element;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<String> getAliases() {
        return aliases;
    }

    @Override
    public Optional<Bean<?>> getBean() {
        return Optional.ofNullable(bean);
    }

    @Override
    public Member getMember() {
        return member;
    }

    @Override
    public AnnotatedElement getElement() {
        return element;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefaultInjectionPoint that = (DefaultInjectionPoint) o;
        return Objects.equals(bean, that.bean) && member.equals(that.member) && element.equals(that.element);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bean, member, element);
    }

    @Override
    public String toString() {
        return "DefaultInjectionPoint{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", aliases=" + aliases +
                ", bean=" + bean +
                ", member=" + member +
                ", element=" + element +
                '}';
    }
}
