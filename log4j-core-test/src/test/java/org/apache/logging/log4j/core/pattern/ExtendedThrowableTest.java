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
package org.apache.logging.log4j.core.pattern;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-throwablefilter.xml")
public class ExtendedThrowableTest {
    private ListAppender app;

    @BeforeEach
    public void setUp(@Named("List") final ListAppender app) throws Exception {
        this.app = app.clear();
    }

    @Test
    public void testException(final LoggerContext context) {
        final Logger logger = context.getLogger("LoggerTest");
        final Throwable cause = new NullPointerException("null pointer");
        final Throwable parent = new IllegalArgumentException("IllegalArgument", cause);
        logger.error("Exception", parent);
        final List<String> msgs = app.getMessages();
        assertNotNull(msgs);
        assertEquals(1, msgs.size(), "Incorrect number of messages. Should be 1 is " + msgs.size());
        assertTrue(msgs.get(0).contains("suppressed"), "No suppressed lines");
    }
}
