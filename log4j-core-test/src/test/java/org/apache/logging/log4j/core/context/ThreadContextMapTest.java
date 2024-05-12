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
package org.apache.logging.log4j.core.context;

import java.util.Properties;
import java.util.stream.Stream;
import org.apache.logging.log4j.spi.ThreadContextMap;
import org.apache.logging.log4j.test.spi.AbstractThreadContextMapTest;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ThreadContextMapTest extends AbstractThreadContextMapTest {

    static Stream<ThreadContextMap> defaultMaps() {
        return Stream.of(
                new StringArrayThreadContextMap(),
                new CopyOnWriteSortedArrayThreadContextMap(),
                new GarbageFreeSortedArrayThreadContextMap());
    }

    static Stream<ThreadContextMap> inheritableMaps() {
        final Properties props = new Properties();
        props.setProperty("log4j2.isThreadContextMapInheritable", "true");
        final PropertiesUtil util = new PropertiesUtil(props);
        return Stream.of(
                new CopyOnWriteSortedArrayThreadContextMap(util), new GarbageFreeSortedArrayThreadContextMap(util));
    }

    @ParameterizedTest
    @MethodSource("defaultMaps")
    void threadLocalNotInheritableByDefault(final ThreadContextMap contextMap) {
        assertThreadLocalNotInheritable(contextMap);
    }

    @ParameterizedTest
    @MethodSource("inheritableMaps")
    void threadLocalInheritableIfConfigured(final ThreadContextMap contextMap) {
        assertThreadLocalInheritable(contextMap);
    }
}
