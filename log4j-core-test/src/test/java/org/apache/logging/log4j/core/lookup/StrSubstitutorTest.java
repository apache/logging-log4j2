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
import org.apache.logging.log4j.test.junit.StatusLoggerLevel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@StatusLoggerLevel("OFF")
public class StrSubstitutorTest {

    private static final String TESTKEY = "TestKey";
    private static final String TESTVAL = "TestValue";


    @BeforeAll
    public static void before() {
        System.setProperty(TESTKEY, TESTVAL);
    }

    @AfterAll
    public static void after() {
        System.clearProperty(TESTKEY);
    }

    @Test
    public void testLookup() {
        final Map<String, String> map = new HashMap<>();
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
    public void testJavaDocExample() {
        final Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("animal", "quick brown fox");
        valuesMap.put("target", "lazy dog");
        final String templateString = "The ${animal} jumped over the ${target}.";
        final StrSubstitutor sub = new StrSubstitutor(valuesMap);
        final String resolvedString = sub.replace(templateString);
        assertEquals("The quick brown fox jumped over the lazy dog.", resolvedString);
    }

    @Test
    public void testDelimiterExampleFromJavaDoc() {
        final Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("animal", "quick brown fox");
        valuesMap.put("target", "lazy dog");
        final String templateString = "The ${animal} jumped over the ${target}. ${undefined.number:-1234567890}";
        final StrSubstitutor sub = new StrSubstitutor(valuesMap);
        final String resolvedString = sub.replace(templateString);
        assertEquals("The quick brown fox jumped over the lazy dog. 1234567890", resolvedString);
    }

    @Test
    public void testEscapedRecursionExampleFromJavaDoc() {
        final Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("name", "x");
        final String templateString = "The variable $${${name}} must be used.";
        final StrSubstitutor sub = new StrSubstitutor(valuesMap);

        sub.setEnableSubstitutionInVariables(false);
        final String resolvedString = sub.replace(templateString);
        assertEquals("The variable ${x} must be used.", resolvedString);

        // Due to escaping enabling recursion now should make no difference.
        sub.setEnableSubstitutionInVariables(true);
        final String resolvedStringWithRecursion = sub.replace(templateString);
        assertEquals(resolvedString, resolvedStringWithRecursion);
    }

    @Test
    public void testPrePostfixRecursionExampleFromJavaDoc() {
        final Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("name", "x");
        final String templateString = "The variable ${$[name]} must be used.";
        final StrSubstitutor sub = new StrSubstitutor(valuesMap, "$[", "]");
        final String resolvedString = sub.replace(templateString);
        assertEquals("The variable ${x} must be used.", resolvedString);
    }

    @Test
    public void testRecursionExampleFromJavaDoc() {
        final Map<String, String> valuesMap = new HashMap<>();
        valuesMap.put("name", "x");
        valuesMap.put("x", "3");
        final String templateString = "The value ${${name}} must be used.";
        final StrSubstitutor sub = new StrSubstitutor(valuesMap);

        sub.setEnableSubstitutionInVariables(false);
        assertEquals("The value ${${name}} must be used.", sub.replace(templateString));

        sub.setEnableSubstitutionInVariables(true);
        assertEquals("The value 3 must be used.", sub.replace(templateString));
    }

    @Test
    public void testValueEscapeDelimiter() {
        final Map<String, String> valuesMap = new HashMap<>();
        // Example from MainMapLookup. Key contains ":-"
        valuesMap.put("main:--file", "path/file.txt");
        // Create substitutor, initially without support for escaping :-
        final StrSubstitutor sub = new StrSubstitutor(
                new PropertiesLookup(valuesMap),
                StrSubstitutor.DEFAULT_PREFIX,
                StrSubstitutor.DEFAULT_SUFFIX,
                StrSubstitutor.DEFAULT_ESCAPE,
                StrSubstitutor.DEFAULT_VALUE_DELIMITER,
                null // Ensure valueEscapeMatcher == null
        );
        // regular default values work without a valueEscapeMatcher.
        assertEquals("3", sub.replace("${y:-3}"));
        // variables with ':-' are treated as if they have a default value
        assertEquals("-file", sub.replace("${main:--file}"));
        // yet escaping doesn't work anymore (results in the original string).
        assertEquals("${main:\\--file}", sub.replace("${main:\\--file}"));

        // ensure there is valueEscapeMatcher (by resetting the value delimiter)
        sub.setValueDelimiter(StrSubstitutor.DEFAULT_VALUE_DELIMITER_STRING);
        // now the escaped variable with ":-" in it will be resolved.
        assertEquals("path/file.txt", sub.replace("${main:\\--file}"));
        // default values continue to work:
        assertEquals("no help", sub.replace("${main:\\--help:-no help}"));
        // even in minimalistic corner case:
        assertEquals("", sub.replace("${:\\-:-}"));
    }

    @Test
    public void testDefault() {
        final Map<String, String> map = new HashMap<>();
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
        final Map<String, String> map = new HashMap<>();
        map.put(TESTKEY, "${java:version}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        final String value = subst.replace("${sys:TestKey1:-${ctx:TestKey}}");
        assertEquals("${java:version}", value);
    }

    @Test
    public void testInfiniteSubstitutionOnString() {
        final StrLookup lookup = new Interpolator(new MapLookup(new HashMap<>()));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        String infiniteSubstitution = "${${::-${::-$${::-j}}}}";
        assertEquals(infiniteSubstitution, subst.replace(infiniteSubstitution));
    }

    @Test
    public void testInfiniteSubstitutionOnStringBuilder() {
        final StrLookup lookup = new Interpolator(new MapLookup(new HashMap<>()));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        String infiniteSubstitution = "${${::-${::-$${::-j}}}}";
        assertEquals(infiniteSubstitution, subst.replace(null, new StringBuilder(infiniteSubstitution)));
    }

    @Test
    public void testRecursiveSubstitution() {
        final Map<String, String> map = new HashMap<>();
        map.put("first", "${ctx:first}");
        map.put("second", "secondValue");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("${ctx:first} and secondValue", subst.replace("${ctx:first} and ${ctx:second}"));
    }

    @Test
    public void testRecursiveWithDefault() {
        final Map<String, String> map = new HashMap<>();
        map.put("first", "${ctx:first:-default}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("default", subst.replace("${ctx:first}"));
    }

    @Test
    public void testRecursiveWithRecursiveDefault() {
        final Map<String, String> map = new HashMap<>();
        map.put("first", "${ctx:first:-${ctx:first}}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("${ctx:first}", subst.replace("${ctx:first}"));
    }

    @Test
    public void testNestedSelfReferenceWithRecursiveEvaluation() {
        final Map<String, String> map = new HashMap<>();
        map.put("first", "${${ctx:first}}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("${${ctx:first}}", subst.replace("${ctx:first}"));
    }

    @Test
    public void testRandomWithRecursiveDefault() {
        final Map<String, String> map = new HashMap<>();
        map.put("first", "${env:RANDOM:-${ctx:first}}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(true);
        assertEquals("${ctx:first}", subst.replace("${ctx:first}"));
    }

    @Test
    public void testNoRecursiveEvaluationWithDefault() {
        final Map<String, String> map = new HashMap<>();
        map.put("first", "${java:version}");
        map.put("second", "${java:runtime}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        assertEquals("${java:version}", subst.replace("${ctx:first:-${ctx:second}}"));
    }

    @Test
    public void testNoRecursiveEvaluationWithDepthOne() {
        final Map<String, String> map = new HashMap<>();
        map.put("first", "${java:version}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        assertEquals("${java:version}", subst.replace("${ctx:first}"));
    }

    @Test
    public void testLookupsNestedWithoutRecursiveEvaluation() {
        final Map<String, String> map = new HashMap<>();
        map.put("first", "${java:version}");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        assertEquals("${java:version}", subst.replace("${${lower:C}t${lower:X}:first}"));
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

    @Test
    public void testTopLevelLookupsWithoutRecursiveEvaluation() {
        final Map<String, String> map = new HashMap<>();
        map.put("key", "VaLuE");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        assertEquals("value", subst.replace("${lower:${ctx:key}}"));
    }

    @Test
    public void testTopLevelLookupsWithoutRecursiveEvaluation_doubleLower() {
        final Map<String, String> map = new HashMap<>();
        map.put("key", "VaLuE");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        assertEquals("value", subst.replace("${lower:${lower:${ctx:key}}}"));
    }

    @Test
    public void testTopLevelLookupsWithoutRecursiveEvaluationAndDefaultValueLookup() {
        final Map<String, String> map = new HashMap<>();
        map.put("key2", "TWO");
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        final StrSubstitutor subst = new StrSubstitutor(lookup);
        subst.setRecursiveEvaluationAllowed(false);
        assertEquals("two", subst.replace("${lower:${ctx:key1:-${ctx:key2}}}"));
    }
}
