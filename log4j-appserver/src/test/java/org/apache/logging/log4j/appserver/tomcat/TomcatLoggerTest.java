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
package org.apache.logging.log4j.appserver.tomcat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j2-listAppender.xml")
public class TomcatLoggerTest {

    private static final String MESSAGE = "Hello world!";

    @Test
    public void testMessageForwarding(@Named("List") final ListAppender app) {
        final Log juliLog = LogFactory.getLog(TomcatLoggerTest.class);
        juliLog.debug(MESSAGE);
        List<LogEvent> events = app.getEvents();
        assertEquals(1, events.size());
        Object[] parameters = events.get(0).getMessage().getParameters();
        assertEquals(1, parameters.length);
        assertEquals(MESSAGE, parameters[0]);
    }

}
