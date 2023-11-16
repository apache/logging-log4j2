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
package org.apache.logging.log4j.core.async;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.categories.AsyncLoggers;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(AsyncLoggers.class)
public class AsyncRootLoggerDefaultLocationTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "AsyncRootLoggerDefaultLocationTest.xml");
    }

    @AfterClass
    public static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
    }

    @Test
    public void testAsyncLogWritesToLog() throws Exception {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final ListAppender app = context.getConfiguration().getAppender("List");
        assertNotNull(app);
        final Logger log = context.getLogger("com.foo.Bar");
        final String msg = "Async root logger msg with no location by default";
        log.info(msg);
        context.stop();
        assertEquals(1, app.getEvents().size());
        final LogEvent event = app.getEvents().get(0);
        assertFalse("includeLocation should be false", event.isIncludeLocation());
        assertNull("Location data should not be present", event.getSource());
    }
}
