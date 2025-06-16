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
package org.apache.logging.slf4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LoggerContextSource("log4j2-calling-class.xml")
class CallerInformationTest {

    @Test
    void testClassLogger(@Named("Class") final ListAppender app) {
        app.clear();
        final Logger logger = LoggerFactory.getLogger("ClassLogger");
        logger.info("Ignored message contents.");
        logger.warn("Verifying the caller class is still correct.");
        logger.error("Hopefully nobody breaks me!");
        final List<String> messages = app.getMessages();
        assertEquals(3, messages.size(), "Incorrect number of messages.");
        for (final String message : messages) {
            assertEquals(this.getClass().getName(), message, "Incorrect caller class name.");
        }
    }

    @Test
    void testMethodLogger(@Named("Method") final ListAppender app) {
        app.clear();
        final Logger logger = LoggerFactory.getLogger("MethodLogger");
        logger.info("More messages.");
        logger.warn("CATASTROPHE INCOMING!");
        logger.error("ZOMBIES!!!");
        logger.warn("brains~~~");
        logger.info("Itchy. Tasty.");
        final List<String> messages = app.getMessages();
        assertEquals(5, messages.size(), "Incorrect number of messages.");
        for (final String message : messages) {
            assertEquals("testMethodLogger", message, "Incorrect caller method name.");
        }
    }
}
