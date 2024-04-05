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
package org.apache.logging.log4j.core.impl.internal;

import org.apache.logging.log4j.test.spi.ScopedContextProviderSuite;
import org.junit.jupiter.api.Test;

class DefaultScopedContextProviderTest extends ScopedContextProviderSuite {

    private static QueuedScopedContextProvider createProvider() {
        return new QueuedScopedContextProvider();
    }

    @Test
    void testScope() {
        testScope(createProvider());
    }

    @Test
    void testRunWhere() {
        testRunWhere(createProvider());
    }

    @Test
    void testRunThreads() {
        testRunThreads(createProvider());
    }

    @Test
    void testThreads() throws Exception {
        testThreads(createProvider());
    }

    @Test
    void testThreadException() throws Exception {
        testThreadException(createProvider());
    }

    @Test
    void testThreadCall() throws Exception {
        testThreadCall(createProvider());
    }
}
