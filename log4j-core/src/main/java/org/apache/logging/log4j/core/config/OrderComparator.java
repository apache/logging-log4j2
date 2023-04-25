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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * Comparator for classes annotated with {@link Order}.
 *
 * @since 2.1
 */
public class OrderComparator implements Comparator<Class<?>>, Serializable {

    private static final long serialVersionUID = 1L;
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
        final Order lhsOrder = Objects.requireNonNull(lhs, "lhs").getAnnotation(Order.class);
        final Order rhsOrder = Objects.requireNonNull(rhs, "rhs").getAnnotation(Order.class);
        if (lhsOrder == null && rhsOrder == null) {
            // both unannotated means equal priority
            return 0;
        }
        // if only one class is @Order-annotated, then prefer that one
        if (rhsOrder == null) {
            return -1;
        }
        if (lhsOrder == null) {
            return 1;
        }
        // larger value means higher priority
        return Integer.signum(rhsOrder.value() - lhsOrder.value());
    }
}
