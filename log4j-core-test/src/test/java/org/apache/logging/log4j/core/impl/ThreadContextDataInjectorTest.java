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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.spi.LoggingSystem;
import org.apache.logging.log4j.spi.ReadOnlyThreadContextMap;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.junit.After;
import org.junit.Test;
import org.junit.platform.commons.function.Try;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ThreadContextDataInjectorTest {

    public static final String THREAD_LOCAL_MAP_CLASS_NAME =
            "org.apache.logging.log4j.spi.CopyOnWriteSortedArrayThreadContextMap";
    public static final String GARBAGE_FREE_MAP_CLASS_NAME =
            "org.apache.logging.log4j.spi.GarbageFreeSortedArrayThreadContextMap";

    @Parameters(name = "{1}")
    public static Object[][] threadContextMapConstructorsAndReadOnlyNames() {
        return new Object[][] {
            {
                (Function<Boolean, ThreadContextMap>) ThreadContextDataInjectorTest::createThreadLocalMap,
                THREAD_LOCAL_MAP_CLASS_NAME
            },
            {
                (Function<Boolean, ThreadContextMap>) ThreadContextDataInjectorTest::createGarbageFreeMap,
                GARBAGE_FREE_MAP_CLASS_NAME
            }
        };
    }

    @Parameter(0)
    public Function<Boolean, ThreadContextMap> threadContextMapConstructor;

    @Parameter(1)
    public String readOnlyThreadContextMapClassName;

    @After
    public void after() {
        ThreadContext.remove("foo");
        ThreadContext.remove("baz");
    }

    private void testContextDataInjector() {
        final ReadOnlyThreadContextMap readOnlythreadContextMap = getThreadContextMap();
        assertThat(
                "thread context map class name",
                (readOnlythreadContextMap == null)
                        ? null
                        : readOnlythreadContextMap.getClass().getName(),
                is(equalTo(readOnlyThreadContextMapClassName)));

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
        LoggingSystem.getProvider()
                .setThreadContextMapFactory(threadContextMapConstructor.apply(isThreadContextMapInheritable));
        ThreadContext.init();
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
        } catch (ExecutionException ee) {
            throw ee.getCause();
        }
    }

    private static ThreadContextMap createThreadLocalMap(final boolean inheritable) {
        final Try<Class<?>> loadedClass = ReflectionSupport.tryToLoadClass(THREAD_LOCAL_MAP_CLASS_NAME);
        final Class<? extends ThreadContextMap> mapClass =
                assertDoesNotThrow(loadedClass::get).asSubclass(ThreadContextMap.class);
        return newInstanceWithCapacityArg(mapClass, inheritable);
    }

    private static ThreadContextMap createGarbageFreeMap(final boolean inheritable) {
        final Try<Class<?>> loadedClass = ReflectionSupport.tryToLoadClass(GARBAGE_FREE_MAP_CLASS_NAME);
        final Class<? extends ThreadContextMap> mapClass =
                assertDoesNotThrow(loadedClass::get).asSubclass(ThreadContextMap.class);
        return newInstanceWithCapacityArg(mapClass, inheritable);
    }

    private static ThreadContextMap newInstanceWithCapacityArg(
            final Class<? extends ThreadContextMap> mapClass, final boolean inheritable) {
        final Constructor<? extends ThreadContextMap> constructor =
                assertDoesNotThrow(() -> mapClass.getDeclaredConstructor(Boolean.TYPE, Integer.TYPE));
        assertTrue(constructor.trySetAccessible(), () -> "Unable to access constructor for " + mapClass);
        return assertDoesNotThrow(
                () -> constructor.newInstance(inheritable, LoggingSystem.THREAD_CONTEXT_DEFAULT_INITIAL_CAPACITY));
    }
}
