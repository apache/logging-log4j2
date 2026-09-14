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
package org.apache.logging.log4j.core.config;

import java.util.Comparator;
import java.util.Objects;
import java.util.OptionalInt;
import org.apache.logging.log4j.plugins.internal.util.AnnotationUtil;

/**
 * Comparator for classes annotated with {@link Order} or {@link org.apache.logging.log4j.plugins.Ordered}.
 *
 * @since 2.1
 */
public class OrderComparator implements Comparator<Class<?>> {

    private static final Comparator<Class<?>> INSTANCE = new OrderComparator();

    /**
     * Returns a singleton instance of this class.
     *
     * @return the singleton for this class.
     */
    public static Comparator<Class<?>> getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(final Class<?> lhs, final Class<?> rhs) {
        Objects.requireNonNull(lhs, "lhs");
        Objects.requireNonNull(rhs, "rhs");
        final OptionalInt lhsOrder = getOrder(lhs);
        final OptionalInt rhsOrder = getOrder(rhs);
        if (lhsOrder.isEmpty() && rhsOrder.isEmpty()) {
            // both unannotated means equal priority
            return 0;
        }
        // if only one class is annotated, then prefer that one
        if (rhsOrder.isEmpty()) {
            return -1;
        }
        if (lhsOrder.isEmpty()) {
            return 1;
        }
        // larger value means higher priority (descending order)
        return Integer.signum(rhsOrder.getAsInt() - lhsOrder.getAsInt());
    }

    private static OptionalInt getOrder(final Class<?> clazz) {
        // Check for legacy @Order annotation first
        final Order order = clazz.getAnnotation(Order.class);
        if (order != null) {
            return OptionalInt.of(order.value());
        }
        // Fall back to @Ordered via AnnotationUtil
        return AnnotationUtil.getOrder(clazz);
    }
}
