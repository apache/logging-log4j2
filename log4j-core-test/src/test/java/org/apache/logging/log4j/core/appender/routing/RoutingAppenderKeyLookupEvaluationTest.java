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
package org.apache.logging.log4j.core.appender.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class RoutingAppenderKeyLookupEvaluationTest {
    private static final String CONFIG = "log4j-routing-lookup.xml";

    private static final String KEY = "user";
    private ListAppender app;

    @Rule
    public final LoggerContextRule loggerContextRule = new LoggerContextRule(CONFIG);

    @Before
    public void setUp() throws Exception {
        ThreadContext.remove(KEY);
        this.app = this.loggerContextRule.getListAppender("List");
    }

    @After
    public void tearDown() throws Exception {
        this.app.clear();
        this.loggerContextRule.getLoggerContext().stop();
        ThreadContext.remove(KEY);
    }

    @Test
    public void testRoutingNoUser() {
        final Logger logger = loggerContextRule.getLogger(getClass());
        logger.warn("no user");
        final String message = app.getMessages().get(0);
        assertEquals("WARN ${ctx:user} no user", message);
    }

    @Test
    public void testRoutingDoesNotMatchRoute() {
        final Logger logger = loggerContextRule.getLogger(getClass());
        ThreadContext.put(KEY, "noRouteExists");
        logger.warn("unmatched user");
        assertTrue(app.getMessages().isEmpty());
    }

    @Test
    public void testRoutingContainsLookup() {
        final Logger logger = loggerContextRule.getLogger(getClass());
        ThreadContext.put(KEY, "${java:version}");
        logger.warn("naughty user");
        final String message = app.getMessages().get(0);
        assertEquals("WARN ${java:version} naughty user", message);
    }

    @Test
    public void testRoutingMatchesEscapedLookup() {
        final Logger logger = loggerContextRule.getLogger(getClass());
        ThreadContext.put(KEY, "${upper:name}");
        logger.warn("naughty user");
        final String message = app.getMessages().get(0);
        assertEquals("WARN ${upper:name} naughty user", message);
    }

    @Test
    public void testRoutesThemselvesNotEvaluated() {
        final Logger logger = loggerContextRule.getLogger(getClass());
        ThreadContext.put(KEY, "NAME");
        logger.warn("unmatched user");
        assertTrue(app.getMessages().isEmpty());
    }
}
