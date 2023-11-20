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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.test.junit.UsingThreadContextMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j-routing-lookup.xml")
@UsingThreadContextMap
public class RoutingAppenderKeyLookupEvaluationTest {

    private static final String KEY = "user";

    private final LoggerContext context;
    private final ListAppender app;

    public RoutingAppenderKeyLookupEvaluationTest(final LoggerContext context, @Named("List") final ListAppender app) {
        this.context = context;
        this.app = app.clear();
    }

    @BeforeEach
    public void setUp() throws Exception {
        ThreadContext.remove(KEY);
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.app.clear();
        ThreadContext.remove(KEY);
    }

    @Test
    public void testRoutingNoUser() {
        final Logger logger = context.getLogger(getClass());
        logger.warn("no user");
        assertThat(app.getMessages()).contains("WARN ${ctx:user} no user");
    }

    @Test
    public void testRoutingDoesNotMatchRoute() {
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put(KEY, "noRouteExists");
        logger.warn("unmatched user");
        assertThat(app.getMessages()).isEmpty();
    }

    @Test
    public void testRoutingContainsLookup() {
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put(KEY, "${java:version}");
        logger.warn("naughty user");
        assertThat(app.getMessages()).contains("WARN ${java:version} naughty user");
    }

    @Test
    public void testRoutingMatchesEscapedLookup() {
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put(KEY, "${upper:name}");
        logger.warn("naughty user");
        assertThat(app.getMessages()).contains("WARN ${upper:name} naughty user");
    }

    @Test
    public void testRoutesThemselvesNotEvaluated() {
        final Logger logger = context.getLogger(getClass());
        ThreadContext.put(KEY, "NAME");
        logger.warn("unmatched user");
        assertThat(app.getMessages()).isEmpty();
    }
}
