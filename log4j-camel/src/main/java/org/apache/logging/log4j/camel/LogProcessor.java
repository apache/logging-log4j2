/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.logging.log4j.camel;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.processor.DefaultExchangeFormatter;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.util.AsyncProcessorHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;

/**
 * Camel Processor for Log4j.
 */
public class LogProcessor implements AsyncProcessor {

    private final ExchangeFormatter formatter;

    private final LoggerWrapper logger;

    public LogProcessor() {
        this(new LoggerWrapper());
    }

    public LogProcessor(final LoggerWrapper logger) {
        this(logger, new DefaultExchangeFormatter());
    }

    public LogProcessor(final LoggerWrapper logger, final ExchangeFormatter formatter) {
        ObjectHelper.notNull(logger, "logger");
        ObjectHelper.notNull(formatter, "formatter");
        this.logger = logger;
        this.formatter = formatter;
    }

    @Override
    public boolean process(final Exchange exchange, final AsyncCallback asyncCallback) {
        if (logger.shouldLog()) {
            logger.log(formatter.format(exchange));
        }
        asyncCallback.done(true);
        return true;
    }

    public void process(Exchange exchange, Throwable exception) {
        if (logger.shouldLog()) {
            logger.log(formatter.format(exchange), exception);
        }
    }

    public void process(Exchange exchange, String message) {
        if (logger.shouldLog()) {
            logger.log(formatter.format(exchange) + message);
        }
    }

    @Override
    public void process(final Exchange exchange) throws Exception {
        AsyncProcessorHelper.process(this, exchange);
    }

    public LoggerWrapper getLogger() {
        return logger;
    }

    public void setLoggerName(final String name) {
        logger.setLoggerName(name);
    }

    public void setMarker(final Marker marker) {
        logger.setMarker(marker);
    }

    public void setLevel(final Level level) {
        logger.setLevel(level);
    }

}
