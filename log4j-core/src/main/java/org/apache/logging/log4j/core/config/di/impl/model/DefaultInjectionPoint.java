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

import org.apache.logging.log4j.core.config.di.api.bean.Bean;
import org.apache.logging.log4j.core.config.di.api.model.InjectionPoint;
import org.apache.logging.log4j.core.config.di.api.model.MetaElement;
import org.apache.logging.log4j.core.config.di.api.model.MetaMember;
import org.apache.logging.log4j.core.config.di.api.model.Qualifiers;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

class DefaultInjectionPoint implements InjectionPoint {
    private final Type type;
    private final Qualifiers qualifiers;
    private final Bean<?> bean;
    private final MetaMember<?> member;
    private final MetaElement element;

    DefaultInjectionPoint(final Type type, final Qualifiers qualifiers, final Bean<?> bean,
                          final MetaMember<?> member, final MetaElement element) {
        this.type = type;
        this.qualifiers = qualifiers;
        this.bean = bean;
        this.member = member;
        this.element = element;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Qualifiers getQualifiers() {
        return qualifiers;
    }

    @Override
    public Optional<Bean<?>> getBean() {
        return Optional.ofNullable(bean);
    }

    @Override
    public MetaMember<?> getMember() {
        return member;
    }

    @Override
    public MetaElement getElement() {
        return element;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DefaultInjectionPoint that = (DefaultInjectionPoint) o;
        return qualifiers.equals(that.qualifiers) &&
                Objects.equals(bean, that.bean) &&
                member.equals(that.member) &&
                element.equals(that.element) &&
                type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiers, bean, member, element, type);
    }

    @Override
    public String toString() {
        return "DefaultInjectionPoint{" +
                "type=" + type.getTypeName() +
                ", qualifiers=" + qualifiers +
                ", bean=" + bean +
                ", member=" + member +
                ", element=" + element +
                '}';
    }
}
