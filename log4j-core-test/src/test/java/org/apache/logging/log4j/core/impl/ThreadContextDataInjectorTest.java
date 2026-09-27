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

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.logging.log4j.core.impl.ContextDataInjectorFactory.createInjector;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.util.ProviderUtil;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ThreadContextDataInjectorTest {

    public static Stream<String> threadContextMapClassNames() {
        return Stream.of(
                "org.apache.logging.log4j.core.context.internal.GarbageFreeSortedArrayThreadContextMap",
                "org.apache.logging.log4j.spi.DefaultThreadContextMap");
    }

    public String threadContextMapClassName;

    private static void resetThreadContextMap() {
        final Log4jProvider provider = (Log4jProvider) ProviderUtil.getProvider();
        provider.resetThreadContextMap();
        ThreadContext.init();
    }

    @AfterEach
    public void after() {
        ThreadContext.remove("foo");
        ThreadContext.remove("baz");
        System.clearProperty("log4j2.threadContextMap");
        System.clearProperty("log4j2.isThreadContextMapInheritable");
    }

    private void testContextDataInjector() {
        final ThreadContextMap threadContextMap = ProviderUtil.getProvider().getThreadContextMapInstance();
        assertThat(
                "thread context map class name",
                threadContextMap.getClass().getName(),
                is(equalTo(threadContextMapClassName)));

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
        resetThreadContextMap();
        ThreadContext.clearMap();
        ThreadContext.put("foo", "bar");
    }

    @ParameterizedTest
    @MethodSource("threadContextMapClassNames")
    public void testThreadContextImmutability(final String name) {
        System.setProperty("log4j2.threadContextMap", name);
        this.threadContextMapClassName = name;
        prepareThreadContext(false);
        testContextDataInjector();
    }

    @ParameterizedTest
    @MethodSource("threadContextMapClassNames")
    public void testInheritableThreadContextImmutability(final String name) throws Throwable {
        System.setProperty("log4j2.threadContextMap", name);
        this.threadContextMapClassName = name;
        prepareThreadContext(true);
        try {
            newSingleThreadExecutor().submit(this::testContextDataInjector).get();
        } catch (final ExecutionException ee) {
            throw ee.getCause();
        }
    }
}
