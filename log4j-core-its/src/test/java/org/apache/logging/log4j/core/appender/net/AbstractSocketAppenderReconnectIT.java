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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.SocketAppender;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.message.StringMapMessage;
import org.apache.logging.log4j.server.TcpSocketServer;
import org.junit.After;
import org.junit.Assert;

/**
 * Subclass for tests that reconnect to an Apache Socket Server. The class makes sure resources are properly shutdown
 * after each @Test method. A subclass normally only has one @Test method.
 * <p>
 * LOG4J2-1311 SocketAppender will lost first several logs after re-connection to log servers.
 * </p>
 */
public class AbstractSocketAppenderReconnectIT {

    protected SocketAppender appender;
    protected int port;
    protected TcpSocketServer<InputStream> server;
    protected Thread thread;

    @After
    public void after() {
        shutdown();
        if (appender != null) {
            appender.stop();
            // Make sure the manager is gone as to not have bad side effect on other tests.
            @SuppressWarnings("resource")
            final AbstractSocketManager appenderManager = appender.getManager();
            if (appenderManager != null) {
                Assert.assertFalse(AbstractManager.hasManager(appenderManager.getName()));
            }
        }
    }

    protected void appendEvent(final SocketAppender appender) {
        final Map<String, String> map = new HashMap<>();
        final String messageText = "Hello, World!";
        final String loggerName = this.getClass().getName();
        map.put("messageText", messageText);
        map.put("threadName", Thread.currentThread().getName());
        // @formatter:off
		final LogEvent event = Log4jLogEvent.newBuilder()
				.setLoggerName(loggerName)
				.setLoggerFqcn(loggerName)
				.setLevel(Level.INFO)
				.setMessage(new StringMapMessage(map))
				.setTimeMillis(System.currentTimeMillis())
				.build();
		// @formatter:on
        appender.append(event);
    }

    protected void shutdown() {
        try {
            server.shutdown();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        try {
            thread.join();
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    protected Thread startServer(long sleepMillis) throws InterruptedException {
        thread = server.startNewThread();
        if (sleepMillis >= 0) {
            Thread.sleep(sleepMillis);
        }
        return thread;
    }

}
