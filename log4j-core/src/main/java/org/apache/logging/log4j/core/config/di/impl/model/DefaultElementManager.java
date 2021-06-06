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

import org.apache.logging.log4j.plugins.api.AnnotationAlias;
import org.apache.logging.log4j.plugins.api.Default;
import org.apache.logging.log4j.plugins.api.DependentScoped;
import org.apache.logging.log4j.plugins.api.Ignore;
import org.apache.logging.log4j.plugins.api.Named;
import org.apache.logging.log4j.plugins.api.QualifierType;
import org.apache.logging.log4j.plugins.api.ScopeType;
import org.apache.logging.log4j.plugins.internal.util.BeanUtils;
import org.apache.logging.log4j.core.config.di.api.bean.Bean;
import org.apache.logging.log4j.core.config.di.api.model.ElementManager;
import org.apache.logging.log4j.core.config.di.api.model.InjectionPoint;
import org.apache.logging.log4j.core.config.di.api.model.MetaAnnotation;
import org.apache.logging.log4j.core.config.di.api.model.MetaAnnotationElement;
import org.apache.logging.log4j.core.config.di.api.model.MetaClass;
import org.apache.logging.log4j.core.config.di.api.model.MetaElement;
import org.apache.logging.log4j.core.config.di.api.model.MetaExecutable;
import org.apache.logging.log4j.core.config.di.api.model.MetaField;
import org.apache.logging.log4j.core.config.di.api.model.MetaMethod;
import org.apache.logging.log4j.core.config.di.api.model.MetaParameter;
import org.apache.logging.log4j.core.config.di.api.model.Qualifiers;
import org.apache.logging.log4j.core.config.di.api.model.Variable;
import org.apache.logging.log4j.plugins.util.Cache;
import org.apache.logging.log4j.plugins.util.TypeUtil;
import org.apache.logging.log4j.plugins.util.WeakCache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DefaultElementManager implements ElementManager {

    private static final Pattern BEAN_METHOD = Pattern.compile("^(is|get|set)([A-Z].*)$");

    private enum AnnotationType {
        QUALIFIER, SCOPE, UNKNOWN
    }

    private final Cache<Class<?>, MetaClass<?>> classCache = WeakCache.newWeakRefCache(DefaultMetaClass::newMetaClass);

    private final Cache<Class<? extends Annotation>, AnnotationType> annotationTypeCache = WeakCache.newCache(clazz -> {
        for (final Annotation annotation : clazz.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (type == AnnotationAlias.class) {
                type = ((AnnotationAlias) annotation).value();
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
    public Qualifiers getQualifiers(final MetaElement element) {
        final String elementName = element.getName();
        final String defaultNamedValue;
        if (element instanceof MetaMethod<?, ?>) {
            final Matcher matcher = BEAN_METHOD.matcher(elementName);
            if (matcher.matches()) {
                defaultNamedValue = BeanUtils.decapitalize(matcher.group(2));
            } else {
                defaultNamedValue = elementName;
            }
        } else if (element instanceof MetaClass<?>) {
            defaultNamedValue = BeanUtils.decapitalize(((MetaClass<?>) element).getJavaClass().getSimpleName());
        } else {
            defaultNamedValue = elementName;
        }
        final Set<MetaAnnotation> qualifiers = element.getAnnotations().stream()
                .filter(annotation -> isQualifierType(annotation.getAnnotationType()))
                .map(annotation -> transformQualifier(annotation, defaultNamedValue))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (qualifiers.stream().map(MetaAnnotation::getAnnotationType).noneMatch(type -> type != Named.class)) {
            qualifiers.add(new DefaultMetaAnnotation(Default.class, Collections.emptySet()));
        }
        return Qualifiers.fromAnnotations(qualifiers);
    }

    private static MetaAnnotation transformQualifier(final MetaAnnotation annotation, final String defaultNamedValue) {
        final Class<? extends Annotation> annotationType = annotation.getAnnotationType();
        final Set<MetaAnnotationElement<?>> elements = annotation.getAnnotationElements().stream()
                .filter(element -> !element.isAnnotationPresent(Ignore.class))
                .map(element -> {
                    if (annotationType == Named.class && element.getName().equals("value")) {
                        final MetaAnnotationElement<String> namedValue = TypeUtil.cast(element);
                        final String value = namedValue.getValue();
                        if (value.isEmpty()) {
                            return namedValue.withNewValue(defaultNamedValue);
                        } else {
                            return namedValue;
                        }
                    } else {
                        return element;
                    }
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new DefaultMetaAnnotation(annotationType, elements);
    }

    private Class<? extends Annotation> getScopeType(final MetaElement element) {
        final Collection<Class<? extends Annotation>> scopeTypes = filterScopeTypes(element.getAnnotations());
        return scopeTypes.isEmpty() ? DependentScoped.class : scopeTypes.iterator().next();
    }

    private Collection<Class<? extends Annotation>> filterScopeTypes(final Collection<MetaAnnotation> annotations) {
        // only expect at most one scope
        final Collection<Class<? extends Annotation>> scopeTypes = new LinkedHashSet<>(1);
        for (final MetaAnnotation annotation : annotations) {
            final Class<? extends Annotation> annotationType = annotation.getAnnotationType();
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
    public <D> InjectionPoint createFieldInjectionPoint(final MetaField<D, ?> field, final Bean<D> owner) {
        Objects.requireNonNull(field);
        final Qualifiers qualifiers = getQualifiers(field);
        return new DefaultInjectionPoint(field.getType(), qualifiers, owner, field, field);
    }

    @Override
    public <D> InjectionPoint createParameterInjectionPoint(final MetaExecutable<D> executable,
                                                            final MetaParameter parameter,
                                                            final Bean<D> owner) {
        Objects.requireNonNull(executable);
        Objects.requireNonNull(parameter);
        final Qualifiers qualifiers = getQualifiers(parameter);
        return new DefaultInjectionPoint(parameter.getType(), qualifiers, owner, executable, parameter);
    }

    @Override
    public Variable createVariable(final MetaElement element) {
        Objects.requireNonNull(element);
        return createVariable(element, getQualifiers(element));
    }

    private Variable createVariable(final MetaElement element, final Qualifiers qualifiers) {
        final Collection<Type> types = element.getTypeClosure();
        final Class<? extends Annotation> scopeType = getScopeType(element);
        return new DefaultVariable(types, qualifiers, scopeType);
    }

    @Override
    public void close() {
        classCache.close();
    }
}
