/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.io;

import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.junit.Before;
import org.junit.ClassRule;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.*;

public abstract class AbstractStreamTest {

    protected static ExtendedLogger getExtendedLogger() {
        return ctx.getLogger("UnitTestLogger");
    }
    
    protected final static String NEWLINE = System.lineSeparator();
    protected final static Level LEVEL = Level.ERROR;
    protected final static String FIRST = "first";

    protected final static String LAST = "last";

    @ClassRule
    public static LoggerContextRule ctx = new LoggerContextRule("log4j2-streams-unit-test.xml");

    protected void assertMessages(final String... messages) {
        final List<String> actualMsgs = ctx.getListAppender("UnitTest").getMessages();
        assertEquals("Unexpected number of results.", messages.length, actualMsgs.size());
        for (int i = 0; i < messages.length; i++) {
            final String start = LEVEL.name() + ' ' + messages[i];
            assertThat(actualMsgs.get(i), startsWith(start));
        }
    }

    @Before
    public void clearAppender() {
        ctx.getListAppender("UnitTest").clear();
    }
}
