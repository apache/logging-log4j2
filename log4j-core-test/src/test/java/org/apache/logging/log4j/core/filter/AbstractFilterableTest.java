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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractFilterableTest {

    MockedAbstractFilterable filterable;

    @BeforeEach
    void setup() {
        filterable = new MockedAbstractFilterable();
    }

    @Test
    void testAddSimpleFilter() {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        assertSame(filter, filterable.getFilter());
    }

    @Test
    void testAddMultipleSimpleFilters() {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        assertSame(filter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(filter);
        assertInstanceOf(CompositeFilter.class, filterable.getFilter());
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    void testAddMultipleEqualSimpleFilter() {
        final Filter filter = new EqualFilter("test");

        filterable.addFilter(filter);
        assertSame(filter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(filter);
        assertInstanceOf(CompositeFilter.class, filterable.getFilter());
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    void testAddCompositeFilter() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(filter1, filter2);

        filterable.addFilter(compositeFilter);
        assertSame(compositeFilter, filterable.getFilter());
    }

    @Test
    void testAddMultipleCompositeFilters() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter3 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(filter1, filter2, filter3);

        filterable.addFilter(compositeFilter);
        assertSame(compositeFilter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(compositeFilter);
        assertInstanceOf(CompositeFilter.class, filterable.getFilter());
        assertEquals(6, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    void testAddSimpleFilterAndCompositeFilter() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter notInCompositeFilterFilter = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(filter1, filter2);

        filterable.addFilter(notInCompositeFilterFilter);
        assertSame(notInCompositeFilterFilter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(compositeFilter);
        assertInstanceOf(CompositeFilter.class, filterable.getFilter());
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    void testAddCompositeFilterAndSimpleFilter() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter notInCompositeFilterFilter = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(filter1, filter2);

        filterable.addFilter(compositeFilter);
        assertSame(compositeFilter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(notInCompositeFilterFilter);
        assertInstanceOf(CompositeFilter.class, filterable.getFilter());
        assertEquals(3, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    void testRemoveSimpleFilterFromSimpleFilter() {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        filterable.removeFilter(filter);
        assertNull(filterable.getFilter());
    }

    @Test
    void testRemoveSimpleEqualFilterFromSimpleFilter() {
        final Filter filterOriginal = new EqualFilter("test");
        final Filter filterCopy = new EqualFilter("test");

        filterable.addFilter(filterOriginal);
        filterable.removeFilter(filterCopy);
        assertNull(filterable.getFilter());
    }

    @Test
    void testRemoveSimpleEqualFilterFromTwoSimpleFilters() {
        final Filter filterOriginal = new EqualFilter("test");
        final Filter filterCopy = new EqualFilter("test");

        filterable.addFilter(filterOriginal);
        filterable.addFilter(filterOriginal);
        filterable.removeFilter(filterCopy);
        assertSame(filterOriginal, filterable.getFilter());
        filterable.removeFilter(filterCopy);
        assertNull(filterable.getFilter());
    }

    @Test
    void testRemoveSimpleEqualFilterFromMultipleSimpleFilters() {
        final Filter filterOriginal = new EqualFilter("test");
        final Filter filterCopy = new EqualFilter("test");

        filterable.addFilter(filterOriginal);
        filterable.addFilter(filterOriginal);
        filterable.addFilter(filterCopy);
        filterable.removeFilter(filterCopy);
        assertInstanceOf(CompositeFilter.class, filterable.getFilter());
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
        filterable.removeFilter(filterCopy);
        assertEquals(filterOriginal, filterable.getFilter());
        filterable.removeFilter(filterOriginal);
        assertNull(filterable.getFilter());
    }

    @Test
    void testRemoveNullFromSingleSimpleFilter() {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        filterable.removeFilter(null);
        assertSame(filter, filterable.getFilter());
    }

    @Test
    void testRemoveNonExistingFilterFromSingleSimpleFilter() {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter newFilter = ThresholdFilter.createFilter(Level.WARN, null, null);

        filterable.addFilter(filter);
        filterable.removeFilter(newFilter);
        assertSame(filter, filterable.getFilter());
    }

    @Test
    void testRemoveSimpleFilterFromCompositeFilter() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(filter1, filter2);

        filterable.addFilter(compositeFilter);

        // should remove internal filter of compositeFilter
        filterable.removeFilter(filter1);
        assertFalse(filterable.getFilter() instanceof CompositeFilter);

        assertEquals(filter2, filterable.getFilter());
    }

    @Test
    void testRemoveSimpleFilterFromCompositeAndSimpleFilter() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(filter1, filter2);
        final Filter anotherFilter = ThresholdFilter.createFilter(Level.WARN, null, null);

        filterable.addFilter(compositeFilter);
        filterable.addFilter(anotherFilter);

        // should not remove internal filter of compositeFilter
        filterable.removeFilter(anotherFilter);
        assertInstanceOf(CompositeFilter.class, filterable.getFilter());
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    void testRemoveCompositeFilterFromCompositeFilter() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(filter1, filter2);

        filterable.addFilter(compositeFilter);
        filterable.removeFilter(compositeFilter);
        assertNull(filterable.getFilter());
    }

    @Test
    void testRemoveFiltersFromComposite() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(filter1, filter2);
        final Filter anotherFilter = ThresholdFilter.createFilter(Level.WARN, null, null);

        filterable.addFilter(compositeFilter);
        filterable.addFilter(anotherFilter);
        assertEquals(3, ((CompositeFilter) filterable.getFilter()).getFilters().size());
        filterable.removeFilter(filter1);
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
        filterable.removeFilter(filter2);
        assertSame(anotherFilter, filterable.getFilter());
    }

    private static class MockedAbstractFilterable extends AbstractFilterable {}

    private static class EqualFilter extends AbstractFilter {
        private final String key;

        public EqualFilter(final String key) {
            this.key = key;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EqualFilter)) {
                return false;
            }

            final EqualFilter that = (EqualFilter) o;

            if (key != null ? !key.equals(that.key) : that.key != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return key != null ? key.hashCode() : 0;
        }
    }
}
