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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ReliabilityStrategyTest {

    @BeforeAll
    static void setUp() {
        System.setProperty("log4j2.reliabilityStrategy", MockReliabilityStrategy.class.getName());
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("log4j2.reliabilityStrategy");
    }

    @Test
    @LoggerContextSource("ReliabilityStrategyTest.xml")
    void beforeStopAppendersCalledBeforeAsyncAppendersStopped(
            @Named final AsyncAppender async, final Configuration config) {
        assertTrue(async.isStarted());
        final MockReliabilityStrategy reliabilityStrategy =
                (MockReliabilityStrategy) config.getRootLogger().getReliabilityStrategy();
        config.stop();
        assertTrue(async.isStopped());
        reliabilityStrategy.rethrowAssertionErrors();
    }
}
