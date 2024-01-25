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
package org.apache.logging.log4j.core.test.config;

import static org.apache.logging.log4j.util.Unbox.box;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.ThreadContextMapFilter;
import org.apache.logging.log4j.util.Strings;

/**
 * Common base class for configuration factory tests.
 */
public abstract class AbstractConfigurationFactoryTest {

    private static final String LOGGER_NAME = "org.apache.logging.log4j.test1.Test";
    private static final String FILE_LOGGER_NAME = "org.apache.logging.log4j.test2.Test";
    private static final String APPENDER_NAME = "STDOUT";

    /**
     * Runs various configuration checks on a configured LoggerContext that should match the equivalent configuration in
     * {@code log4j-test1.xml}.
     */
    protected static void checkConfiguration(final LoggerContext context) {
        final Configuration configuration = context.getConfiguration();
        final Map<String, Appender> appenders = configuration.getAppenders();
        // these used to be separate tests
        assertAll(
                () -> assertNotNull(appenders),
                () -> assertEquals(3, appenders.size()),
                () -> assertNotNull(configuration.getLoggerContext()),
                () -> assertEquals(configuration.getRootLogger(), configuration.getLoggerConfig(Strings.EMPTY)),
                () -> assertThrows(NullPointerException.class, () -> configuration.getLoggerConfig(null)));

        final Logger logger = context.getLogger(LOGGER_NAME);
        assertEquals(Level.DEBUG, logger.getLevel());

        assertEquals(1, logger.filterCount());
        final Iterator<Filter> filterIterator = logger.getFilters();
        assertTrue(filterIterator.hasNext());
        assertTrue(filterIterator.next() instanceof ThreadContextMapFilter);

        final Appender appender = appenders.get(APPENDER_NAME);
        assertTrue(appender instanceof ConsoleAppender);
        assertEquals(APPENDER_NAME, appender.getName());
    }

    protected static void checkFileLogger(final LoggerContext context, final Path logFile) throws IOException {
        final long currentThreadId = Thread.currentThread().getId();
        final Logger logger = context.getLogger(FILE_LOGGER_NAME);
        logger.debug("Greetings from ConfigurationFactoryTest in thread#{}", box(currentThreadId));
        final List<String> lines = Files.readAllLines(logFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).endsWith(Long.toString(currentThreadId)));
    }
}
