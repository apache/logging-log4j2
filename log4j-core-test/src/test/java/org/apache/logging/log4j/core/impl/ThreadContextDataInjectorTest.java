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
package org.apache.logging.log4j.core.impl;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.spi.LoggingSystemProperties;
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

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.logging.log4j.ThreadContext.getThreadContextMap;
import static org.apache.logging.log4j.core.impl.ContextDataInjectorFactory.createInjector;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@RunWith(Parameterized.class)
public class ThreadContextDataInjectorTest {
    @Parameters(name = "{0}")
    public static Collection<String[]> threadContextMapClassNames() {
        return asList(new String[][] {
                { "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap", "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap" },
                { "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap", "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap" },
                { "org.apache.logging.log4j.spi.DefaultThreadContextMap", null }
        });
    }

    @Parameter
    public String threadContextMapClassName;

    @Parameter(value = 1)
    public String readOnlythreadContextMapClassName;

    @Before
    public void before() {
        System.setProperty(LoggingSystemProperties.THREAD_CONTEXT_MAP_CLASS, threadContextMapClassName);
    }

    @After
    public void after() {
        ThreadContext.remove("foo");
        ThreadContext.remove("baz");
        System.clearProperty(LoggingSystemProperties.THREAD_CONTEXT_MAP_CLASS);
        System.clearProperty(LoggingSystemProperties.THREAD_CONTEXT_MAP_INHERITABLE);
    }

    private void testContextDataInjector() {
        ReadOnlyThreadContextMap readOnlythreadContextMap = getThreadContextMap();
        assertThat("thread context map class name",
                   (readOnlythreadContextMap == null) ? null : readOnlythreadContextMap.getClass().getName(),
                   is(equalTo(readOnlythreadContextMapClassName)));

        ContextDataInjector contextDataInjector = createInjector();
        StringMap stringMap = contextDataInjector.injectContextData(null, new SortedArrayStringMap());

        assertThat("thread context map", ThreadContext.getContext(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));
        assertThat("context map", stringMap.toMap(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));

        if (!stringMap.isFrozen()) {
            stringMap.clear();
            assertThat("thread context map", ThreadContext.getContext(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));
            assertThat("context map", stringMap.toMap().entrySet(), is(empty()));
        }

        ThreadContext.put("foo", "bum");
        ThreadContext.put("baz", "bam");

        assertThat("thread context map", ThreadContext.getContext(), allOf(hasEntry("foo", "bum"), hasEntry("baz", "bam")));
        if (stringMap.isFrozen()) {
            assertThat("context map", stringMap.toMap(), allOf(hasEntry("foo", "bar"), not(hasKey("baz"))));
        } else {
            assertThat("context map", stringMap.toMap().entrySet(), is(empty()));
        }
    }

    private void prepareThreadContext(boolean isThreadContextMapInheritable) {
        System.setProperty(LoggingSystemProperties.THREAD_CONTEXT_MAP_INHERITABLE, Boolean.toString(isThreadContextMapInheritable));
        ((PropertiesUtil) PropertiesUtil.getProperties()).reload();
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
            newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    testContextDataInjector();
                }
            }).get();
        } catch (ExecutionException ee) {
            throw ee.getCause();
        }
    }
}
