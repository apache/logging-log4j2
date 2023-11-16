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
package org.apache.logging.log4j.core.filter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.junit.jupiter.api.Test;

public class CompositeFilterTest {

    @Test
    public void testConcatenation() {
        final Filter a = DenyAllFilter.newBuilder().setOnMatch(Result.ACCEPT).build();
        final Filter b = DenyAllFilter.newBuilder().setOnMatch(Result.NEUTRAL).build();
        final Filter c = DenyAllFilter.newBuilder().setOnMatch(Result.DENY).build();
        // The three values need to be distinguishable
        assertNotEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(b, c);
        final Filter[] expected = new Filter[] {a, b, c};
        final CompositeFilter singleA = CompositeFilter.createFilters(new Filter[] {a});
        final CompositeFilter singleB = CompositeFilter.createFilters(new Filter[] {b});
        final CompositeFilter singleC = CompositeFilter.createFilters(new Filter[] {c});
        // Concatenating one at a time
        final CompositeFilter concat1 = singleA.addFilter(b).addFilter(c);
        assertArrayEquals(expected, concat1.getFiltersArray());
        // In reverse order
        final CompositeFilter concat2 = singleA.addFilter(singleB.addFilter(singleC));
        assertArrayEquals(expected, concat2.getFiltersArray());
    }
}
