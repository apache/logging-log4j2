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
package org.apache.logging.log4j.core.appender.net;

import java.io.IOException;
import java.net.Socket;

import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.apache.logging.log4j.server.TcpSocketServer;
import org.apache.logging.log4j.test.AvailablePortFinder;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that a Socket Appender can reconnect to a server after it has been recycled.
 * <p>
 * LOG4J2-1311 SocketAppender will lost first several logs after re-connection to log servers.
 * </p>
 * <p>
 * See also LOG4J2-1934 JMS Appender does not know how to recover from a broken connection. See
 * https://issues.apache.org/jira/browse/LOG4J2-1934
 * </p>
 * <p>
 * This test class' single test method performs the following:
 * </p>
 * <ol>
 * <li>Starts Apache Socket Server</li>
 * <li>Starts a Socket Appender</li>
 * <li>Logs an event OK</li>
 * <li>Stops Apache Socket Server</li>
 * <li>Starts Apache Socket Server</li>
 * <li>Logs an event</li>
 * </ol>
 */
public class SocketAppenderConnectReConnectIT extends AbstractSocketAppenderReconnectIT {

    @Test
    public void testConnectReConnect() throws Exception {
        port = AvailablePortFinder.getNextAvailable();
        // Start server
        server = TcpSocketServer.createJsonSocketServer(port);
        startServer(200);
        // Start appender
        // @formatter:off
        appender = SocketAppender.newBuilder()
                .withPort(port)
                .withReconnectDelayMillis(1000)
                .withName("test")
                .withLayout(JsonLayout.newBuilder().build())
                .build();
        // @formatter:on
        appender.start();
        // Log message
        appendEvent(appender);
        // Stop server
        shutdown();
        // I should not be able to connect to the server now
        try {
            try (Socket socket = new Socket("localhost", port)) {
                Assert.fail("The server socket should not be opened: " + socket);
            }
        } catch (IOException e) {
            // expected
        }
        // HACK START - Gary
        // SANS HACK, the test passes, as somehow the socket in the appender is still valid
        // On Windows 10, I did not try other OSs:
        // HERE, I BREAKPOINT AND GO TO THE OS AND FORCE THE TCP CONNECTION TO CLOSE (TcpView.exe)), SUCH THAT
        // INTERNALLY THE MANAGER GETS:
        // java.net.SocketException: Connection reset by peer: socket write error
        // HACK END
        //
        // Restart server on the SAME port
        server = TcpSocketServer.createJsonSocketServer(port);
        thread = startServer(0);
        try (Socket socket = new Socket("localhost", port)) {
            Assert.assertTrue(socket.isBound());
            Assert.assertFalse(socket.isClosed());
        }
        // Logging again should cause the appender to reconnect
        appendEvent(appender);
    }

}
