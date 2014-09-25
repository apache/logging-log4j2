/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.simple;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LogManagerLoggerContextFactoryRule;
import org.junit.ClassRule;
import org.junit.Test;

public class SimpleLoggerTest {

    @ClassRule
    public static final LogManagerLoggerContextFactoryRule rule = new LogManagerLoggerContextFactoryRule(
            new SimpleLoggerContextFactory());

    private final Logger logger = LogManager.getLogger("TestError");

    @Test
    public void testString() {
        logger.error("Logging without args");
    }

    @Test
    public void testMissingMessageArg() {
        logger.error("Logging without args {}");
    }

    @Test
    public void testEmptyObjectArray() {
        logger.error(new Object[0]);
    }

    /**
     * Tests LOG4J2-811.
     */
    @Test
    public void testMessageWithEmptyObjectArray() {
        logger.error("Logging with an empty Object[] {} {}", new Object[0]);
    }

    /**
     * Tests LOG4J2-811.
     */
    @Test
    public void testMessageWithShortArray() {
        logger.error("Logging with a size 1 Object[] {} {}", new Object[] { "only one param" });
    }
}
