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
package org.apache.logging.log4j.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void testSelectedConstants() {
        assertEquals("Log4jLogEventFactory", Constants.LOG4J_LOG_EVENT_FACTORY);
        assertEquals("Log4jContextSelector", Constants.LOG4J_CONTEXT_SELECTOR);
        assertEquals("Log4jDefaultStatusLevel", Constants.LOG4J_DEFAULT_STATUS_LEVEL);
        assertEquals("java:comp/env/log4j/context-name", Constants.JNDI_CONTEXT_NAME);
        assertEquals("log4j2.Script.enableLanguages", Constants.SCRIPT_LANGUAGES);
        assertEquals(1000, Constants.MILLIS_IN_SECONDS);
        assertEquals(128, Constants.INITIAL_REUSABLE_MESSAGE_SIZE);
        assertEquals(518, Constants.MAX_REUSABLE_MESSAGE_SIZE);
        assertEquals(2048, Constants.ENCODER_CHAR_BUFFER_SIZE);
        assertEquals(8192, Constants.ENCODER_BYTE_BUFFER_SIZE);
    }

    @Test
    void testMaxReusableMessageSizeFormula() {
        assertTrue(Constants.MAX_REUSABLE_MESSAGE_SIZE > Constants.INITIAL_REUSABLE_MESSAGE_SIZE);
    }
}
