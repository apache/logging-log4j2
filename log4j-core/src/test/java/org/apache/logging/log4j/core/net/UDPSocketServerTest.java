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
package org.apache.logging.log4j.core.net;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class UDPSocketServerTest extends AbstractSocketServerTest {
    private static final String PORT = "8199";
    private static final int PORT_NUM = Integer.parseInt(PORT);
    private static UDPSocketServer udpSocketServer;

    private static Thread thread;

    @BeforeClass
    public static void setupClass() throws Exception {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        udpSocketServer = new UDPSocketServer(PORT_NUM);
        thread = new Thread(udpSocketServer);
        thread.start();
    }

    @AfterClass
    public static void tearDownClass() {
        udpSocketServer.shutdown();
        try {
            thread.join();
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    public UDPSocketServerTest() {
        super("udp", PORT, true);
    }

}
