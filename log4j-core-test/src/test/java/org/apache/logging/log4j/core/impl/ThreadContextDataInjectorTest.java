/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.impl;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.logging.log4j.ThreadContext.getThreadContextMap;
import static org.apache.logging.log4j.core.impl.ContextDataInjectorFactory.createInjector;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.test.ThreadContextUtilityClass;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ThreadContextDataInjectorTest {
    @Parameters(name = "{0}")
    public static Collection<String[]> threadContextMapClassNames() {
        return asList(new String[][] {
            {
                "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap",
                "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap"
            },
            {
                "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap",
                "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap"
            },
            {"org.apache.logging.log4j.spi.DefaultThreadContextMap", null}
        });
    }

    @Parameter
    public String threadContextMapClassName;

    @Parameter(value = 1)
    public String readOnlythreadContextMapClassName;

    @Before
    public void before() {
        System.setProperty("log4j2.threadContextMap", threadContextMapClassName);
    }

    @After
    public void after() {
        ThreadContext.remove("foo");
        ThreadContext.remove("baz");
        System.clearProperty("log4j2.threadContextMap");
        System.clearProperty("log4j2.isThreadContextMapInheritable");
    }

    private void testContextDataInjector() {
        final ReadOnlyThreadContextMap readOnlythreadContextMap = getThreadContextMap();
        assertThat(
                "thread context map class name",
                (readOnlythreadContextMap == null)
                        ? null
                        : readOnlythreadContextMap.getClass().getName(),
                is(equalTo(readOnlythreadContextMapClassName)));

        final ContextDataInjector contextDataInjector = createInjector();
        final StringMap stringMap = contextDataInjector.injectContextData(null, new SortedArrayStringMap());

        assertThat("thread context map", ThreadContext.getContext(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));
        assertThat("context map", stringMap.toMap(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));

        if (!stringMap.isFrozen()) {
            stringMap.clear();
            assertThat(
                    "thread context map",
                    ThreadContext.getContext(),
                    allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));
            assertThat("context map", stringMap.toMap().entrySet(), is(empty()));
        }

        ThreadContext.put("foo", "bum");
        ThreadContext.put("baz", "bam");

        assertThat(
                "thread context map",
                ThreadContext.getContext(),
                allOf(hasEntry("foo", "bum"), hasEntry("baz", "bam")));
        if (stringMap.isFrozen()) {
            assertThat("context map", stringMap.toMap(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));
        } else {
            assertThat("context map", stringMap.toMap().entrySet(), is(empty()));
        }
    }

    private void prepareThreadContext(final boolean isThreadContextMapInheritable) {
        System.setProperty("log4j2.isThreadContextMapInheritable", Boolean.toString(isThreadContextMapInheritable));
        PropertiesUtil.getProperties().reload();
        ThreadContextUtilityClass.reset();
        ThreadContext.remove("baz");
        ThreadContext.put("foo", "bar");
    }

    @Test
    public void testThreadContextImmutability() {
        prepareThreadContext(false);
        testContextDataInjector();
    }

    @Test
    public void testInheritableThreadContextImmutability() throws Throwable {
        prepareThreadContext(true);
        try {
            newSingleThreadExecutor().submit(this::testContextDataInjector).get();
        } catch (final ExecutionException ee) {
            throw ee.getCause();
        }
    }
}
