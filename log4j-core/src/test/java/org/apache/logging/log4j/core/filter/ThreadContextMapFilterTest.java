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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.junit.jupiter.api.Test;

public class ThreadContextMapFilterTest {

    @Test
    public void testFilter() {
        ThreadContext.put("userid", "testuser");
        ThreadContext.put("organization", "Apache");
        final KeyValuePair[] pairs = new KeyValuePair[] { new KeyValuePair("userid", "JohnDoe"),
                                                    new KeyValuePair("organization", "Apache")};
        ThreadContextMapFilter filter = ThreadContextMapFilter.createFilter(pairs, "and", null, null);
        assertThat(filter).isNotNull();
        filter.start();
        assertThat(filter.isStarted()).isTrue();
        assertThat(filter.filter(null, Level.DEBUG, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.DENY);
        ThreadContext.remove("userid");
        assertThat(filter.filter(null, Level.DEBUG, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.DENY);
        ThreadContext.put("userid", "JohnDoe");
        assertThat(filter.filter(null, Level.ERROR, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.NEUTRAL);
        ThreadContext.put("organization", "ASF");
        assertThat(filter.filter(null, Level.DEBUG, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.DENY);
        ThreadContext.clearMap();
        filter = ThreadContextMapFilter.createFilter(pairs, "or", null, null);
        assertThat(filter).isNotNull();
        filter.start();
        assertThat(filter.isStarted()).isTrue();
        ThreadContext.put("userid", "testuser");
        ThreadContext.put("organization", "Apache");
        assertThat(filter.filter(null, Level.DEBUG, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.NEUTRAL);
        ThreadContext.put("organization", "ASF");
        assertThat(filter.filter(null, Level.DEBUG, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.DENY);
        ThreadContext.remove("organization");
        assertThat(filter.filter(null, Level.DEBUG, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.DENY);
        final KeyValuePair[] single = new KeyValuePair[] {new KeyValuePair("userid", "testuser")};
        filter = ThreadContextMapFilter.createFilter(single, null, null, null);
        assertThat(filter).isNotNull();
        filter.start();
        assertThat(filter.isStarted()).isTrue();
        assertThat(filter.filter(null, Level.DEBUG, null, (Object) null, (Throwable) null)).isSameAs(Filter.Result.NEUTRAL);
        ThreadContext.clearMap();
    }
}
