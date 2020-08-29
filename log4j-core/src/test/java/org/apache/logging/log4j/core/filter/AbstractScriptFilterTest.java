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

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.UsingThreadContextMap;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@UsingThreadContextMap
@Tag("groovy")
public abstract class AbstractScriptFilterTest {

    @Test
    public void testGroovyFilter(final LoggerContext context, @Named("List") final ListAppender app) throws Exception {
        final Logger logger = context.getLogger("TestGroovyFilter");
        logger.traceEntry();
        logger.info("This should not be logged");
        ThreadContext.put("UserId", "JohnDoe");
        logger.info("This should be logged");
        ThreadContext.clearMap();
        try {
            final List<String> messages = app.getMessages();
            assertNotNull(messages, "No Messages");
            assertEquals(messages.size(), 2, "Incorrect number of messages. Expected 2, Actual " + messages.size());
        } finally {
            app.clear();
        }
    }

    @Test
    public void testJavascriptFilter(final LoggerContext context, @Named("List") final ListAppender app) throws Exception {
        final Logger logger = context.getLogger("TestJavaScriptFilter");
        logger.traceEntry();
        logger.info("This should not be logged");
        ThreadContext.put("UserId", "JohnDoe");
        logger.info("This should be logged");
        ThreadContext.clearMap();
        final List<String> messages = app.getMessages();
        try {
            assertNotNull(messages, "No Messages");
            assertEquals(messages.size(), 2, "Incorrect number of messages. Expected 2, Actual " + messages.size());
        } finally {
            app.clear();
        }
    }

}
