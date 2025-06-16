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
package org.apache.logging.log4j.jul.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.logging.Logger;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.jul.LogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiLoggerTest extends AbstractLoggerTest {

    @BeforeAll
    static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
    }

    @AfterAll
    static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
    }

    @BeforeEach
    void setUp() {
        logger = Logger.getLogger(LOGGER_NAME);
        logger.setFilter(null);
        assertThat(getEffectiveLevel(logger), equalTo(java.util.logging.Level.FINE));
        eventAppender = ListAppender.getListAppender("TestAppender");
        flowAppender = ListAppender.getListAppender("FlowAppender");
        stringAppender = ListAppender.getListAppender("StringAppender");
        assertNotNull(eventAppender);
        assertNotNull(flowAppender);
        assertNotNull(stringAppender);
    }

    @AfterEach
    void tearDown() {
        if (eventAppender != null) {
            eventAppender.clear();
        }
        if (flowAppender != null) {
            flowAppender.clear();
        }
        if (stringAppender != null) {
            stringAppender.clear();
        }
    }

    @Test
    void testGetParent() {
        final Logger parent = logger.getParent();
        assertNull(parent, "No parent logger should be automatically set up using log4j-api");
    }

    @Test
    void testSetParentFails() {
        assertThrows(UnsupportedOperationException.class, () -> logger.setParent(null));
    }

    @Test
    void testSetLevelFails() {
        logger.setLevel(null);
    }
}
