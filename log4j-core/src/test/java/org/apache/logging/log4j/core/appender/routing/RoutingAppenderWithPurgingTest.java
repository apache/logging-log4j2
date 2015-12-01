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

import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Testing Routing appender purge facilities
 */
public class RoutingAppenderWithPurgingTest {
    private static final String CONFIG = "log4j-routing-purge.xml";
    private static final String IDLE_LOG_FILE1 = "target/routing-purge-idle/routingtest-1.log";
    private static final String IDLE_LOG_FILE2 = "target/routing-purge-idle/routingtest-2.log";
    private static final String IDLE_LOG_FILE3 = "target/routing-purge-idle/routingtest-3.log";
    private static final String MANUAL_LOG_FILE1 = "target/routing-purge-manual/routingtest-1.log";
    private static final String MANUAL_LOG_FILE2 = "target/routing-purge-manual/routingtest-2.log";
    private static final String MANUAL_LOG_FILE3 = "target/routing-purge-manual/routingtest-3.log";
   

    private ListAppender app;
    private RoutingAppender routingAppenderIdle;
    private RoutingAppender routingAppenderIdleWithHangingAppender;
    private RoutingAppender routingAppenderManual;

    @Rule
    public LoggerContextRule init = new LoggerContextRule(CONFIG);

    @Rule
    public CleanFiles files = new CleanFiles(IDLE_LOG_FILE1, IDLE_LOG_FILE2, IDLE_LOG_FILE3, 
    		MANUAL_LOG_FILE1, MANUAL_LOG_FILE2, MANUAL_LOG_FILE3);
	

    @Before
    public void setUp() throws Exception {
        this.app = this.init.getListAppender("List");
        this.routingAppenderIdle = this.init.getRequiredAppender("RoutingPurgeIdle", RoutingAppender.class);
        this.routingAppenderIdleWithHangingAppender =
                this.init.getRequiredAppender("RoutingPurgeIdleWithHangingAppender", RoutingAppender.class);
        this.routingAppenderManual = this.init.getRequiredAppender("RoutingPurgeManual", RoutingAppender.class);
    }

    @After
    public void tearDown() throws Exception {
        this.app.clear();
    }

    @Test(timeout = 5000)
    public void routingTest() throws InterruptedException {
        StructuredDataMessage msg = new StructuredDataMessage("1", "This is a test 1", "Service");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = app.getEvents();
        assertNotNull("No events generated", list);
        assertTrue("Incorrect number of events. Expected 1, got " + list.size(), list.size() == 1);
        msg = new StructuredDataMessage("2", "This is a test 2", "Service");
        EventLogger.logEvent(msg);
        msg = new StructuredDataMessage("3", "This is a test 3", "Service");
        EventLogger.logEvent(msg);
        String[] files = {IDLE_LOG_FILE1, IDLE_LOG_FILE2, IDLE_LOG_FILE3, MANUAL_LOG_FILE1, MANUAL_LOG_FILE2, MANUAL_LOG_FILE3};
        assertFileExistance(files);
        
        assertEquals("Incorrect number of appenders with IdlePurgePolicy.", 3, routingAppenderIdle.getAppenders().size());
        assertEquals("Incorrect number of appenders with IdlePurgePolicy with HangingAppender.",
                3, routingAppenderIdleWithHangingAppender.getAppenders().size());
        assertEquals("Incorrect number of appenders manual purge.", 3, routingAppenderManual.getAppenders().size());

        Thread.sleep(3000);
        EventLogger.logEvent(msg);

        assertEquals("Incorrect number of appenders with IdlePurgePolicy.", 1, routingAppenderIdle.getAppenders().size());
        assertEquals("Incorrect number of appenders with manual purge.", 3, routingAppenderManual.getAppenders().size());
        
        routingAppenderManual.deleteAppender("1");
        routingAppenderManual.deleteAppender("2");
        routingAppenderManual.deleteAppender("3");

        assertEquals("Incorrect number of appenders with IdlePurgePolicy.", 1, routingAppenderIdle.getAppenders().size());
        assertEquals("Incorrect number of appenders with manual purge.", 0, routingAppenderManual.getAppenders().size());
    }
    
    private void assertFileExistance(String... files) {
    	for (String file : files) {
			assertTrue("File should exist - " + file + " file ", new File(file).exists());
		}
    }
}
