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

import org.apache.logging.log4j.plugins.api.Default;
import org.apache.logging.log4j.plugins.api.Inject;
import org.apache.logging.log4j.plugins.api.Named;
import org.apache.logging.log4j.plugins.api.Produces;
import org.apache.logging.log4j.plugins.api.QualifierType;
import org.apache.logging.log4j.core.config.di.api.bean.Bean;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manages model metadata of program elements.
 */
public interface ElementManager extends AutoCloseable {

    /**
     * Gets metadata about a class.
     */
    <T> MetaClass<T> getMetaClass(final Class<T> clazz);

    /**
     * Indicates if an annotation type is considered a {@linkplain QualifierType qualifier}.
     */
    boolean isQualifierType(Class<? extends Annotation> annotationType);

    /**
     * Extracts the collection of {@linkplain QualifierType qualifiers} from the annotations on an element.
     * If no qualifiers other than {@link Named} are present, then the {@link Default} qualifier is also returned in
     * the collection.
     *
     * @param element program element to extract qualifiers from
     * @return qualifiers present on the element
     */
    Qualifiers getQualifiers(MetaElement element);

    /**
     * Checks if a class has exactly one injectable constructor. A constructor is <i>injectable</i> if:
     * <ol>
     *     <li>it is annotated with {@link Inject}; or</li>
     *     <li>it has as least one parameter annotated with {@link Inject} or a {@linkplain QualifierType qualifier annotation}; or</li>
     *     <li>it is the lone no-arg constructor.</li>
     * </ol>
     *
     * @param type class to find an injectable constructor in
     * @return true if the class has exactly one injectable constructor or false otherwise
     */
    default boolean isInjectable(final MetaClass<?> type) {
        final List<MetaConstructor<?>> injectConstructors = type.getConstructors().stream()
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .collect(Collectors.toList());
        if (injectConstructors.size() > 1) {
            return false;
        }
        if (injectConstructors.size() == 1) {
            return true;
        }
        final List<MetaConstructor<?>> implicitConstructors = type.getConstructors().stream()
                .filter(constructor -> constructor.getParameters().stream().anyMatch(this::isInjectable))
                .collect(Collectors.toList());
        if (implicitConstructors.size() > 1) {
            return false;
        }
        if (implicitConstructors.size() == 1) {
            return true;
        }
        return type.getDefaultConstructor().isPresent();
    }

    /**
     * Checks if an element is injectable. An element is <i>injectable</i> if:
     * <ol>
     *     <li>it is annotated with {@link Inject}; or</li>
     *     <li>it is annotated with a {@linkplain QualifierType qualifier annotation} and is not annotated with {@link Produces}.</li>
     * </ol>
     *
     * @param element field, method, or parameter to check
     * @return true if the element is injectable or false otherwise
     */
    default boolean isInjectable(final MetaElement element) {
        return element.isAnnotationPresent(Inject.class) ||
                (element.getAnnotations().stream().map(MetaAnnotation::getAnnotationType).anyMatch(this::isQualifierType) &&
                        !element.isAnnotationPresent(Produces.class));
    }

    /**
     * Creates an injection point for a field with an optional owning bean.
     *
     * @param field field where injection will take place
     * @param owner bean where field is located or null for static fields
     * @param <D>   bean type
     * @return an injection point describing the field
     */
    <D> InjectionPoint createFieldInjectionPoint(final MetaField<D, ?> field, final Bean<D> owner);

    /**
     * Creates an injection point for a method or constructor parameter with an optional owning bean.
     *
     * @param executable method or constructor where injection will take place
     * @param parameter  which parameter of that executable to create a point at
     * @param owner      bean where executable is located or null for static methods
     * @param <D>        bean type
     * @return an injection point describing the parameter
     */
    <D> InjectionPoint createParameterInjectionPoint(final MetaExecutable<D> executable,
                                                     final MetaParameter parameter, final Bean<D> owner);

    /**
     * Creates a collection of injection points for all the parameters of a method or constructor with an optional
     * owning bean.
     *
     * @param executable method or constructor where injection will take place
     * @param owner      bean where executable is located or null for static methods
     * @param <D>        bean type
     * @return collection of injection points describing the executable parameters
     */
    default <D> Collection<InjectionPoint> createExecutableInjectionPoints(final MetaExecutable<D> executable, final Bean<D> owner) {
        Objects.requireNonNull(executable);
        return executable.getParameters().stream()
                .map(parameter -> createParameterInjectionPoint(executable, parameter, owner))
                .collect(Collectors.toList());
    }

    /**
     * Creates a variable for an element.
     */
    Variable createVariable(final MetaElement element);

    @Override
    void close();
}
