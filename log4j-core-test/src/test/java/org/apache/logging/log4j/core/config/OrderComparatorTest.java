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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.plugins.Ordered;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link OrderComparator} with both the legacy {@link Order} annotation and the
 * {@link Ordered} fallback. Both sort in descending order: a larger value means higher priority.
 */
class OrderComparatorTest {

    @Order(5)
    static class LowOrder {}

    @Order(10)
    static class HighOrder {}

    @Ordered(5)
    static class LowOrdered {}

    @Ordered(10)
    static class HighOrdered {}

    static class NoAnnotation {}

    @Test
    void higherOrderValueHasHigherPriority() {
        final List<Class<?>> classes = Arrays.asList(LowOrder.class, HighOrder.class);
        classes.sort(OrderComparator.getInstance());
        assertEquals(HighOrder.class, classes.get(0));
        assertEquals(LowOrder.class, classes.get(1));
    }

    @Test
    void higherOrderedValueHasHigherPriority() {
        final List<Class<?>> classes = Arrays.asList(LowOrdered.class, HighOrdered.class);
        classes.sort(OrderComparator.getInstance());
        assertEquals(HighOrdered.class, classes.get(0));
        assertEquals(LowOrdered.class, classes.get(1));
    }

    @Test
    void annotatedClassesComeBeforeUnannotated() {
        final List<Class<?>> classes = Arrays.asList(NoAnnotation.class, HighOrdered.class);
        classes.sort(OrderComparator.getInstance());
        assertEquals(HighOrdered.class, classes.get(0));
        assertEquals(NoAnnotation.class, classes.get(1));
    }

    @Test
    void unannotatedClassesAreEqual() {
        assertEquals(0, OrderComparator.getInstance().compare(NoAnnotation.class, NoAnnotation.class));
    }
}