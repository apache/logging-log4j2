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
package org.apache.logging.log4j.core.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.core.test.junit.ReconfigurationPolicy;
import org.junit.jupiter.api.Test;

/**
 * Tests LOG4j2-962.
 */
@LoggerContextSource(value = "log4j2-962.xml", reconfigure = ReconfigurationPolicy.BEFORE_EACH)
class PatternLayoutMainMapLookupTest {

    static {
        // Must be set before Log4j writes the header to the appenders.
        MainMapLookup.setMainArguments("value0", "value1", "value2");
    }

    @Test
    void testFileName(@Named("File") final FileAppender fileApp) {
        final String name = fileApp.getFileName();
        assertEquals("target/value0.log", name);
    }

    @Test
    void testHeader(final LoggerContext context, @Named("List") final ListAppender listApp) {
        final Logger logger = context.getLogger(getClass());
        logger.info("Hello World");
        final List<String> initialMessages = listApp.getMessages();
        assertFalse(initialMessages.isEmpty());
        final String messagesStr = initialMessages.toString();
        assertEquals("Header: value0", initialMessages.get(0), messagesStr);
        listApp.stop();
        final List<String> finalMessages = listApp.getMessages();
        assertEquals(3, finalMessages.size());
        assertEquals("Footer: value1", finalMessages.get(2));
        listApp.clear();
    }
}
