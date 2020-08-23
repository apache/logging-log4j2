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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasSize;

@Tag("json")
@LoggerContextSource("log4j-reference-level.json")
public class AppenderRefLevelJsonTest {

    private final ListAppender app1;
    private final ListAppender app2;

    org.apache.logging.log4j.Logger logger1;
    org.apache.logging.log4j.Logger logger2;
    org.apache.logging.log4j.Logger logger3;
    Marker testMarker = MarkerManager.getMarker("TEST");

    public AppenderRefLevelJsonTest(final LoggerContext context, @Named("LIST1") final ListAppender first, @Named("LIST2") final ListAppender second) {
        logger1 = context.getLogger("org.apache.logging.log4j.test1");
        logger2 = context.getLogger("org.apache.logging.log4j.test2");
        logger3 = context.getLogger("org.apache.logging.log4j.test3");
        app1 = first.clear();
        app2 = second.clear();
    }

    @Test
    public void logger1() {
        logger1.traceEntry();
        logger1.debug("debug message");
        logger1.error("Test Message");
        logger1.info("Info Message");
        logger1.warn("warn Message");
        logger1.traceExit();
        assertThat(app1.getEvents(), hasSize(6));
        assertThat(app2.getEvents(), hasSize(1));
    }

    @Test
    public void logger2() {
        logger2.traceEntry();
        logger2.debug("debug message");
        logger2.error("Test Message");
        logger2.info("Info Message");
        logger2.warn("warn Message");
        logger2.traceExit();
        assertThat(app1.getEvents(), hasSize(2));
        assertThat(app2.getEvents(), hasSize(4));
    }

    @Test
    public void logger3() {
        logger3.traceEntry();
        logger3.debug(testMarker, "debug message");
        logger3.error("Test Message");
        logger3.info(testMarker, "Info Message");
        logger3.warn("warn Message");
        logger3.traceExit();
        assertThat(app1.getEvents(), hasSize(4));
    }
}

