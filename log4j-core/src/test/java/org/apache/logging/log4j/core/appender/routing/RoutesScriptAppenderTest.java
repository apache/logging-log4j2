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
package org.apache.logging.log4j.core.appender.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 */
@RunWith(Parameterized.class)
public class RoutesScriptAppenderTest {

    @Parameterized.Parameters(name = "{0}")
    public static String[] getParameters() {
        return new String[] { 
                "log4j-routing-routes-script-groovy.xml",
                "log4j-routing-routes-script-javascript.xml" };
    }

    @Rule
    public final LoggerContextRule loggerContextRule;

    public RoutesScriptAppenderTest(final String configLocation) {
        this.loggerContextRule = new LoggerContextRule(configLocation);
    }

    private ListAppender getListAppender() {
        final String key = "Service2";
        final RoutingAppender routingAppender = getRoutingAppender();
        Assert.assertTrue(routingAppender.isStarted());
        final Map<String, AppenderControl> appenders = routingAppender.getAppenders();
        final AppenderControl appenderControl = appenders.get(key);
        assertNotNull("No appender control generated for '" + key + "'; appenders = " + appenders, appenderControl);
        final ListAppender listAppender = (ListAppender) appenderControl.getAppender();
        return listAppender;
    }

    private RoutingAppender getRoutingAppender() {
        return loggerContextRule.getRequiredAppender("Routing", RoutingAppender.class);
    }

    private void logAndCheck() {
        final Logger logger = loggerContextRule.getLogger(RoutesScriptAppenderTest.class);
        logger.error("Hello");
        final ListAppender listAppender = getListAppender();
        final List<LogEvent> list = listAppender.getEvents();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 1, got " + list.size(), list.size() == 1);
        logger.error("World");
        assertTrue("Incorrect number of events. Expected 2, got " + list.size(), list.size() == 2);
    }

    @Test(expected = AssertionError.class)
    public void testAppenderAbsence() {
        loggerContextRule.getListAppender("List1");
    }

    @Test
    public void testListAppenderPresence() {
        // No appender until an event is routed, even thought we initialized the default route on startup.
        Assert.assertNull("No appender control generated", getRoutingAppender().getAppenders().get("Service2"));
    }

    @Test
    public void testNoPurgePolicy() {
        // No PurgePolicy in this test
        Assert.assertNull("Unexpected PurgePolicy", getRoutingAppender().getPurgePolicy());
    }

    @Test
    public void testNoRewritePolicy() {
        // No RewritePolicy in this test
        Assert.assertNull("Unexpected RewritePolicy", getRoutingAppender().getRewritePolicy());
    }

    @Test
    public void testRoutingAppenderRoutes() {
        final RoutingAppender routingAppender = getRoutingAppender();
        Assert.assertNull(routingAppender.getDefaultRouteScript());
        Assert.assertNull(routingAppender.getDefaultRoute());
        final Routes routes = routingAppender.getRoutes();
        Assert.assertNotNull(routes);
        Assert.assertNotNull(routes.getPatternScript());
        Assert.assertEquals("Service2", routes.getPattern());
    }

    @Test
    public void testRoutingAppenderPresence() {
        getRoutingAppender();
    }

    @Test
    public void testRoutingPresence1() {
        logAndCheck();
    }

    @Test
    public void testRoutingPresence2() {
        logAndCheck();
    }
}
