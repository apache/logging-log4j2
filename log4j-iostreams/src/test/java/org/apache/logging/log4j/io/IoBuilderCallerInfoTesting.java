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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;

import static org.junit.Assert.*;

public class IoBuilderCallerInfoTesting {

    protected static Logger getExtendedLogger() {
        return ctx.getLogger("ClassAndMethodLogger");
    }
    
    protected static Logger getLogger() {
        return getExtendedLogger();
    }
    
    protected final static Level LEVEL = Level.WARN;

    @ClassRule
    public static LoggerContextRule ctx = new LoggerContextRule("log4j2-streams-calling-info.xml");

    public void assertMessages(final String msg, final int size, final String methodName) {
        final ListAppender appender = ctx.getListAppender("ClassAndMethod");
        assertEquals(msg + ".size", size, appender.getMessages().size());
        for (final String message : appender.getMessages()) {
            assertEquals(msg + " has incorrect caller info", this.getClass().getName() + '.' + methodName, message);
        }
    }

    @Before
    public void clearAppender() {
        ctx.getListAppender("ClassAndMethod").clear();
    }
}
