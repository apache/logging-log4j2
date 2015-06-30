/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.logging.log4j.core.filter;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Unit test for <code>BurstFilter</code>.
 */
public class BurstFilterLogDelayTest {

    @Test
    public void testCompareToOverflow() {
        // no overflow, but close
        final Delayed d1 = BurstFilter.createLogDelay(Long.MAX_VALUE - TimeUnit.SECONDS.toNanos(10) - System.nanoTime());

        // Overflow
        final Delayed d2 = BurstFilter.createLogDelay(Long.MAX_VALUE + TimeUnit.SECONDS.toNanos(10) - System.nanoTime());

        assertThat(d2, is(greaterThan(d1)));
    }

}
