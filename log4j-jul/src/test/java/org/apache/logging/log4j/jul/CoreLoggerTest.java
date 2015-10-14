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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.logging.log4j.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CoreLoggerTest extends AbstractLoggerTest {

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
    }

    @Before
    public void setUp() throws Exception {
        logger = Logger.getLogger(LOGGER_NAME);
        assertThat(logger.getLevel(), equalTo(Level.FINE));
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
    public void testRootSetLevelToNull() throws Exception {
        final Logger rootLogger = Logger.getLogger(Strings.EMPTY);
        assertThat(rootLogger.getLevel(), equalTo(Level.SEVERE));
        assertThat(rootLogger.isLoggable(Level.SEVERE), is(true));
        // null test
        rootLogger.setLevel(null);
        assertThat(rootLogger.getLevel(), equalTo(null));
        assertThat(rootLogger.isLoggable(Level.SEVERE), is(true));
        // now go back to a different one
        rootLogger.setLevel(Level.INFO);
        assertThat(rootLogger.getLevel(), equalTo(Level.INFO));
        assertThat(rootLogger.isLoggable(Level.FINE), is(false));
    }

    @Test
    public void testSetLevel() throws Exception {
        final Logger childLogger = Logger.getLogger(LOGGER_NAME + ".Child");
        assertThat(childLogger.getLevel(), equalTo(Level.FINE));
        logger.setLevel(Level.SEVERE);
        assertThat(childLogger.getLevel(), equalTo(Level.FINE));
        assertThat(logger.getLevel(), equalTo(Level.SEVERE));
        logger.setLevel(Level.FINER);
        assertThat(logger.getLevel(), equalTo(Level.FINER));
        logger.setLevel(Level.FINE);
        assertThat(logger.getLevel(), equalTo(Level.FINE));
        assertThat(childLogger.getLevel(), equalTo(Level.FINE));
        assertThat(childLogger.isLoggable(Level.ALL), is(false));
    }

    @Test
    public void testSetLevelToNull() throws Exception {
        final Logger childLogger = Logger.getLogger(LOGGER_NAME + ".NullChild");
        assertThat(childLogger.getLevel(), equalTo(Level.FINE));
        assertThat(childLogger.isLoggable(Level.FINE), is(true));
        childLogger.setLevel(Level.SEVERE);
        assertThat(childLogger.getLevel(), equalTo(Level.SEVERE));
        assertThat(childLogger.isLoggable(Level.FINE), is(false));
        // null test
        childLogger.setLevel(null);
        assertThat(childLogger.getLevel(), equalTo(null));
        assertThat(childLogger.isLoggable(Level.FINE), is(true));
        // now go back
        childLogger.setLevel(Level.SEVERE);
        assertThat(childLogger.getLevel(), equalTo(Level.SEVERE));
        assertThat(childLogger.isLoggable(Level.FINE), is(false));
    }

}