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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;

class ConsoleAppenderBuilderTest {

    /**
     * Tests https://issues.apache.org/jira/browse/LOG4J2-1620
     */
    @Test
    void testDefaultImmediateFlush() {
        assertTrue(ConsoleAppender.newBuilder().isImmediateFlush());
    }

    /**
     * Tests https://issues.apache.org/jira/browse/LOG4J2-1636
     *
     * Tested with Oracle 7 and 8 and IBM Java 8.
     */
    @Test
    void testDefaultLayoutDefaultCharset() {
        final ConsoleAppender appender =
                ConsoleAppender.newBuilder().setName("test").build();
        final PatternLayout layout = (PatternLayout) appender.getLayout();
        final String charsetName = System.getProperty("sun.stdout.encoding");
        final String expectedName =
                charsetName != null ? charsetName : Charset.defaultCharset().name();
        assertEquals(expectedName, layout.getCharset().name());
    }

    /**
     * Tests https://issues.apache.org/jira/browse/LOG4J2-2441
     */
    @Test
    void testSetNullErrorHandlerIsNotAllowed() {
        final ConsoleAppender appender =
                ConsoleAppender.newBuilder().setName("test").build();
        final ErrorHandler handler = appender.getHandler();
        assertNotNull(handler);
        // This could likely be allowed to throw, but we're just testing that
        // setting null does not actually set a null handler.
        appender.setHandler(null);
        assertSame(handler, appender.getHandler());
    }
}
