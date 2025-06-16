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
package org.apache.logging.log4j.core.async;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Test class loading deadlock condition from the LOG4J2-1457
 */
@Tag(Tags.ASYNC_LOGGERS)
class AsyncLoggerClassLoadDeadlockTest {

    static final int RING_BUFFER_SIZE = 128;

    @BeforeAll
    static void beforeClass() {
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("AsyncLogger.RingBufferSize", String.valueOf(RING_BUFFER_SIZE));
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "AsyncLoggerConsoleTest.xml");
    }

    @Test
    @Timeout(value = 30000, unit = TimeUnit.MILLISECONDS)
    void testClassLoaderDeadlock() {
        // touch the class so static init will be called
        final AsyncLoggerClassLoadDeadlock temp = new AsyncLoggerClassLoadDeadlock();
        assertNotNull(temp);
    }
}
