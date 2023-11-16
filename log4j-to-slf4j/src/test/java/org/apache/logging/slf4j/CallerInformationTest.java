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

import static org.junit.Assert.*;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.testUtil.StringListAppender;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

@UsingStatusListener
@LoggerContextSource
public class CallerInformationTest {

    @Test
    public void testClassLogger() throws Exception {
        final SLF4JLogger logger = (SLF4JLogger) LogManager.getLogger("ClassLogger");
        final StringListAppender<ILoggingEvent> app = TestUtil.getListAppender(logger, "Class");
        logger.info("Ignored message contents.");
        logger.warn("Verifying the caller class is still correct.");
        logger.error("Hopefully nobody breaks me!");
        final List<String> messages = app.strList;
        assertEquals("Incorrect number of messages.", 3, messages.size());
        for (final String message : messages) {
            assertEquals("Incorrect caller class name.", this.getClass().getName(), message);
        }
    }

    @Test
    public void testMethodLogger() throws Exception {
        final SLF4JLogger logger = (SLF4JLogger) LogManager.getLogger("MethodLogger");
        final StringListAppender<ILoggingEvent> app = TestUtil.getListAppender(logger, "Method");
        logger.info("More messages.");
        logger.warn("CATASTROPHE INCOMING!");
        logger.error("ZOMBIES!!!");
        logger.warn("brains~~~");
        logger.info("Itchy. Tasty.");
        final List<String> messages = app.strList;
        assertEquals("Incorrect number of messages.", 5, messages.size());
        for (final String message : messages) {
            assertEquals("Incorrect caller method name.", "testMethodLogger", message);
        }
    }
}
