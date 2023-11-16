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
package org.apache.logging.log4j.core.appender;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

@SetSystemProperty(key = Constants.SCRIPT_LANGUAGES, value = "Groovy, Javascript")
public class ScriptAppenderSelectorTest {

    @Test
    @LoggerContextSource("log4j-appender-selector-javascript.xml")
    void testJavaScriptSelector(final Configuration config) {
        verify(config);
    }

    @Test
    @LoggerContextSource("log4j-appender-selector-groovy.xml")
    void testGroovySelector(final Configuration config) {
        verify(config);
    }

    static void verify(final Configuration config) {
        assertNull(config.getAppender("List1"), "List1 appender should not be initialized");
        assertNull(config.getAppender("List2"), "List2 appender should not be initialized");
        final ListAppender listAppender = config.getAppender("SelectIt");
        assertNotNull(listAppender);
        final ExtendedLogger logger = config.getLoggerContext().getLogger(ScriptAppenderSelectorTest.class);
        logger.error("Hello");
        assertThat(listAppender.getEvents(), hasSize(1));
        logger.error("World");
        assertThat(listAppender.getEvents(), hasSize(2));
        logger.error(MarkerManager.getMarker("HEXDUMP"), "DEADBEEF");
        assertThat(listAppender.getEvents(), hasSize(3));
    }
}
