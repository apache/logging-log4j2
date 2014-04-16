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

import org.apache.camel.Component;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.ProcessorEndpoint;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.util.ServiceHelper;

/**
 * Camel Endpoint for logging to Log4j.
 */
@UriEndpoint(scheme = "log4j")
public class LogEndpoint extends ProcessorEndpoint {

    private volatile Processor logger;

    @UriParam
    private String level;

    @UriParam
    private String marker;

    public LogEndpoint() {
    }

    public LogEndpoint(final String endpointURI, final Component component) {
        super(endpointURI, component);
    }

    @Override
    protected void doStart() throws Exception {
        ServiceHelper.startService(logger);
    }

    @Override
    protected void doStop() throws Exception {
        ServiceHelper.stopService(logger);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new LogProducer(this, this.logger);
    }

    @Override
    protected String createEndpointUri() {
        return "log4j:" + logger.toString();
    }

    public Processor getLogger() {
        return logger;
    }

    public void setLogger(final Processor logger) {
        this.logger = logger;
        setProcessor(this.logger);
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(final String level) {
        this.level = level;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(final String marker) {
        this.marker = marker;
    }
}
