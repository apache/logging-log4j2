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
package org.apache.logging.log4j.plugins.util;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Comparator;
import java.util.OptionalInt;
import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.internal.util.AnnotationUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Comparator for annotated elements using {@link Ordered} followed by their element name.
 */
public class OrderedComparator implements Comparator<AnnotatedElement> {
    public static final OrderedComparator INSTANCE = new OrderedComparator();

    @Override
    public int compare(final AnnotatedElement lhs, final AnnotatedElement rhs) {
        if (lhs == rhs) {
            return 0;
        }
        final OptionalInt lhsOrder = AnnotationUtil.getOrder(lhs);
        final OptionalInt rhsOrder = AnnotationUtil.getOrder(rhs);
        if (lhsOrder.isPresent() && rhsOrder.isPresent()) {
            return Integer.compare(lhsOrder.getAsInt(), rhsOrder.getAsInt());
        }
        if (lhsOrder.isPresent()) {
            return -1;
        }
        if (rhsOrder.isPresent()) {
            return 1;
        }
        return getName(lhs).compareToIgnoreCase(getName(rhs));
    }

    private static String getName(final AnnotatedElement element) {
        if (element instanceof Class<?> clazz) {
            return clazz.getName();
        }
        if (element instanceof Field field) {
            return field.getName();
        }
        if (element instanceof Parameter parameter) {
            return parameter.getName();
        }
        if (element instanceof Method method) {
            return method.getName();
        }
        return Strings.EMPTY;
    }
}
