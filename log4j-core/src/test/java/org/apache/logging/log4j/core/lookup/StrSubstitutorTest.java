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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

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

        value = subst.replace("${BadKey:-Unknown}-${ctx:BadKey:-Unknown}-${sys:BadKey:-Unknown}");
        assertEquals("Unknown-Unknown-Unknown", value);
        value = subst.replace("${BadKey:-Unknown}-${ctx:BadKey}-${sys:BadKey:-Unknown}");
        assertEquals("Unknown-${ctx:BadKey}-Unknown", value);
        value = subst.replace("${BadKey:-Unknown}-${ctx:BadKey:-}-${sys:BadKey:-Unknown}");
        assertEquals("Unknown--Unknown", value);
    }

    @Test
    public void testDefault() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TESTKEY, TESTVAL);
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        ThreadContext.put(TESTKEY, TESTVAL);
        //String value = subst.replace("${sys:TestKey1:-${ctx:TestKey}}");
        final String value = subst.replace("${sys:TestKey1:-${ctx:TestKey}}");
        assertEquals("TestValue", value);
    }

    @Test
    public void testDefaultReferencesLookupValue() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TESTKEY, "${java:version}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        final String value = subst.replace("${sys:TestKey1:-${ctx:TestKey}}");
        assertEquals("${java:version}", value);
    }

    @Test
    public void testInfiniteSubstitutionOnString() {
        final StrLookup lookup = new Interpolator(new MapLookup(new HashMap<String, String>()));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        String infiniteSubstitution = "${${::-${::-$${::-j}}}}";
        assertEquals(infiniteSubstitution, subst.replace(infiniteSubstitution));
    }

    @Test
    public void testInfiniteSubstitutionOnStringBuilder() {
        final StrLookup lookup = new Interpolator(new MapLookup(new HashMap<String, String>()));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        String infiniteSubstitution = "${${::-${::-$${::-j}}}}";
        assertEquals(infiniteSubstitution, subst.replace(null, new StringBuilder(infiniteSubstitution)));
    }

    @Test
    public void testRecursiveSubstitution() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("first", "${ctx:first}");
        map.put("second", "secondValue");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("${ctx:first} and secondValue", subst.replace("${ctx:first} and ${ctx:second}"));
    }

    @Test
    public void testRecursiveWithDefault() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("first", "${ctx:first:-default}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("default", subst.replace("${ctx:first}"));
    }

    @Test
    public void testRecursiveWithRecursiveDefault() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("first", "${ctx:first:-${ctx:first}}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("${ctx:first}", subst.replace("${ctx:first}"));
    }

    @Test
    public void testNestedSelfReferenceWithRecursiveEvaluation() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("first", "${${ctx:first}}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("${${ctx:first}}", subst.replace("${ctx:first}"));
    }

    @Test
    public void testRandomWithRecursiveDefault() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("first", "${env:RANDOM:-${ctx:first}}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("${ctx:first}", subst.replace("${ctx:first}"));
    }

    @Test
    public void testNoRecursiveEvaluationWithDefault() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("first", "${java:version}");
        map.put("second", "${java:runtime}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        assertEquals("${java:version}", subst.replace("${ctx:first:-${ctx:second}}"));
    }

    @Test
    public void testNoRecursiveEvaluationWithDepthOne() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put("first", "${java:version}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        assertEquals("${java:version}", subst.replace("${ctx:first}"));
    }

    @Test
    public void testLookupThrows() {
        final StrSubstitutor subst = new StrSubstitutor(new Interpolator(new StrLookup() {

            @Override
            public String lookup(String key) {
                if ("throw".equals(key)) {
                    throw new RuntimeException();
                }
                return "success";
            }

            @Override
            public String lookup(LogEvent event, String key) {
                return lookup(key);
            }
        }));
        subst.setRecursiveEvaluationAllowed(false);
        assertEquals("success ${foo:throw} success", subst.replace("${foo:a} ${foo:throw} ${foo:c}"));
    }
}
