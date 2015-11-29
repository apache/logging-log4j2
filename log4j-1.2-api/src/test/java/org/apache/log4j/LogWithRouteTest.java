/*
 * Copyright (c) 2015 Nextiva, Inc. to Present.
 * All rights reserved.
 */
package org.apache.log4j;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Class Description goes here.
 * Created by rgoers on 11/29/15
 */
public class LogWithRouteTest {

    private static final String CONFIG = "log-RouteWithMDC.xml";

    @ClassRule
    public static final LoggerContextRule CTX = new LoggerContextRule(CONFIG);

    @Test
    public void testMDC() throws Exception {
        MDC.put("Type", "Service");
        MDC.put("Name", "John Smith");
        Logger logger = Logger.getLogger("org.apache.test.logging");
        logger.debug("This is a test");
        ListAppender listApp = (ListAppender) CTX.getAppender("List");
        assertNotNull(listApp);
        List<String> msgs = listApp.getMessages();
        assertNotNull("No messages received", msgs);
        assertTrue(msgs.size() == 1);
        assertTrue("Type is missing", msgs.get(0).contains("Type=Service"));
        assertTrue("Name is missing", msgs.get(0).contains("Name=John Smith"));
    }
}
