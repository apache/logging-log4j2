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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.SocketAppenderTest.TcpSocketTestServer;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 */
public class SocketAppenderBufferSizeTest {

    private TcpSocketTestServer tcpServer;

    private LoggerContext loggerContext;
    private Logger logger;

    @Rule
    public LoggerContextRule loggerContextRule = new LoggerContextRule("log4j-empty.xml");

    @Before
    public void setup() throws Exception {
        tcpServer = new TcpSocketTestServer(AvailablePortFinder.getNextAvailable());
        tcpServer.start();
        ThreadContext.clearAll();
        loggerContext = loggerContextRule.getLoggerContext();
        logger = loggerContext.getLogger(SocketAppenderBufferSizeTest.class.getName());
    }

    @After
    public void teardown() {
        tcpServer.shutdown();
        loggerContext = null;
        logger = null;
        tcpServer.reset();
        ThreadContext.clearAll();
    }

    @Test
    public void testTcpAppenderDefaultEncoderBufferSize() throws Exception {
        SocketAppenderTest.testTcpAppender(tcpServer, logger, Constants.ENCODER_BYTE_BUFFER_SIZE);
    }

    @Test
    public void testTcpAppenderLargeEncoderBufferSize() throws Exception {
        SocketAppenderTest.testTcpAppender(tcpServer, logger, Constants.ENCODER_BYTE_BUFFER_SIZE * 100);
    }

    @Test
    public void testTcpAppenderSmallestEncoderBufferSize() throws Exception {
        SocketAppenderTest.testTcpAppender(tcpServer, logger, 1);
    }

}
