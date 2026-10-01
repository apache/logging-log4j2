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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.BeforeEach;

@LoggerContextSource("log4j2-streams-calling-info.xml")
public class IoBuilderCallerInfoTesting {

    private LoggerContext context = null;

    IoBuilderCallerInfoTesting(LoggerContext context) {
        this.context = context;
    }

    protected Logger getExtendedLogger() {
        return context.getLogger("ClassAndMethodLogger");
    }

    protected Logger getLogger() {
        return getExtendedLogger();
    }

    protected static final Level LEVEL = Level.WARN;

    public void assertMessages(final String msg, final int size, final String methodName) {
        final ListAppender appender = context.getConfiguration().getAppender("ClassAndMethod");
        assertEquals(size, appender.getMessages().size(), msg + ".size");
        for (final String message : appender.getMessages()) {
            assertEquals(this.getClass().getName() + '.' + methodName, message, msg + " has incorrect caller info");
        }
    }

    @BeforeEach
    public void clearAppender() {
        ListAppender listApp = context.getConfiguration().getAppender("ClassAndMethod");
        listApp.clear();
    }
}
