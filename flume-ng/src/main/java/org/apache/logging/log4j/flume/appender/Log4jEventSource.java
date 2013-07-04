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
package org.apache.logging.log4j.flume.appender;

import org.apache.flume.ChannelException;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.instrumentation.SourceCounter;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class Log4jEventSource extends AbstractSource implements EventDrivenSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(Log4jEventSource.class);

    private final SourceCounter sourceCounter = new SourceCounter("log4j");

    public Log4jEventSource() {
        setName("Log4jEvent");
    }

    @Override
    public synchronized void start() {
        super.start();

        LOGGER.info("Log4j Source started");
    }

    @Override
    public synchronized void stop() {
        super.stop();

        LOGGER.info("Log4j Source stopped. Metrics {}", sourceCounter);
    }


    public void send(final Event event) {
        sourceCounter.incrementAppendReceivedCount();
        sourceCounter.incrementEventReceivedCount();
        try {
            getChannelProcessor().processEvent(event);
        } catch (final ChannelException ex) {
            LOGGER.warn("Unabled to process event {}" + event, ex);
            throw ex;
        }
        sourceCounter.incrementAppendAcceptedCount();
        sourceCounter.incrementEventAcceptedCount();
    }
}
