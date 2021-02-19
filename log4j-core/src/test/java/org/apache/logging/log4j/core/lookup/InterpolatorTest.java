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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.junit.JndiRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;

/**
 *
 */
public class InterpolatorTest {

    private static final String TESTKEY = "TestKey";
    private static final String TESTKEY2 = "TestKey2";
    private static final String TESTVAL = "TestValue";

    private static final String TEST_CONTEXT_RESOURCE_NAME = "logging/context-name";
    private static final String TEST_CONTEXT_NAME = "app-1";

    @ClassRule
    public static RuleChain rules = RuleChain.outerRule(new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            System.setProperty(TESTKEY, TESTVAL);
            System.setProperty(TESTKEY2, TESTVAL);
        }

        @Override
        protected void after() {
            System.clearProperty(TESTKEY);
            System.clearProperty(TESTKEY2);
        }
    }).around(new JndiRule(
        JndiLookup.CONTAINER_JNDI_RESOURCE_PATH_PREFIX + TEST_CONTEXT_RESOURCE_NAME, TEST_CONTEXT_NAME));

    @Test
    public void testLookup() {
        final Map<String, String> map = new HashMap<>();
        map.put(TESTKEY, TESTVAL);
        final StrLookup lookup = new Interpolator(new MapLookup(map));
        ThreadContext.put(TESTKEY, TESTVAL);
        String value = lookup.lookup(TESTKEY);
        assertThat(value).isEqualTo(TESTVAL);
        value = lookup.lookup("ctx:" + TESTKEY);
        assertThat(value).isEqualTo(TESTVAL);
        value = lookup.lookup("sys:" + TESTKEY);
        assertThat(value).isEqualTo(TESTVAL);
        value = lookup.lookup("SYS:" + TESTKEY2);
        assertThat(value).isEqualTo(TESTVAL);
        value = lookup.lookup("BadKey");
        assertThat(value).isNull();
        ThreadContext.clearMap();
        value = lookup.lookup("ctx:" + TESTKEY);
        assertThat(value).isEqualTo(TESTVAL);
        value = lookup.lookup("jndi:" + TEST_CONTEXT_RESOURCE_NAME);
        assertThat(value).isEqualTo(TEST_CONTEXT_NAME);
    }

    private void assertLookupNotEmpty(final StrLookup lookup, final String key) {
        final String value = lookup.lookup(key);
        assertThat(value).isNotNull();
        assertThat(value.isEmpty()).isFalse();
        System.out.println(key + " = " + value);
    }

    @Test
    public void testLookupWithDefaultInterpolator() {
        final StrLookup lookup = new Interpolator();
        String value = lookup.lookup("sys:" + TESTKEY);
        assertThat(value).isEqualTo(TESTVAL);
        value = lookup.lookup("env:PATH");
        assertThat(value).isNotNull();
        value = lookup.lookup("jndi:" + TEST_CONTEXT_RESOURCE_NAME);
        assertThat(value).isEqualTo(TEST_CONTEXT_NAME);
        value = lookup.lookup("date:yyyy-MM-dd");
        assertThat(value).describedAs("No Date").isNotNull();
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        final String today = format.format(new Date());
        assertThat(today).isEqualTo(value);
        assertLookupNotEmpty(lookup, "java:version");
        assertLookupNotEmpty(lookup, "java:runtime");
        assertLookupNotEmpty(lookup, "java:vm");
        assertLookupNotEmpty(lookup, "java:os");
        assertLookupNotEmpty(lookup, "java:locale");
        assertLookupNotEmpty(lookup, "java:hw");
    }
}
