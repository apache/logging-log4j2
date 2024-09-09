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
package org.apache.logging.jul.tolog4j.test.support;

import static org.junit.Assert.assertTrue;

import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.logging.jul.tolog4j.LogManager;
import org.apache.logging.jul.tolog4j.spi.AbstractLoggerAdapter;
import org.apache.logging.jul.tolog4j.support.AbstractLogger;
import org.apache.logging.jul.tolog4j.test.JulTestProperties;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests if the logger adapter can be customized.
 */
public class CustomLoggerAdapterTest {

    private static final org.apache.logging.log4j.Logger LOGGER = StatusLogger.getLogger();

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
        System.setProperty(JulTestProperties.JUL_LOGGER_ADAPTER, CustomLoggerAdapter.class.getName());
    }

    @AfterClass
    public static void tearDownClass() {
        System.clearProperty("java.util.logging.manager");
        System.clearProperty(JulTestProperties.JUL_LOGGER_ADAPTER);
    }

    @Test
    public void testCustomLoggerAdapter() {
        Logger logger = Logger.getLogger(CustomLoggerAdapterTest.class.getName());
        assertTrue("CustomLoggerAdapter is used", logger instanceof CustomLogger);
    }

    public static class CustomLoggerAdapter extends AbstractLoggerAdapter {

        @Override
        public Logger newLogger(String name, LoggerContext context) {
            return new CustomLogger(context.getLogger(name));
        }
    }

    private static class CustomLogger extends AbstractLogger {

        CustomLogger(ExtendedLogger logger) {
            super(logger);
        }

        @Override
        public void setFilter(Filter newFilter) {}

        @Override
        public void setLevel(final Level newLevel) throws SecurityException {
            LOGGER.error("Cannot set JUL log level through Log4j API: ignoring call to Logger.setLevel({})", newLevel);
        }

        @Override
        public void addHandler(Handler handler) {}

        @Override
        public void removeHandler(Handler handler) {}

        @Override
        public void setUseParentHandlers(boolean useParentHandlers) {}

        @Override
        public void setParent(Logger parent) {}
    }
}
