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

package org.apache.logging.log4j.plugins;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Comparator;

/**
 * Specifies the order in which the annotated class should be considered in when processing plugins.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginOrder {
    int value();

    int FIRST = Integer.MIN_VALUE;

    int LAST = Integer.MAX_VALUE;

    Comparator<Class<?>> COMPARATOR = (lhs, rhs) -> {
        if (lhs == rhs) {
            return 0;
        }
        final PluginOrder lhsOrder = lhs.getAnnotation(PluginOrder.class);
        final PluginOrder rhsOrder = rhs.getAnnotation(PluginOrder.class);
        if (lhsOrder != null && rhsOrder != null) {
            return Integer.compare(lhsOrder.value(), rhsOrder.value());
        } else if (lhsOrder != null) {
            return -1;
        } else if (rhsOrder != null) {
            return 1;
        } else {
            return lhs.getName().compareTo(rhs.getName());
        }
    };
}
