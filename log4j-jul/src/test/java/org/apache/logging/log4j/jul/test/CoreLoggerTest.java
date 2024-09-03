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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.jul.Constants;
import org.apache.logging.log4j.jul.CoreLoggerAdapter;
import org.apache.logging.log4j.jul.LogManager;
import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CoreLoggerTest extends AbstractLoggerTest {

    private static final Level[] LEVELS = new Level[] {
        Level.ALL,
        Level.FINEST,
        Level.FINER,
        Level.FINE,
        Level.CONFIG,
        Level.INFO,
        Level.WARNING,
        Level.SEVERE,
        Level.OFF
    };

    private static Level getEffectiveLevel(final Logger logger) {
        for (final Level level : LEVELS) {
            if (logger.isLoggable(level)) {
                return level;
            }
        }
        throw new RuntimeException("No level is enabled.");
    }

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
        System.setProperty(Constants.LOGGER_ADAPTOR_PROPERTY, CoreLoggerAdapter.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
        System.clearProperty(Constants.LOGGER_ADAPTOR_PROPERTY);
    }

    @Before
    public void setUp() {
        // Reset the logger context
        LoggerContext.getContext(false).reconfigure();

        logger = Logger.getLogger(LOGGER_NAME);
        logger.setFilter(null);
        assertThat(logger.getLevel()).isEqualTo(Level.FINE);
        eventAppender = ListAppender.getListAppender("TestAppender");
        flowAppender = ListAppender.getListAppender("FlowAppender");
        stringAppender = ListAppender.getListAppender("StringAppender");
        assertThat(eventAppender).isNotNull();
        assertThat(flowAppender).isNotNull();
        assertThat(stringAppender).isNotNull();
    }

    @After
    public void tearDown() {
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
    public void testRootSetLevelToNull() {
        final Logger rootLogger = Logger.getLogger(Strings.EMPTY);
        assertThat(rootLogger.getLevel()).isEqualTo(Level.SEVERE);
        assertThat(getEffectiveLevel(rootLogger)).isEqualTo(Level.SEVERE);
        // null test
        rootLogger.setLevel(null);
        assertThat(rootLogger.getLevel()).isNull();
        assertThat(getEffectiveLevel(rootLogger)).isEqualTo(Level.SEVERE);
        // now go back to a different one
        rootLogger.setLevel(Level.INFO);
        assertThat(rootLogger.getLevel()).isEqualTo(Level.INFO);
        assertThat(getEffectiveLevel(rootLogger)).isEqualTo(Level.INFO);
    }

    @Test
    public void testSetLevel() {
        final Logger a = Logger.getLogger("a");
        final Logger a_b = Logger.getLogger("a.b");
        final Logger a_b_c = Logger.getLogger("a.b.c");
        // test default for this test
        assertThat(a.getLevel()).isNull();
        assertThat(a_b.getLevel()).isNull();
        assertThat(a_b_c.getLevel()).isNull();
        // all levels
        for (final Level level : LEVELS) {
            a.setLevel(level);
            assertThat(a.getLevel()).isEqualTo(level);
            assertThat(getEffectiveLevel(a)).isEqualTo(level);
            assertThat(a_b.getLevel()).isNull();
            assertThat(getEffectiveLevel(a_b)).isEqualTo(level);
            assertThat(a_b_c.getLevel()).isNull();
            assertThat(getEffectiveLevel(a_b_c)).isEqualTo(level);
        }
    }

    @Test
    public void testSetLevelToNull() {
        final Logger childLogger = Logger.getLogger(LOGGER_NAME + ".NullChild");
        assertThat(childLogger.getLevel()).isNull();
        assertThat(getEffectiveLevel(childLogger)).isEqualTo(Level.FINE);
        // Set explicit level
        childLogger.setLevel(Level.SEVERE);
        assertThat(childLogger.getLevel()).isEqualTo(Level.SEVERE);
        assertThat(getEffectiveLevel(childLogger)).isEqualTo(Level.SEVERE);
        // Set null level
        childLogger.setLevel(null);
        assertThat(childLogger.getLevel()).isNull();
        assertThat(getEffectiveLevel(childLogger)).isEqualTo(Level.FINE);
        // now go back
        childLogger.setLevel(Level.SEVERE);
        assertThat(childLogger.getLevel()).isEqualTo(Level.SEVERE);
        assertThat(getEffectiveLevel(childLogger)).isEqualTo(Level.SEVERE);
    }
}
