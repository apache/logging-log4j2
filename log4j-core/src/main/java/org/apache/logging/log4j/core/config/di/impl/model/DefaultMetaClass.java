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
import org.apache.logging.log4j.core.config.di.api.model.MetaConstructor;
import org.apache.logging.log4j.core.config.di.api.model.MetaField;
import org.apache.logging.log4j.core.config.di.api.model.MetaMethod;
import org.apache.logging.log4j.plugins.util.LazyValue;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.util.Value;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

class DefaultMetaClass<T> implements MetaClass<T> {

    static <T> MetaClass<T> newMetaClass(final Class<T> javaClass) {
        return newMetaClass(javaClass, javaClass, javaClass.getAnnotations());
    }

    static <T> MetaClass<T> newMetaClass(final Type baseType, final Class<T> javaClass, final Annotation... annotations) {
        return new DefaultMetaClass<>(baseType, javaClass, TypeUtil.getTypeClosure(baseType), DefaultMetaAnnotation.fromAnnotations(annotations));
    }

    private final Type baseType;
    private final Class<T> javaClass;
    private final Collection<Type> typeClosure;
    private final Collection<MetaAnnotation> annotations;
    private final Value<Collection<MetaConstructor<T>>> constructors;
    private final Value<Collection<MetaMethod<T, ?>>> methods;
    private final Value<Collection<MetaField<T, ?>>> fields;

    private DefaultMetaClass(final Type baseType, final Class<T> javaClass, final Collection<Type> typeClosure,
                             final Collection<MetaAnnotation> annotations) {
        this.baseType = baseType;
        this.javaClass = javaClass;
        this.typeClosure = typeClosure;
        this.annotations = annotations;
        constructors = LazyValue.forSupplier(() -> Arrays.stream(javaClass.getConstructors())
                .<Constructor<T>>map(TypeUtil::cast)
                .map(this::getMetaConstructor)
                .collect(Collectors.toList()));
        methods = LazyValue.forSupplier(() -> Arrays.stream(javaClass.getMethods())
                .map(this::getMetaMethod)
                .collect(Collectors.toList()));
        fields = LazyValue.forSupplier(() -> TypeUtil.getAllDeclaredFields(javaClass).stream()
                .map(this::getMetaField)
                .collect(Collectors.toList()));
    }

    @Override
    public String getName() {
        return baseType.getTypeName();
    }

    @Override
    public Collection<MetaAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public Type getType() {
        return baseType;
    }

    @Override
    public Collection<Type> getTypeClosure() {
        return typeClosure;
    }

    @Override
    public Class<T> getJavaClass() {
        return javaClass;
    }

    @Override
    public Collection<MetaConstructor<T>> getConstructors() {
        return constructors.get();
    }

    @Override
    public MetaConstructor<T> getMetaConstructor(final Constructor<T> constructor) {
        return new DefaultMetaConstructor<>(this, constructor);
    }

    @Override
    public Optional<MetaConstructor<T>> getDefaultConstructor() {
        try {
            return Optional.of(new DefaultMetaConstructor<>(this, javaClass.getConstructor()));
        } catch (final NoSuchMethodException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public Collection<MetaMethod<T, ?>> getMethods() {
        return methods.get();
    }

    @Override
    public <U> MetaMethod<T, U> getMetaMethod(final Method method) {
        return new DefaultMetaMethod<>(this, method);
    }

    @Override
    public Collection<MetaField<T, ?>> getFields() {
        return fields.get();
    }

    @Override
    public <U> MetaField<T, U> getMetaField(final Field field) {
        return new DefaultMetaField<>(this, field);
    }

    @Override
    public String toString() {
        return baseType.getTypeName();
    }
}
