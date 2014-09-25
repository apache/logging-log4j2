/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.logging.log4j.core.filter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.AbstractSerializationTest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.junit.runners.Parameterized.Parameters;

public class DynamicThresholdFilterSerializationTest extends AbstractSerializationTest {

    @Parameters
    public static Collection<Object[]> data() {
        final KeyValuePair[] pairs = new KeyValuePair[] {
                new KeyValuePair("testuser", "DEBUG"),
                new KeyValuePair("JohnDoe", "warn") };
        final KeyValuePair[] pairsEmpty = new KeyValuePair[0];
        return Arrays.asList(new Object[][] {
                { DynamicThresholdFilter.createFilter("userid", pairsEmpty, Level.ERROR, Result.ACCEPT, Result.DENY) },
                { DynamicThresholdFilter.createFilter("userid", pairsEmpty, Level.ERROR, null, null) },
                { DynamicThresholdFilter.createFilter("userid", pairs, Level.ERROR, null, null) } });
    }

    public DynamicThresholdFilterSerializationTest(final Serializable serializable) {
        super(serializable);
    }
}
