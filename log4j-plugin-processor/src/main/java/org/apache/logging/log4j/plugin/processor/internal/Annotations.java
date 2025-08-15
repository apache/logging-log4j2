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
package org.apache.logging.log4j.plugin.processor.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import org.apache.logging.log4j.plugin.processor.GraalVmProcessor;

public final class Annotations {

    private static final Collection<String> FACTORY_TYPE_NAMES = List.of(
            "org.apache.logging.log4j.plugins.Factory",
            "org.apache.logging.log4j.plugins.PluginFactory",
            "org.apache.logging.log4j.plugins.SingletonFactory",
            "org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory",
            "org.apache.logging.log4j.core.config.plugins.PluginFactory");

    private static final Collection<String> INJECT_NAMES = List.of("org.apache.logging.log4j.plugins.Inject");

    private static final Collection<String> QUALIFIER_TYPE_NAMES = List.of(
            "org.apache.logging.log4j.plugins.Named",
            "org.apache.logging.log4j.plugins.PluginAttribute",
            "org.apache.logging.log4j.plugins.PluginBuilderAttribute",
            "org.apache.logging.log4j.plugins.PluginElement",
            "org.apache.logging.log4j.plugins.PluginNode",
            "org.apache.logging.log4j.plugins.PluginValue",
            "org.apache.logging.log4j.core.config.plugins.PluginAttribute",
            "org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute",
            "org.apache.logging.log4j.core.config.plugins.PluginConfiguration",
            "org.apache.logging.log4j.core.config.plugins.PluginElement",
            "org.apache.logging.log4j.core.config.plugins.PluginLoggerContext",
            "org.apache.logging.log4j.core.config.plugins.PluginNode",
            "org.apache.logging.log4j.core.config.plugins.PluginValue");

    /**
     * These must be public types with either:
     * <ul>
     *     <li>A factory method.</li>
     *     <li>A static method called {@code newInstance}.</li>
     *     <li>A public no-argument constructor.</li>
     * </ul>
     * <p>
     *     <strong>Note:</strong> The annotations listed here must also be declared in
     *     {@link GraalVmProcessor}.
     * </p>
     */
    private static final Collection<String> PLUGIN_ANNOTATION_NAMES =
            List.of("org.apache.logging.log4j.plugins.Plugin", "org.apache.logging.log4j.core.config.plugins.Plugin");

    /**
     * Reflection is also used to create meta annotation strategies.
     * .
     * <p>
     *     <strong>Note:</strong> The annotations listed here must also be declared in
     *     {@link GraalVmProcessor}.
     * </p>
     */
    private static final Collection<String> META_ANNOTATION_STRATEGY_NAMES = List.of(
            "org.apache.logging.log4j.plugins.condition.Conditional",
            "org.apache.logging.log4j.plugins.validation.Constraint");

    public enum Type {
        INJECT,
        /**
         * Annotation used to mark a configuration attribute, element or other injected parameters.
         */
        QUALIFIER,
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
         * {@link org.apache.logging.log4j.plugins.validation.ConstraintValidator}
         * or
         * {@link org.apache.logging.log4j.plugins.condition.Condition}.
         */
        META_ANNOTATION_STRATEGY,
        /**
         * Unknown
         */
        UNKNOWN
    }

    private final Map<TypeElement, Type> typeElementToTypeMap = new HashMap<>();

    public Annotations(final Elements elements) {
        FACTORY_TYPE_NAMES.forEach(className -> addTypeElementIfExists(elements, className, Type.FACTORY));
        INJECT_NAMES.forEach(className -> addTypeElementIfExists(elements, className, Type.INJECT));
        QUALIFIER_TYPE_NAMES.forEach(className -> addTypeElementIfExists(elements, className, Type.QUALIFIER));
        PLUGIN_ANNOTATION_NAMES.forEach(className -> addTypeElementIfExists(elements, className, Type.PLUGIN));
        META_ANNOTATION_STRATEGY_NAMES.forEach(
                className -> addTypeElementIfExists(elements, className, Type.META_ANNOTATION_STRATEGY));
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
