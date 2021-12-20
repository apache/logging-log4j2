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

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNull;

/**
 * JndiLookupTest
 */
public class JndiRestrictedLookupTest {

    private static final String DOMAIN = "apache.org";

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j2.enableJndiLookup", "true");
    }

    @Test
    public void testDnsLookup() throws Exception {
        final StrLookup lookup = new JndiLookup();
        String result = lookup.lookup("dns:/" + DOMAIN);
        assertNull("DNS data returend", result);

    }
}
