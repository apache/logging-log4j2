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
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.test.JUnit5Bridge;
import org.junit.Test;

/**
 * JndiDisabledLookupTest
 *
 * Verifies the Lookups are disabled without the log4j2.enableJndiLookup property set to true.
 */
public class JndiDisabledLookupTest {

    @Test
    public void testLookup() {
        JUnit5Bridge.assertThrows(IllegalStateException.class, new Runnable() {
            @Override
            public void run() {
                new JndiLookup();
            }
        });
    }
}
