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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.junit.jupiter.api.Test;

class ThreadContextMapFilterTest {

    @Test
    void testFilter() {
        ThreadContext.put("userid", "testuser");
        ThreadContext.put("organization", "Apache");
        final KeyValuePair[] pairs =
                new KeyValuePair[] {new KeyValuePair("userid", "JohnDoe"), new KeyValuePair("organization", "Apache")};
        ThreadContextMapFilter filter = ThreadContextMapFilter.createFilter(pairs, "and", null, null);
        assertNotNull(filter);
        filter.start();
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, (Object) null, null));
        ThreadContext.remove("userid");
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, (Object) null, null));
        ThreadContext.put("userid", "JohnDoe");
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.ERROR, null, (Object) null, null));
        ThreadContext.put("organization", "ASF");
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, (Object) null, null));
        ThreadContext.clearMap();
        filter = ThreadContextMapFilter.createFilter(pairs, "or", null, null);
        assertNotNull(filter);
        filter.start();
        assertTrue(filter.isStarted());
        ThreadContext.put("userid", "testuser");
        ThreadContext.put("organization", "Apache");
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.DEBUG, null, (Object) null, null));
        ThreadContext.put("organization", "ASF");
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, (Object) null, null));
        ThreadContext.remove("organization");
        assertSame(Filter.Result.DENY, filter.filter(null, Level.DEBUG, null, (Object) null, null));
        final KeyValuePair[] single = new KeyValuePair[] {new KeyValuePair("userid", "testuser")};
        filter = ThreadContextMapFilter.createFilter(single, null, null, null);
        assertNotNull(filter);
        filter.start();
        assertTrue(filter.isStarted());
        assertSame(Filter.Result.NEUTRAL, filter.filter(null, Level.DEBUG, null, (Object) null, null));
        ThreadContext.clearMap();
    }
}
