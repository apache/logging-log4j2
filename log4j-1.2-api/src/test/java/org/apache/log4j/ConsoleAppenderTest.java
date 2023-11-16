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
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Used to test Log4j 1 support.
 */
public class ConsoleAppenderTest {

    private ConsoleAppender consoleAppender;

    @BeforeEach
    public void beforeEach() {
        consoleAppender = new ConsoleAppender();
    }

    @Test
    public void testFollow() {
        // Only really care that it compiles, behavior is secondary at this level.
        consoleAppender.setFollow(true);
        assertTrue(consoleAppender.getFollow());
    }

    @Test
    public void testTarget() {
        // Only really care that it compiles, behavior is secondary at this level.
        consoleAppender.setTarget(ConsoleAppender.SYSTEM_OUT);
        assertEquals(ConsoleAppender.SYSTEM_OUT, consoleAppender.getTarget());
    }
}
