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
package org.apache.log4j.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.net.mock.MockSyslogServer;
import org.apache.logging.log4j.core.test.net.mock.MockSyslogServerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Class Description goes here.
 */
public class SyslogAppenderTest {

    // TODO Use an ephemeral port, save it in a sys prop, and update test config files.
    private static final int PORTNUM = 9999;
    private MockSyslogServer syslogServer;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.LOG4J1_CONFIGURATION_FILE_PROPERTY, "target/test-classes/log4j1-syslog.xml");
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(ConfigurationFactory.LOG4J1_CONFIGURATION_FILE_PROPERTY);
    }

    @After
    public void teardown() {
        if (syslogServer != null) {
            syslogServer.shutdown();
        }
    }

    @Test
    public void sendMessage() throws Exception {
        initTCPTestEnvironment(null);
        Logger logger = LogManager.getLogger(SyslogAppenderTest.class);
        logger.info("This is a test");
        List<String> messages = null;
        for (int i = 0; i < 5; ++i) {
            Thread.sleep(250);
            messages = syslogServer.getMessageList();
            if (messages != null && messages.size() > 0) {
                break;
            }
        }
        assertNotNull("No messages received", messages);
        assertEquals("Sent message not detected", 1, messages.size());
    }


    protected void initTCPTestEnvironment(final String messageFormat) throws IOException {
        syslogServer = MockSyslogServerFactory.createTCPSyslogServer(1, PORTNUM);
        syslogServer.start();
    }
}
