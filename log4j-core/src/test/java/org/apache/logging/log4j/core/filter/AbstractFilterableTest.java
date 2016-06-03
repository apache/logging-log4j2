/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.logging.log4j.core.filter;

import static org.junit.Assert.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.junit.Before;
import org.junit.Test;

public class AbstractFilterableTest {

    MockedAbstractFilterable filterable;

    @Before
    public void setup() {
        filterable = new MockedAbstractFilterable();
    }

    @Test
    public void testAddSimpleFilter() throws Exception {
        Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        assertSame(filter, filterable.getFilter());
    }

    @Test
    public void testAddMultipleSimpleFilters() throws Exception {
        Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        assertSame(filter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(filter);
        assertTrue(filterable.getFilter() instanceof CompositeFilter);
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    public void testAddMultipleEqualSimpleFilter() throws Exception {
        Filter filter = new EqualFilter("test");

        filterable.addFilter(filter);
        assertSame(filter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(filter);
        assertTrue(filterable.getFilter() instanceof CompositeFilter);
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    public void testAddCompositeFilter() throws Exception {
        Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter compositeFilter = CompositeFilter.createFilters(new Filter[] {filter1, filter2});

        filterable.addFilter(compositeFilter);
        assertSame(compositeFilter, filterable.getFilter());
    }

    @Test
    public void testAddMultipleCompositeFilters() throws Exception {
        Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter filter3 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2, filter3});

        filterable.addFilter(compositeFilter);
        assertSame(compositeFilter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(compositeFilter);
        assertTrue(filterable.getFilter() instanceof CompositeFilter);
        assertEquals(6, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    public void testAddSimpleFilterAndCompositeFilter() throws Exception {
        Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter notInCompositeFilterFilter = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});

        filterable.addFilter(notInCompositeFilterFilter);
        assertSame(notInCompositeFilterFilter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(compositeFilter);
        assertTrue(filterable.getFilter() instanceof CompositeFilter);
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    public void testAddCompositeFilterAndSimpleFilter() throws Exception {
        Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter notInCompositeFilterFilter = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});

        filterable.addFilter(compositeFilter);
        assertSame(compositeFilter, filterable.getFilter());
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(notInCompositeFilterFilter);
        assertTrue(filterable.getFilter() instanceof CompositeFilter);
        assertEquals(3, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    public void testRemoveSimpleFilterFromSimpleFilter() throws Exception {
        Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        filterable.removeFilter(filter);
        assertNull(filterable.getFilter());
    }

    @Test
    public void testRemoveSimpleEqualFilterFromSimpleFilter() throws Exception {
        Filter filterOriginal = new EqualFilter("test");
        Filter filterCopy = new EqualFilter("test");

        filterable.addFilter(filterOriginal);
        filterable.removeFilter(filterCopy);
        assertNull(filterable.getFilter());
    }

    @Test
    public void testRemoveSimpleEqualFilterFromTwoSimpleFilters() throws Exception {
        Filter filterOriginal = new EqualFilter("test");
        Filter filterCopy = new EqualFilter("test");

        filterable.addFilter(filterOriginal);
        filterable.addFilter(filterOriginal);
        filterable.removeFilter(filterCopy);
        assertSame(filterOriginal, filterable.getFilter());
        filterable.removeFilter(filterCopy);
        assertNull(filterable.getFilter());
    }

    @Test
    public void testRemoveSimpleEqualFilterFromMultipleSimpleFilters() throws Exception {
        Filter filterOriginal = new EqualFilter("test");
        Filter filterCopy = new EqualFilter("test");

        filterable.addFilter(filterOriginal);
        filterable.addFilter(filterOriginal);
        filterable.addFilter(filterCopy);
        filterable.removeFilter(filterCopy);
        assertTrue(filterable.getFilter() instanceof CompositeFilter);
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
        filterable.removeFilter(filterCopy);
        assertEquals(filterOriginal, filterable.getFilter());
        filterable.removeFilter(filterOriginal);
        assertNull(filterable.getFilter());
    }

    @Test
    public void testRemoveNullFromSingleSimpleFilter() throws Exception {
        Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        filterable.removeFilter(null);
        assertSame(filter, filterable.getFilter());
    }


    @Test
    public void testRemoveNonExistingFilterFromSingleSimpleFilter() throws Exception {
        Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter newFilter = ThresholdFilter.createFilter(Level.WARN, null, null);

        filterable.addFilter(filter);
        filterable.removeFilter(newFilter);
        assertSame(filter, filterable.getFilter());
    }

    @Test
    public void testRemoveSimpleFilterFromCompositeFilter() {
        Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});

        filterable.addFilter(compositeFilter);

        // should remove internal filter of compositeFilter
        filterable.removeFilter(filter1);
        assertFalse(filterable.getFilter() instanceof CompositeFilter);

        assertEquals(filter2, filterable.getFilter());
    }

    @Test
    public void testRemoveSimpleFilterFromCompositeAndSimpleFilter() {
        Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});
        Filter anotherFilter = ThresholdFilter.createFilter(Level.WARN, null, null);


        filterable.addFilter(compositeFilter);
        filterable.addFilter(anotherFilter);

        // should not remove internal filter of compositeFilter
        filterable.removeFilter(anotherFilter);
        assertTrue(filterable.getFilter() instanceof CompositeFilter);
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
    }

    @Test
    public void testRemoveCompositeFilterFromCompositeFilter() {
        Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});

        filterable.addFilter(compositeFilter);
        filterable.removeFilter(compositeFilter);
        assertNull(filterable.getFilter());
    }

    @Test
    public void testRemoveFiltersFromComposite() {
        Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});
        Filter anotherFilter = ThresholdFilter.createFilter(Level.WARN, null, null);

        filterable.addFilter(compositeFilter);
        filterable.addFilter(anotherFilter);
        assertEquals(3, ((CompositeFilter) filterable.getFilter()).getFilters().size());
        filterable.removeFilter(filter1);
        assertEquals(2, ((CompositeFilter) filterable.getFilter()).getFilters().size());
        filterable.removeFilter(filter2);
        assertSame(anotherFilter, filterable.getFilter());
    }

    private class MockedAbstractFilterable extends AbstractFilterable {}

    private class EqualFilter extends AbstractFilter {
        private final String key;
        public EqualFilter(final String key) {
            this.key = key;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof EqualFilter)) return false;

            EqualFilter that = (EqualFilter) o;

            if (key != null ? !key.equals(that.key) : that.key != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return key != null ? key.hashCode() : 0;
        }
    }
}
