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

package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.ReconfigurationPolicy;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests LOG4j2-962.
 */
@LoggerContextSource(value = "log4j2-962.xml", reconfigure = ReconfigurationPolicy.BEFORE_EACH)
public class PatternLayoutMainMapLookupTest {

    static {
        // Must be set before Log4j writes the header to the appenders.
        MainMapLookup.setMainArguments("value0", "value1", "value2");
    }

    @Test
    public void testFileName(@Named("File") final FileAppender fileApp) {
        final String name = fileApp.getFileName();
        assertEquals("target/value0.log", name);
    }

    @Test
    public void testHeader(final LoggerContext context, @Named("List") final ListAppender listApp) {
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
