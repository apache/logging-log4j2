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
import org.apache.flume.EventDrivenSource;
import org.apache.flume.instrumentation.SourceCounter;
import org.apache.flume.source.AbstractSource;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 */
public class Log4jEventSource extends AbstractSource implements EventDrivenSource {

    private SourceCounter sourceCounter = new SourceCounter("log4j");

    private static final Logger logger = LoggerFactory.getLogger(Log4jEventSource.class);

    public Log4jEventSource() {
        setName("Log4jEvent");
    }

    @Override
    public synchronized void start() {
        super.start();

        logger.info("Log4j Source started");
    }

    @Override
    public synchronized void stop() {
        super.stop();

        logger.info("Log4j Source stopped. Metrics {}", sourceCounter);
    }


    public void send(FlumeEvent event) {
        sourceCounter.incrementAppendReceivedCount();
        sourceCounter.incrementEventReceivedCount();
        try {
            getChannelProcessor().processEvent(event);
        } catch (ChannelException ex) {
            logger.warn("Unabled to process event {}" + event, ex);
            throw ex;
        }
        sourceCounter.incrementAppendAcceptedCount();
        sourceCounter.incrementEventAcceptedCount();
    }
}
