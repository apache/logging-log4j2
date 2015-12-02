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
package org.apache.logging.log4j.core.net.server;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.Protocol;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class TcpJsonSocketServerTest extends AbstractSocketServerTest {
    
    private static TcpSocketServer<InputStream> server;

    @BeforeClass
    public static void setupClass() throws Exception {
        (LoggerContext.getContext(false)).reconfigure();
        server = TcpSocketServer.createJsonSocketServer(PORT_NUM);
        thread = server.startNewThread();
    }

    @AfterClass
    public static void tearDownClass() {
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

    public TcpJsonSocketServerTest() {
        super(Protocol.TCP, PORT, false);
    }

    @Override
    protected Layout<String> createLayout() {
        return super.createJsonLayout();
    }

}
