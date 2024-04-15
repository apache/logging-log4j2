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
package org.apache.logging.log4j.spi;

import java.util.Properties;
import java.util.stream.Stream;
import org.apache.logging.log4j.internal.map.StringArrayThreadContextMap;
import org.apache.logging.log4j.test.spi.ThreadContextMapSuite;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Execution(ExecutionMode.CONCURRENT)
class ThreadContextMapTest extends ThreadContextMapSuite {

    private static final String KEY = "key";
    private static final Lazy<PropertiesUtil> defaultMapProperties = Lazy.pure(() -> createMapProperties(false));
    private static final Lazy<PropertiesUtil> inheritableMapProperties = Lazy.pure(() -> createMapProperties(true));

    private static PropertiesUtil createMapProperties(final boolean inheritable) {
        final Properties props = new Properties();
        // By specifying all the possible properties, the resulting thread context maps do not depend on other
        // property sources like Java system properties.
        props.setProperty("log4j2.threadContextInitialCapacity", "16");
        props.setProperty("log4j2.isThreadContextMapInheritable", inheritable ? "true" : "false");
        return new PropertiesUtil(props);
    }

    static Stream<ThreadContextMap> defaultMaps() {
        return Stream.of(
                new DefaultThreadContextMap(),
                new CopyOnWriteSortedArrayThreadContextMap(),
                new GarbageFreeSortedArrayThreadContextMap(),
                new StringArrayThreadContextMap());
    }

    static Stream<ThreadContextMap> localMaps() {
        return Stream.of(
                new DefaultThreadContextMap(true, defaultMapProperties.get()),
                new CopyOnWriteSortedArrayThreadContextMap(defaultMapProperties.get()),
                new GarbageFreeSortedArrayThreadContextMap(defaultMapProperties.get()),
                new StringArrayThreadContextMap());
    }

    static Stream<ThreadContextMap> inheritableMaps() {
        return Stream.of(
                new DefaultThreadContextMap(true, inheritableMapProperties.get()),
                new CopyOnWriteSortedArrayThreadContextMap(inheritableMapProperties.get()),
                new GarbageFreeSortedArrayThreadContextMap(inheritableMapProperties.get()));
    }

    @ParameterizedTest
    @MethodSource("defaultMaps")
    void threadLocalNotInheritableByDefault(final ThreadContextMap threadContext) {
        threadContext.put(KEY, "threadLocalNotInheritableByDefault");
        assertThreadContextValueOnANewThread(threadContext, KEY, null);
    }

    @ParameterizedTest
    @MethodSource("inheritableMaps")
    void threadLocalInheritableIfConfigured(final ThreadContextMap threadContext) {
        threadContext.put(KEY, "threadLocalInheritableIfConfigured");
        assertThreadContextValueOnANewThread(threadContext, KEY, "threadLocalInheritableIfConfigured");
    }

    @ParameterizedTest
    @MethodSource("localMaps")
    void saveAndRestoreMap(final ThreadContextMap threadContext) {
        assertContextDataCanBeSavedAndRestored(threadContext);
    }

    @ParameterizedTest
    @MethodSource("localMaps")
    void saveAndRestoreMapOnAnotherThread(final ThreadContextMap threadContext) {
        assertContextDataCanBeTransferred(threadContext);
    }

    @ParameterizedTest
    @MethodSource("localMaps")
    void savedValueNotNullIfMapEmpty(final ThreadContextMap threadContext) {
        assertSavedValueNotNullIfMapEmpty(threadContext);
    }

    @ParameterizedTest
    @MethodSource("localMaps")
    void restoreDoesNotAcceptNull(final ThreadContextMap threadContext) {
        assertRestoreDoesNotAcceptNull(threadContext);
    }
}
