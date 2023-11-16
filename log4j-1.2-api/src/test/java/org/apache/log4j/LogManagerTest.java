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
package org.apache.log4j;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

/**
 * Tests {@link LogManager}.
 */
public class LogManagerTest {

    private static final String SIMPLE_NAME = LogManagerTest.class.getSimpleName();

    List<String> getCurrentLoggerNames() {
        return Collections.list((Enumeration<Logger>) LogManager.getCurrentLoggers()).stream()
                .map(Logger::getName)
                .collect(Collectors.toList());
    }

    @Test
    public void testGetCurrentLoggers() {
        Logger.getLogger(SIMPLE_NAME);
        Logger.getLogger(SIMPLE_NAME + ".foo");
        Logger.getLogger(SIMPLE_NAME + ".foo.bar");
        final List<String> names = getCurrentLoggerNames();
        assertTrue(names.contains(SIMPLE_NAME));
        assertTrue(names.contains(SIMPLE_NAME + ".foo"));
        assertTrue(names.contains(SIMPLE_NAME + ".foo.bar"));
    }
}
