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
package org.apache.logging.log4j.core.test.net.mock;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public abstract class MockSyslogServer extends Thread {

    private static volatile int threadInitNumber;
    protected static Logger LOGGER = StatusLogger.getLogger();

    protected List<String> messageList = new ArrayList<>();

    @Deprecated
    protected int port;

    @Deprecated
    public MockSyslogServer(final int numberOfMessagesToReceive, final int port) {
        this();
        this.port = port;
    }

    public MockSyslogServer() {
        setName(getClass().getSimpleName() + "-" + (++threadInitNumber));
    }

    public abstract int getLocalPort();

    public void shutdown() {}

    public int getNumberOfReceivedMessages() {
        return messageList.size();
    }

    public List<String> getMessageList() {
        return messageList;
    }
}
