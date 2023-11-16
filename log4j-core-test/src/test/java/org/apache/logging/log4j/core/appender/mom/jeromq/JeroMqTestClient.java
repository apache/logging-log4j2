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
package org.apache.logging.log4j.core.appender.mom.jeromq;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

class JeroMqTestClient implements Callable<List<String>> {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final ZMQ.Context context;

    private final String endpoint;
    private final List<String> messages;
    private final int receiveCount;

    JeroMqTestClient(final ZMQ.Context context, final String endpoint, final int receiveCount) {
        this.context = context;
        this.endpoint = endpoint;
        this.receiveCount = receiveCount;
        this.messages = new ArrayList<>(receiveCount);
    }

    @Override
    public List<String> call() throws Exception {
        try (final ZMQ.Socket subscriber = context.socket(SocketType.SUB)) {
            LOGGER.info("Starting JeroMqTestClient.");
            subscriber.connect(endpoint);
            subscriber.subscribe(ZMQ.SUBSCRIPTION_ALL);
            LOGGER.info("Subscribing JeroMqTestClient to JeroMqAppender.");
            for (int messageNum = 0;
                    messageNum < receiveCount && !Thread.currentThread().isInterrupted();
                    messageNum++) {
                // Use trim to remove the tailing '0' character
                final String message = subscriber.recvStr(0).trim();
                LOGGER.debug("JeroMqTestClient received a message: {}.", message);
                messages.add(message);
            }
        }
        return messages;
    }
}
