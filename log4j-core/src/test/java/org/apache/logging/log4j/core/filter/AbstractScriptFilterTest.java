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
package org.apache.logging.log4j.core.filter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Test;

/**
 *
 */
public abstract class AbstractScriptFilterTest {

    public abstract LoggerContextRule getContext();

    @Test
    public void testGroovyFilter() throws Exception {
        final Logger logger = LogManager.getLogger("TestGroovyFilter");
        logger.traceEntry();
        logger.info("This should not be logged");
        ThreadContext.put("UserId", "JohnDoe");
        logger.info("This should be logged");
        ThreadContext.clearMap();
        final ListAppender app = getContext().getListAppender("List");
        try {
            final List<String> messages = app.getMessages();
            assertNotNull("No Messages", messages);
            assertTrue("Incorrect number of messages. Expected 2, Actual " + messages.size(), messages.size() == 2);
        } finally {
            app.clear();
        }
    }

    @Test
    public void testJavascriptFilter() throws Exception {
        final Logger logger = LogManager.getLogger("TestJavaScriptFilter");
        logger.traceEntry();
        logger.info("This should not be logged");
        ThreadContext.put("UserId", "JohnDoe");
        logger.info("This should be logged");
        ThreadContext.clearMap();
        final ListAppender app = getContext().getListAppender("List");
        final List<String> messages = app.getMessages();
        try {
            assertNotNull("No Messages", messages);
            assertTrue("Incorrect number of messages. Expected 2, Actual " + messages.size(), messages.size() == 2);
        } finally {
            app.clear();
        }
    }

}
