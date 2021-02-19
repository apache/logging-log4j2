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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AbstractFilterableTest {

    MockedAbstractFilterable filterable;

    @BeforeEach
    public void setup() {
        filterable = new MockedAbstractFilterable();
    }

    @Test
    public void testAddSimpleFilter() throws Exception {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        assertThat(filterable.getFilter()).isSameAs(filter);
    }

    @Test
    public void testAddMultipleSimpleFilters() throws Exception {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        assertThat(filterable.getFilter()).isSameAs(filter);
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(filter);
        assertThat(filterable.getFilter() instanceof CompositeFilter).isTrue();
        assertThat(((CompositeFilter) filterable.getFilter()).getFiltersArray().length).isEqualTo(2);
    }

    @Test
    public void testAddMultipleEqualSimpleFilter() throws Exception {
        final Filter filter = new EqualFilter("test");

        filterable.addFilter(filter);
        assertThat(filterable.getFilter()).isSameAs(filter);
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(filter);
        assertThat(filterable.getFilter() instanceof CompositeFilter).isTrue();
        assertThat(((CompositeFilter) filterable.getFilter()).getFiltersArray().length).isEqualTo(2);
    }

    @Test
    public void testAddCompositeFilter() throws Exception {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(new Filter[] {filter1, filter2});

        filterable.addFilter(compositeFilter);
        assertThat(filterable.getFilter()).isSameAs(compositeFilter);
    }

    @Test
    public void testAddMultipleCompositeFilters() throws Exception {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter3 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2, filter3});

        filterable.addFilter(compositeFilter);
        assertThat(filterable.getFilter()).isSameAs(compositeFilter);
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(compositeFilter);
        assertThat(filterable.getFilter() instanceof CompositeFilter).isTrue();
        assertThat(((CompositeFilter) filterable.getFilter()).getFiltersArray().length).isEqualTo(6);
    }

    @Test
    public void testAddSimpleFilterAndCompositeFilter() throws Exception {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter notInCompositeFilterFilter = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});

        filterable.addFilter(notInCompositeFilterFilter);
        assertThat(filterable.getFilter()).isSameAs(notInCompositeFilterFilter);
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(compositeFilter);
        assertThat(filterable.getFilter() instanceof CompositeFilter).isTrue();
        assertThat(((CompositeFilter) filterable.getFilter()).getFiltersArray().length).isEqualTo(2);
    }

    @Test
    public void testAddCompositeFilterAndSimpleFilter() throws Exception {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter notInCompositeFilterFilter = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});

        filterable.addFilter(compositeFilter);
        assertThat(filterable.getFilter()).isSameAs(compositeFilter);
        // adding a second filter converts the filter
        // into a CompositeFilter.class
        filterable.addFilter(notInCompositeFilterFilter);
        assertThat(filterable.getFilter() instanceof CompositeFilter).isTrue();
        assertThat(((CompositeFilter) filterable.getFilter()).getFiltersArray().length).isEqualTo(3);
    }

    @Test
    public void testRemoveSimpleFilterFromSimpleFilter() throws Exception {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        filterable.removeFilter(filter);
        assertThat(filterable.getFilter()).isNull();
    }

    @Test
    public void testRemoveSimpleEqualFilterFromSimpleFilter() throws Exception {
        final Filter filterOriginal = new EqualFilter("test");
        final Filter filterCopy = new EqualFilter("test");

        filterable.addFilter(filterOriginal);
        filterable.removeFilter(filterCopy);
        assertThat(filterable.getFilter()).isNull();
    }

    @Test
    public void testRemoveSimpleEqualFilterFromTwoSimpleFilters() throws Exception {
        final Filter filterOriginal = new EqualFilter("test");
        final Filter filterCopy = new EqualFilter("test");

        filterable.addFilter(filterOriginal);
        filterable.addFilter(filterOriginal);
        filterable.removeFilter(filterCopy);
        assertThat(filterable.getFilter()).isSameAs(filterOriginal);
        filterable.removeFilter(filterCopy);
        assertThat(filterable.getFilter()).isNull();
    }

    @Test
    public void testRemoveSimpleEqualFilterFromMultipleSimpleFilters() throws Exception {
        final Filter filterOriginal = new EqualFilter("test");
        final Filter filterCopy = new EqualFilter("test");

        filterable.addFilter(filterOriginal);
        filterable.addFilter(filterOriginal);
        filterable.addFilter(filterCopy);
        filterable.removeFilter(filterCopy);
        assertThat(filterable.getFilter() instanceof CompositeFilter).isTrue();
        assertThat(((CompositeFilter) filterable.getFilter()).getFiltersArray().length).isEqualTo(2);
        filterable.removeFilter(filterCopy);
        assertThat(filterable.getFilter()).isEqualTo(filterOriginal);
        filterable.removeFilter(filterOriginal);
        assertThat(filterable.getFilter()).isNull();
    }

    @Test
    public void testRemoveNullFromSingleSimpleFilter() throws Exception {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);

        filterable.addFilter(filter);
        filterable.removeFilter(null);
        assertThat(filterable.getFilter()).isSameAs(filter);
    }


    @Test
    public void testRemoveNonExistingFilterFromSingleSimpleFilter() throws Exception {
        final Filter filter = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter newFilter = ThresholdFilter.createFilter(Level.WARN, null, null);

        filterable.addFilter(filter);
        filterable.removeFilter(newFilter);
        assertThat(filterable.getFilter()).isSameAs(filter);
    }

    @Test
    public void testRemoveSimpleFilterFromCompositeFilter() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});

        filterable.addFilter(compositeFilter);

        // should remove internal filter of compositeFilter
        filterable.removeFilter(filter1);
        assertThat(filterable.getFilter() instanceof CompositeFilter).isFalse();

        assertThat(filterable.getFilter()).isEqualTo(filter2);
    }

    @Test
    public void testRemoveSimpleFilterFromCompositeAndSimpleFilter() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});
        final Filter anotherFilter = ThresholdFilter.createFilter(Level.WARN, null, null);


        filterable.addFilter(compositeFilter);
        filterable.addFilter(anotherFilter);

        // should not remove internal filter of compositeFilter
        filterable.removeFilter(anotherFilter);
        assertThat(filterable.getFilter() instanceof CompositeFilter).isTrue();
        assertThat(((CompositeFilter) filterable.getFilter()).getFiltersArray().length).isEqualTo(2);
    }

    @Test
    public void testRemoveCompositeFilterFromCompositeFilter() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});

        filterable.addFilter(compositeFilter);
        filterable.removeFilter(compositeFilter);
        assertThat(filterable.getFilter()).isNull();
    }

    @Test
    public void testRemoveFiltersFromComposite() {
        final Filter filter1 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter filter2 = ThresholdFilter.createFilter(Level.ERROR, null, null);
        final Filter compositeFilter = CompositeFilter.createFilters(new Filter[]{filter1, filter2});
        final Filter anotherFilter = ThresholdFilter.createFilter(Level.WARN, null, null);

        filterable.addFilter(compositeFilter);
        filterable.addFilter(anotherFilter);
        assertThat(((CompositeFilter) filterable.getFilter()).getFiltersArray().length).isEqualTo(3);
        filterable.removeFilter(filter1);
        assertThat(((CompositeFilter) filterable.getFilter()).getFiltersArray().length).isEqualTo(2);
        filterable.removeFilter(filter2);
        assertThat(filterable.getFilter()).isSameAs(anotherFilter);
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
