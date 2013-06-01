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
package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.helpers.KeyValuePair;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
 *
 */
public class ThreadContextMapFilterTest {

    @Test
    public void testFilter() {
        ThreadContext.put("userid", "testuser");
        ThreadContext.put("organization", "Apache");
        final KeyValuePair[] pairs = new KeyValuePair[] { new KeyValuePair("userid", "JohnDoe"),
                                                    new KeyValuePair("organization", "Apache")};
        ThreadContextMapFilter filter = ThreadContextMapFilter.createFilter(pairs, "and", null, null);
        filter.start();
        assertTrue(filter.isStarted());
        assertTrue(filter.filter(null, Level.DEBUG, null, null, (Throwable)null) == Filter.Result.DENY);
        ThreadContext.remove("userid");
        assertTrue(filter.filter(null, Level.DEBUG, null, null, (Throwable)null) == Filter.Result.DENY);
        ThreadContext.put("userid", "JohnDoe");
        assertTrue(filter.filter(null, Level.ERROR, null, null, (Throwable)null) == Filter.Result.NEUTRAL);
        ThreadContext.put("organization", "ASF");
        assertTrue(filter.filter(null, Level.DEBUG, null, null, (Throwable)null) == Filter.Result.DENY);
        ThreadContext.clear();
        filter = ThreadContextMapFilter.createFilter(pairs, "or", null, null);
        filter.start();
        assertTrue(filter.isStarted());
        ThreadContext.put("userid", "testuser");
        ThreadContext.put("organization", "Apache");
        assertTrue(filter.filter(null, Level.DEBUG, null, null, (Throwable)null) == Filter.Result.NEUTRAL);
        ThreadContext.put("organization", "ASF");
        assertTrue(filter.filter(null, Level.DEBUG, null, null, (Throwable)null) == Filter.Result.DENY);
        ThreadContext.remove("organization");
        assertTrue(filter.filter(null, Level.DEBUG, null, null, (Throwable)null) == Filter.Result.DENY);
        final KeyValuePair[] single = new KeyValuePair[] {new KeyValuePair("userid", "testuser")};
        filter = ThreadContextMapFilter.createFilter(single, null, null, null);
        filter.start();
        assertTrue(filter.isStarted());
        assertTrue(filter.filter(null, Level.DEBUG, null, null, (Throwable)null) == Filter.Result.NEUTRAL);
        ThreadContext.clear();
    }
}
