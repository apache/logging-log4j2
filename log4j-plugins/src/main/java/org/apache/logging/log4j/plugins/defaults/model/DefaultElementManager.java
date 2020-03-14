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

import org.apache.logging.log4j.plugins.api.AliasFor;
import org.apache.logging.log4j.plugins.api.Dependent;
import org.apache.logging.log4j.plugins.api.QualifierType;
import org.apache.logging.log4j.plugins.api.ScopeType;
import org.apache.logging.log4j.plugins.spi.bean.Bean;
import org.apache.logging.log4j.plugins.spi.model.ElementManager;
import org.apache.logging.log4j.plugins.spi.model.InjectionPoint;
import org.apache.logging.log4j.plugins.spi.model.MetaClass;
import org.apache.logging.log4j.plugins.spi.model.MetaElement;
import org.apache.logging.log4j.plugins.spi.model.MetaExecutable;
import org.apache.logging.log4j.plugins.spi.model.MetaField;
import org.apache.logging.log4j.plugins.spi.model.MetaMethod;
import org.apache.logging.log4j.plugins.spi.model.MetaParameter;
import org.apache.logging.log4j.plugins.spi.model.Qualifiers;
import org.apache.logging.log4j.plugins.spi.model.Variable;
import org.apache.logging.log4j.plugins.util.Cache;
import org.apache.logging.log4j.plugins.util.WeakCache;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultElementManager implements ElementManager {

    private static final Pattern BEAN_METHOD = Pattern.compile("^(is|get|set)([A-Z].*)$");

    private enum AnnotationType {
        QUALIFIER, SCOPE, UNKNOWN
    }

    private final Cache<Class<?>, MetaClass<?>> classCache = WeakCache.newWeakRefCache(DefaultMetaClass::newMetaClass);

    private final Cache<Class<? extends Annotation>, AnnotationType> annotationTypeCache = WeakCache.newCache(clazz -> {
        for (final Annotation annotation : clazz.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type == AliasFor.class) {
                type = ((AliasFor) annotation).value();
            }
            if (type == QualifierType.class) {
                return AnnotationType.QUALIFIER;
            }
            if (type == ScopeType.class) {
                return AnnotationType.SCOPE;
            }
        }
        return AnnotationType.UNKNOWN;
    });

    @Override
    public <T> MetaClass<T> getMetaClass(final Class<T> clazz) {
        return classCache.get(clazz);
    }

    @Override
    public boolean isQualifierType(final Class<? extends Annotation> annotationType) {
        return getAnnotationType(annotationType) == AnnotationType.QUALIFIER;
    }

    @Override
    public Qualifiers getQualifiers(final MetaElement<?> element) {
        final String elementName = element.getName();
        final String defaultName;
        if (element instanceof MetaMethod<?, ?>) {
            final Matcher matcher = BEAN_METHOD.matcher(elementName);
            if (matcher.matches()) {
                defaultName = Introspector.decapitalize(matcher.group(2));
            } else {
                defaultName = elementName;
            }
        } else if (element instanceof MetaClass<?>) {
            defaultName = Introspector.decapitalize(((MetaClass<?>) element).getJavaClass().getSimpleName());
        } else {
            defaultName = elementName;
        }
        return Qualifiers.fromQualifierAnnotations(filterQualifiers(element.getAnnotations()), defaultName);
    }

    private Collection<Annotation> filterQualifiers(final Collection<Annotation> annotations) {
        final Collection<Annotation> qualifiers = new LinkedHashSet<>(annotations.size());
        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isQualifierType(annotationType)) {
                qualifiers.add(annotation);
            }
        }
        return qualifiers;
    }

    private Class<? extends Annotation> getScopeType(final MetaElement<?> element) {
        final Collection<Class<? extends Annotation>> scopeTypes = filterScopeTypes(element.getAnnotations());
        return scopeTypes.isEmpty() ? Dependent.class : scopeTypes.iterator().next();
    }

    private Collection<Class<? extends Annotation>> filterScopeTypes(final Collection<Annotation> annotations) {
        // only expect at most one scope
        final Collection<Class<? extends Annotation>> scopeTypes = new LinkedHashSet<>(1);
        for (final Annotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isScopeType(annotationType)) {
                scopeTypes.add(annotationType);
            }
        }
        return Collections.unmodifiableCollection(scopeTypes);
    }

    private boolean isScopeType(final Class<? extends Annotation> annotationType) {
        return getAnnotationType(annotationType) == AnnotationType.SCOPE;
    }

    private AnnotationType getAnnotationType(final Class<? extends Annotation> annotationType) {
        return annotationTypeCache.get(annotationType);
    }

    @Override
    public <D, T> InjectionPoint<T> createFieldInjectionPoint(final MetaField<D, T> field, final Bean<D> owner) {
        Objects.requireNonNull(field);
        final Qualifiers qualifiers = getQualifiers(field);
        return new DefaultInjectionPoint<>(field.getType(), qualifiers, owner, field, field);
    }

    @Override
    public <D, P> InjectionPoint<P> createParameterInjectionPoint(final MetaExecutable<D, ?> executable,
                                                                  final MetaParameter<P> parameter,
                                                                  final Bean<D> owner) {
        Objects.requireNonNull(executable);
        Objects.requireNonNull(parameter);
        final Qualifiers qualifiers = getQualifiers(parameter);
        return new DefaultInjectionPoint<>(parameter.getType(), qualifiers, owner, executable, parameter);
    }

    @Override
    public Variable createVariable(final MetaElement<?> element) {
        Objects.requireNonNull(element);
        return createVariable(element, getQualifiers(element));
    }

    @Override
    public Variable createVariable(final InjectionPoint<?> point) {
        Objects.requireNonNull(point);
        return createVariable(point.getElement(), point.getQualifiers());
    }

    private Variable createVariable(final MetaElement<?> element, final Qualifiers qualifiers) {
        final Collection<Type> types = element.getTypeClosure();
        final Class<? extends Annotation> scopeType = getScopeType(element);
        return DefaultVariable.newVariable(types, qualifiers, scopeType);
    }

    @Override
    public void close() {
        classCache.close();
    }
}
