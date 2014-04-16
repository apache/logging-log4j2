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
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultAsyncProducer;

/**
 * Log4j Camel producer. Does essentially the same thing as the Camel Core LogProducer.
 */
public class LogProducer extends DefaultAsyncProducer {

    private final Processor logger;

    public LogProducer(final Endpoint endpoint, final Processor logger) {
        super(endpoint);
        this.logger = logger;
    }

    @Override
    public boolean process(final Exchange exchange, final AsyncCallback asyncCallback) {
        try {
            logger.process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        } finally {
            asyncCallback.done(true);
        }
        return true;
    }

    public Processor getLogger() {
        return logger;
    }
}
