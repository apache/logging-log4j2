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

package org.apache.logging.log4j.plugins.visit;

import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.Key;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * Visitor of {@link Node} instances during configuration injection.
 *
 * @since 3.0.0
 */
public interface NodeVisitor {

    /**
     * Visits a field with a given node and returns the value to be used for field injection.
     *
     * @param field    annotated field to get an injectable value for
     * @param node     configuration node where the field is being visited
     * @param debugLog value for appending debug info about this field
     * @return the value to inject in the field or null if nothing should be changed in the field
     */
    Object visitField(final Field field, final Node node, final StringBuilder debugLog);

    /**
     * Visits a parameter with a given node and returns the value to be used for parameter injection.
     *
     * @param parameter annotated parameter to get an injectable value for
     * @param node      configuration node where the parameter is being visited
     * @param debugLog  value for appending debug info about this parameter
     * @return the value to inject in the parameter
     */
    Object visitParameter(final Parameter parameter, final Node node, final StringBuilder debugLog);

    /**
     * Meta annotation to configure how an annotation should be handled during configuration injection.
     *
     * @see org.apache.logging.log4j.plugins.di.Injector#configure(Node)
     */
    @Documented
    @Target(ElementType.ANNOTATION_TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Kind {
        /**
         * Implementation class of {@link NodeVisitor} to use for the annotation.
         */
        Class<? extends NodeVisitor> value();
    }

    /**
     * Returns the Key corresponding to the {@link Kind} of {@link NodeVisitor} the annotated element should use or
     * {@code null} if none are found.
     */
    static Key<? extends NodeVisitor> keyFor(final AnnotatedElement element) {
        for (final Annotation annotation : element.getAnnotations()) {
            final NodeVisitor.Kind kind = annotation.annotationType().getAnnotation(NodeVisitor.Kind.class);
            if (kind != null) {
                return Key.forClass(kind.value());
            }
        }
        return null;
    }
}
