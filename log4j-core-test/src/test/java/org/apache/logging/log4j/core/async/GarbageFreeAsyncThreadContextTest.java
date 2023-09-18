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

import java.nio.file.Path;

import org.apache.logging.log4j.test.TestProperties;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.apache.logging.log4j.test.junit.UsingTestProperties;
import org.junit.jupiter.api.Test;

/**
 * Provided as a dedicated test as it depends on cached state during Log4j startup.
 */
@UsingStatusListener
@UsingTestProperties
class GarbageFreeAsyncThreadContextTest {

    private static TestProperties props;

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    void garbageFreeMixed() throws Exception {
        AsyncThreadContextTest.doTestAsyncLogWritesToLog(AsyncThreadContextTest.ContextImpl.GARBAGE_FREE,
                AsyncThreadContextTest.Mode.MIXED, getClass(), loggingPath, props);
    }
}
