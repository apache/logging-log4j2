package org.apache.logging.log4j.core.filter;
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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the AbstractFilter test.
 */
public class AbstractFilterTest {

    @Test
    public void testUnrolledBackwardsCompatible() {
        final ConcreteFilter filter = new ConcreteFilter();
        final Filter.Result expected = Filter.Result.DENY;
        verifyMethodsWithUnrolledVarargs(filter, Filter.Result.DENY);

        filter.testResult = Filter.Result.ACCEPT;
        verifyMethodsWithUnrolledVarargs(filter, Filter.Result.ACCEPT);
    }

    private void verifyMethodsWithUnrolledVarargs(final ConcreteFilter filter, final Filter.Result expected) {
        final Logger logger = null;
        final Level level = null;
        final Marker marker = null;
        assertEquals(expected, filter.filter(logger, level, marker, "", 1));
        assertEquals(expected, filter.filter(logger, level, marker, "", 1, 2));
        assertEquals(expected, filter.filter(logger, level, marker, "", 1, 2, 3));
        assertEquals(expected, filter.filter(logger, level, marker, "", 1, 2, 3, 4));
        assertEquals(expected, filter.filter(logger, level, marker, "", 1, 2, 3, 4, 5));
        assertEquals(expected, filter.filter(logger, level, marker, "", 1, 2, 3, 4, 5, 6));
        assertEquals(expected, filter.filter(logger, level, marker, "", 1, 2, 3, 4, 5, 6, 7));
        assertEquals(expected, filter.filter(logger, level, marker, "", 1, 2, 3, 4, 5, 6, 7, 8));
        assertEquals(expected, filter.filter(logger, level, marker, "", 1, 2, 3, 4, 5, 6, 7, 8, 9));
        assertEquals(expected, filter.filter(logger, level, marker, "", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    }

    /**
     * Concreted filter class that does not override the methods with unrolled varargs.
     */
    static class ConcreteFilter extends AbstractFilter {
        Result testResult = Result.DENY;
        @Override
        public Result filter(final LogEvent event) {
            return testResult;
        }

        @Override
        public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                final Throwable t) {
            return testResult;
        }

        @Override
        public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                final Throwable t) {
            return testResult;
        }

        @Override
        public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                final Object... params) {
            return testResult;
        }
    }
}