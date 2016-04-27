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

package org.apache.logging.log4j.jul;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.logging.Logger;

import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ApiLoggerTest extends AbstractLoggerTest {

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
        System.setProperty(Constants.LOGGER_ADAPTOR_PROPERTY, ApiLoggerAdapter.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
        System.clearProperty(Constants.LOGGER_ADAPTOR_PROPERTY);
    }

    @Before
    public void setUp() throws Exception {
        logger = Logger.getLogger(LOGGER_NAME);
        assertThat(logger.getLevel(), equalTo(java.util.logging.Level.FINE));
        eventAppender = ListAppender.getListAppender("TestAppender");
        flowAppender = ListAppender.getListAppender("FlowAppender");
        stringAppender = ListAppender.getListAppender("StringAppender");
        assertNotNull(eventAppender);
        assertNotNull(flowAppender);
        assertNotNull(stringAppender);
    }

    @After
    public void tearDown() throws Exception {
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
    public void testGetParent() throws Exception {
        final Logger parent = logger.getParent();
        assertNull("No parent logger should be automatically set up using log4j-api", parent);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetParentFails() throws Exception {
        logger.setParent(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetLevelFails() throws Exception {
        logger.setLevel(null);
    }
}
