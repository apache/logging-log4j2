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

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

/**
 *
 */
@RunWith(Parameterized.class)
public class DefaultRouteScriptAppenderTest {

    @Parameterized.Parameters(name = "{0} {1}")
    public static Object[][] getParameters() {
        // @formatter:off
        return new Object[][] {
                { "log4j-routing-default-route-script-groovy.xml", false },
                { "log4j-routing-default-route-script-javascript.xml", false },
                { "log4j-routing-script-staticvars-javascript.xml", true },
                { "log4j-routing-script-staticvars-groovy.xml", true },
        };
        // @formatter:on
    }

    @Rule
    public final LoggerContextRule loggerContextRule;

    private final boolean expectBindingEntries;

    public DefaultRouteScriptAppenderTest(final String configLocation, final boolean expectBindingEntries) {
        this.loggerContextRule = new LoggerContextRule(configLocation);
        this.expectBindingEntries = expectBindingEntries;
    }

    private void checkStaticVars() {
        final RoutingAppender routingAppender = getRoutingAppender();
        final ConcurrentMap<Object, Object> map = routingAppender.getScriptStaticVariables();
        if (expectBindingEntries) {
            assertEquals("TestValue2", map.get("TestKey"));
            assertEquals("HEXDUMP", map.get("MarkerName"));
        }
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
        final Marker marker = MarkerManager.getMarker("HEXDUMP");
        final Logger logger = loggerContextRule.getLogger(DefaultRouteScriptAppenderTest.class);
        logger.error("Hello");
        final ListAppender listAppender = getListAppender();
        assertEquals("Incorrect number of events", 1, listAppender.getEvents().size());
        logger.error("World");
        assertEquals("Incorrect number of events", 2, listAppender.getEvents().size());
        logger.error(marker, "DEADBEEF");
        assertEquals("Incorrect number of events", 3, listAppender.getEvents().size());
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
    public void testRoutingAppenderDefaultRouteKey() {
        final RoutingAppender routingAppender = getRoutingAppender();
        Assert.assertNotNull(routingAppender.getDefaultRouteScript());
        Assert.assertNotNull(routingAppender.getDefaultRoute());
        assertEquals("Service2", routingAppender.getDefaultRoute().getKey());
    }

    @Test
    public void testRoutingAppenderPresence() {
        getRoutingAppender();
    }

    @Test
    public void testRoutingPresence1() {
        logAndCheck();
        checkStaticVars();
    }

    @Test
    public void testRoutingPresence2() {
        logAndCheck();
        checkStaticVars();
    }
}
