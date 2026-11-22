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
package org.apache.logging.log4j.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.jupiter.api.BeforeEach;

@LoggerContextSource("log4j2-streams-unit-test.xml")
public abstract class AbstractStreamTest {

    private LoggerContext context = null;

    AbstractStreamTest(LoggerContext context) {
        this.context = context;
    }

    protected ExtendedLogger getExtendedLogger() {
        return context.getLogger("UnitTestLogger");
    }

    protected static final String NEWLINE = System.lineSeparator();
    protected static final Level LEVEL = Level.ERROR;
    protected static final String FIRST = "first";

    protected static final String LAST = "last";

    protected void assertMessages(final String... messages) {
        ListAppender listApp = context.getConfiguration().getAppender("UnitTest");
        final List<String> actualMsgs = listApp.getMessages();
        assertEquals(messages.length, actualMsgs.size(), "Unexpected number of results.");
        for (int i = 0; i < messages.length; i++) {
            final String start = LEVEL.name() + ' ' + messages[i];
            assertThat(actualMsgs.get(i), startsWith(start));
        }
    }

    @BeforeEach
    public void clearAppender() {
        ListAppender listApp = context.getConfiguration().getAppender("UnitTest");
        listApp.clear();
    }
}
