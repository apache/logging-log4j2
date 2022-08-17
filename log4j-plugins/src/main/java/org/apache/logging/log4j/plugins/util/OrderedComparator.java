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

package org.apache.logging.log4j.plugins.util;

import org.apache.logging.log4j.plugins.Ordered;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Comparator;

/**
 * Comparator for annotated elements using {@link Ordered} followed by their name from {@link Keys#getName}.
 */
public class OrderedComparator implements Comparator<AnnotatedElement> {
    public static final OrderedComparator INSTANCE = new OrderedComparator();

    @Override
    public int compare(final AnnotatedElement lhs, final AnnotatedElement rhs) {
        if (lhs == rhs) {
            return 0;
        }
        final Ordered lhsOrder = lhs.getAnnotation(Ordered.class);
        final Ordered rhsOrder = rhs.getAnnotation(Ordered.class);
        if (lhsOrder != null && rhsOrder != null) {
            return Integer.compare(lhsOrder.value(), rhsOrder.value());
        } else if (lhsOrder != null) {
            return -1;
        } else if (rhsOrder != null) {
            return 1;
        } else {
            return getName(lhs).compareToIgnoreCase(getName(rhs));
        }
    }

    private static String getName(final AnnotatedElement element) {
        if (element instanceof Class<?>) {
            return Keys.getName((Class<?>) element);
        }
        if (element instanceof Field) {
            return Keys.getName((Field) element);
        }
        if (element instanceof Parameter) {
            return Keys.getName((Parameter) element);
        }
        if (element instanceof Method) {
            return Keys.getName((Method) element);
        }
        if (element instanceof AnnotatedType) {
            return Keys.getName((AnnotatedType) element);
        }
        return Strings.EMPTY;
    }
}
