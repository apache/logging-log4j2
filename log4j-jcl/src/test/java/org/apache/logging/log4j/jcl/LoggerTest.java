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
package org.apache.logging.log4j.jcl;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;

/**
 *
 */
public class LoggerTest {

    private static final String CONFIG = "log4j-test1.xml";

    @Rule
    public final InitialLoggerContext context = new InitialLoggerContext(CONFIG);

    @Test
    public void testLog() {
        final Log logger = LogFactory.getLog("LoggerTest");
        logger.debug("Test message");
        verify("List", "o.a.l.l.j.LoggerTest Test message MDC{}" + Constants.LINE_SEPARATOR);
        logger.debug("Exception: " , new NullPointerException("Test"));
        verify("List", "o.a.l.l.j.LoggerTest Exception:  MDC{}" + Constants.LINE_SEPARATOR);
        logger.info("Info Message");
        verify("List", "o.a.l.l.j.LoggerTest Info Message MDC{}" + Constants.LINE_SEPARATOR);
        logger.info("Info Message {}");
        verify("List", "o.a.l.l.j.LoggerTest Info Message {} MDC{}" + Constants.LINE_SEPARATOR);
    }

    private void verify(final String name, final String expected) {
        final ListAppender listApp = context.getListAppender(name);
        final List<String> events = listApp.getMessages();
        assertThat(events, hasSize(1));
        final String actual = events.get(0);
        assertThat(actual, equalTo(expected));
        listApp.clear();
    }

}
