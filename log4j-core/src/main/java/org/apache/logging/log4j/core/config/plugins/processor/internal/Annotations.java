/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.config.plugins.processor.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

public final class Annotations {

    /**
     * These are fields, methods or parameters that correspond to Log4j configuration attributes, elements, and other
     * injected elements.
     * <p>
     *     <strong>Note:</strong> The annotations listed here must also be declared in
     *     {@link org.apache.logging.log4j.core.config.plugins.processor.GraalVmProcessor}.
     * </p>
     */
    private static final Collection<String> PARAMETER_ANNOTATION_NAMES = Arrays.asList(
            "org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute",
            "org.apache.logging.log4j.core.config.plugins.PluginConfiguration",
            "org.apache.logging.log4j.core.config.plugins.PluginElement",
            "org.apache.logging.log4j.core.config.plugins.PluginLoggerContext",
            "org.apache.logging.log4j.core.config.plugins.PluginNode",
            "org.apache.logging.log4j.core.config.plugins.PluginValue");
    /**
     * These are static methods that must be reachable through reflection.
     * <p>
     *     <strong>Note:</strong> The annotations listed here must also be declared in
     *     {@link org.apache.logging.log4j.core.config.plugins.processor.GraalVmProcessor}.
     * </p>
     */
    private static final Collection<String> FACTORY_ANNOTATION_NAMES = Arrays.asList(
            "org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory",
            "org.apache.logging.log4j.core.config.plugins.PluginFactory");
    /**
     * These must be public types with either:
     * <ul>
     *     <li>A factory method.</li>
     *     <li>A static method called {@code newInstance}.</li>
     *     <li>A public no-argument constructor.</li>
     * </ul>
     * <p>
     *     <strong>Note:</strong> The annotations listed here must also be declared in
     *     {@link org.apache.logging.log4j.core.config.plugins.processor.GraalVmProcessor}.
     * </p>
     */
    private static final Collection<String> PLUGIN_ANNOTATION_NAMES =
            Collections.singletonList("org.apache.logging.log4j.core.config.plugins.Plugin");

    /**
     * Reflection is also used to create constraint validators and plugin visitors.
     * <p>
     *     <strong>Note:</strong> The annotations listed here must also be declared in
     *     {@link org.apache.logging.log4j.core.config.plugins.processor.GraalVmProcessor}.
     * </p>
     */
    private static final Collection<String> CONSTRAINT_OR_VISITOR_ANNOTATION_NAMES = Arrays.asList(
            "org.apache.logging.log4j.core.config.plugins.validation.Constraint",
            "org.apache.logging.log4j.core.config.plugins.PluginVisitorStrategy");

    public enum Type {
        /**
         * Annotation used to mark a configuration attribute, element or other injected parameters.
         */
        PARAMETER,
        /**
         * Annotation used to mark a Log4j Plugin factory method.
         */
        FACTORY,
        /**
         * Annotation used to mark a Log4j Plugin class.
         */
        PLUGIN,
        /**
         * Annotation containing the name of a
         * {@link org.apache.logging.log4j.core.config.plugins.validation.ConstraintValidator}
         * or
         * {@link org.apache.logging.log4j.core.config.plugins.visitors.PluginVisitor}.
         */
        CONSTRAINT_OR_VISITOR,
        /**
         * Unknown
         */
        UNKNOWN
    }

    private final Map<TypeElement, Type> typeElementToTypeMap = new HashMap<>();

    public Annotations(final Elements elements) {
        PARAMETER_ANNOTATION_NAMES.forEach(className -> addTypeElementIfExists(elements, className, Type.PARAMETER));
        FACTORY_ANNOTATION_NAMES.forEach(className -> addTypeElementIfExists(elements, className, Type.FACTORY));
        PLUGIN_ANNOTATION_NAMES.forEach(className -> addTypeElementIfExists(elements, className, Type.PLUGIN));
        CONSTRAINT_OR_VISITOR_ANNOTATION_NAMES.forEach(
                className -> addTypeElementIfExists(elements, className, Type.CONSTRAINT_OR_VISITOR));
    }

    private void addTypeElementIfExists(Elements elements, CharSequence className, Type type) {
        final TypeElement element = elements.getTypeElement(className);
        if (element != null) {
            typeElementToTypeMap.put(element, type);
        }
    }

    public Annotations.Type classifyAnnotation(TypeElement element) {
        return typeElementToTypeMap.getOrDefault(element, Type.UNKNOWN);
    }

    public Element getAnnotationClassValue(Element element, TypeElement annotation) {
        // This prevents getting an "Attempt to access Class object for TypeMirror" exception
        AnnotationMirror annotationMirror = element.getAnnotationMirrors().stream()
                .filter(am -> am.getAnnotationType().asElement().equals(annotation))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No `@" + annotation + "` annotation found on " + element));
        AnnotationValue annotationValue = annotationMirror.getElementValues().entrySet().stream()
                .filter(e -> "value".equals(e.getKey().getSimpleName().toString()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No `value` found `@" + annotation + "` annotation on " + element));
        DeclaredType value = (DeclaredType) annotationValue.getValue();
        return value.asElement();
    }
}
