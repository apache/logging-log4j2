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

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 */
public class StrSubstitutorTest {

     private static final String TESTKEY = "TestKey";
    private static final String TESTVAL = "TestValue";


    @BeforeClass
    public static void before() {
        System.setProperty(TESTKEY, TESTVAL);
    }

    @AfterClass
    public static void after() {
        System.clearProperty(TESTKEY);
    }


    @Test
    public void testLookup() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TESTKEY, TESTVAL);
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        ThreadContext.put(TESTKEY, TESTVAL);
        String value = subst.replace("${TestKey}-${ctx:TestKey}-${sys:TestKey}");
        assertEquals("TestValue-TestValue-TestValue", value);
        value = subst.replace("${BadKey}");
        assertEquals("${BadKey}", value);
    }
}
