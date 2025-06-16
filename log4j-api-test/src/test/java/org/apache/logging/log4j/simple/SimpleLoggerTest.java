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
package org.apache.logging.log4j.simple;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.test.junit.LoggerContextFactoryExtension;
import org.apache.logging.log4j.util.Constants;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Tag("smoke")
class SimpleLoggerTest {

    @RegisterExtension
    public static final LoggerContextFactoryExtension EXTENSION =
            new LoggerContextFactoryExtension(SimpleLoggerContextFactory.INSTANCE);

    private final Logger logger = LogManager.getLogger("TestError");

    @Test
    void testString() {
        logger.error("Logging without args");
    }

    @Test
    void testMissingMessageArg() {
        logger.error("Logging without args {}");
    }

    @Test
    void testEmptyObjectArray() {
        logger.error(Constants.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Tests LOG4J2-811.
     */
    @Test
    void testMessageWithEmptyObjectArray() {
        logger.error("Logging with an empty Object[] {} {}", Constants.EMPTY_BYTE_ARRAY);
    }

    /**
     * Tests LOG4J2-811.
     */
    @Test
    void testMessageWithShortArray() {
        logger.error("Logging with a size 1 Object[] {} {}", new Object[] {"only one param"});
    }
}
