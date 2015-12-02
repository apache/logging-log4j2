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

import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.net.Protocol;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class UdpXmlSocketServerTest extends AbstractSocketServerTest {

    private static UdpSocketServer<InputStream> server;

    @BeforeClass
    public static void setupClass() throws Exception {
        (LoggerContext.getContext(false)).reconfigure();
        // Use a tiny buffer just to test the code, the TCP test uses a large buffer
        server = new UdpSocketServer<>(PORT_NUM, new XmlInputStreamLogEventBridge(100,
                Charset.defaultCharset()));
        thread = server.startNewThread();
    }

    @AfterClass
    public static void tearDownClass() {
        server.shutdown();
        try {
            thread.join();
        } catch (final InterruptedException e) {
            // ignore
        }
    }

    public UdpXmlSocketServerTest() {
        super(Protocol.UDP, PORT, true);
    }

    @Override
    protected Layout<? extends Serializable> createLayout() {
        return super.createXmlLayout();
    }

}
