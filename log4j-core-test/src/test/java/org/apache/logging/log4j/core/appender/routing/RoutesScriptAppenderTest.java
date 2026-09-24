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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Scripts.Groovy") // technically only half of these tests require groovy
public abstract class RoutesScriptAppenderTest {

    @BeforeAll
    public static void beforeClass() {
        System.setProperty(Constants.SCRIPT_LANGUAGES, "Groovy, JavaScript");
    }

    private LoggerContext loggerContext;
    private final boolean expectBindingEntries;

    public RoutesScriptAppenderTest(LoggerContext loggerContext, final boolean expectBindingEntries) {
        this.loggerContext = loggerContext;
        this.expectBindingEntries = expectBindingEntries;
    }

    private void checkStaticVars() {
        final RoutingAppender routingAppender = getRoutingAppender();
        final ConcurrentMap<Object, Object> map = routingAppender.getScriptStaticVariables();
        if (expectBindingEntries) {
            assertEquals(map.get("TestKey"), "TestValue2");
            assertEquals(map.get("MarkerName"), "HEXDUMP");
        }
    }

    private ListAppender getListAppender() {
        final String key = "Service2";
        final RoutingAppender routingAppender = getRoutingAppender();
        assertTrue(routingAppender.isStarted());
        final Map<String, AppenderControl> appenders = routingAppender.getAppenders();
        final AppenderControl appenderControl = appenders.get(key);
        assertNotNull(appenderControl, "No appender control generated for '" + key + "'; appenders = " + appenders);
        return (ListAppender) appenderControl.getAppender();
    }

    private RoutingAppender getRoutingAppender() {
        return (RoutingAppender) loggerContext.getConfiguration().getAppender("Routing");
    }

    private void logAndCheck() {
        final Marker marker = MarkerManager.getMarker("HEXDUMP");
        final Logger logger = (Logger) loggerContext.getLogger(RoutesScriptAppenderTest.class);
        logger.error("Hello");
        final ListAppender listAppender = getListAppender();
        assertEquals(1, listAppender.getEvents().size(), "Incorrect number of events");
        logger.error("World");
        assertEquals(2, listAppender.getEvents().size(), "Incorrect number of events");
        logger.error(marker, "DEADBEEF");
        assertEquals(3, listAppender.getEvents().size(), "Incorrect number of events");
    }

    @Test
    public void testNotListAppender() {
        Appender appender = loggerContext.getConfiguration().getAppender("List1");
        assertFalse(appender instanceof ListAppender);
    }

    @Test
    public void testListAppenderPresence() {
        // No appender until an event is routed, even thought we initialized the default route on startup.
        assertNull(getRoutingAppender().getAppenders().get("Service2"), "No appender control generated");
    }

    @Test
    public void testNoPurgePolicy() {
        // No PurgePolicy in this test
        assertNull(getRoutingAppender().getPurgePolicy(), "Unexpected PurgePolicy");
    }

    @Test
    public void testNoRewritePolicy() {
        // No RewritePolicy in this test
        assertNull(getRoutingAppender().getRewritePolicy(), "Unexpected RewritePolicy");
    }

    @Test
    public void testRoutingAppenderRoutes() {
        final RoutingAppender routingAppender = getRoutingAppender();
        assertEquals(expectBindingEntries, routingAppender.getDefaultRouteScript() != null);
        assertEquals(expectBindingEntries, routingAppender.getDefaultRoute() != null);
        final Routes routes = routingAppender.getRoutes();
        assertNotNull(routes);
        assertNotNull(routes.getPatternScript());
        final LogEvent logEvent =
                DefaultLogEventFactory.getInstance().createEvent("", null, "", Level.ERROR, null, null, null);
        assertEquals("Service2", routes.getPattern(logEvent, new ConcurrentHashMap<>()));
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
