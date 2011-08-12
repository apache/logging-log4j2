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
package org.apache.logging.log4j.core.appender.flume;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderBase;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.Filters;
import org.apache.logging.log4j.core.layout.RFC5424Layout;

import java.net.InetAddress;

/**
 *
 */
@Plugin(name="Flume",type="Core",elementType="appender",printObject=true)
public class FlumeAvroAppender extends AppenderBase {

    private FlumeAvroManager manager;

    private final String mdcIncludes;
    private final String mdcExcludes;
    private final String mdcRequired;

    private final String eventPrefix;

    private final String mdcPrefix;

    private final boolean compressBody;

    private final String hostname;

    private final int reconnectDelay;

    private final int retries;

    private FlumeAvroAppender(String name, Filters filters, Layout layout, boolean handleException,
                              String hostname, String includes, String excludes, String required, String mdcPrefix,
                              String eventPrefix, boolean compress, int delay, int retries, FlumeAvroManager manager) {
        super(name, filters, layout, handleException);
        this.manager = manager;
        this.mdcIncludes = includes;
        this.mdcExcludes = excludes;
        this.mdcRequired = required;
        this.eventPrefix = eventPrefix;
        this.mdcPrefix = mdcPrefix;
        this.compressBody = compress;
        this.hostname = hostname;
        this.reconnectDelay = delay;
        this.retries = retries;
    }

    public void append(LogEvent event) {

        FlumeEvent flumeEvent = new FlumeEvent(event, hostname, mdcIncludes, mdcExcludes, mdcRequired, mdcPrefix,
            eventPrefix, compressBody);
        flumeEvent.setBody(getLayout().format(flumeEvent));
        manager.send(flumeEvent, reconnectDelay, retries);
    }

    @Override
    public void stop() {
        super.stop();
        manager.release();
    }

    @PluginFactory
    public static FlumeAvroAppender createAppender(@PluginAttr("agents") Agent[] agents,
                                                   @PluginAttr("reconnectionDelay") String delay,
                                                   @PluginAttr("agentRetries") String agentRetries,
                                                   @PluginAttr("name") String name,
                                                   @PluginAttr("suppressExceptions") String suppress,
                                                   @PluginAttr("mdcExcludes") String excludes,
                                                   @PluginAttr("mdcIncludes") String includes,
                                                   @PluginAttr("mdcRequired") String required,
                                                   @PluginAttr("mdcPrefix") String mdcPrefix,
                                                   @PluginAttr("eventPrefix") String eventPrefix,
                                                   @PluginAttr("compress") String compressBody,
                                                   @PluginElement("layout") Layout layout,
                                                   @PluginElement("filters") Filters filters) {

        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            logger.error("Unable to determine local hostname", ex);
            return null;
        }
        if (agents == null || agents.length == 0) {
            logger.debug("No agents provided, using defaults");
            agents = new Agent[] {Agent.createAgent(null, null)};
        }

        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        boolean compress = compressBody == null ? true : Boolean.valueOf(compressBody);

        int reconnectDelay = delay == null ? 0 : Integer.parseInt(delay);
        int retries = agentRetries == null ? 0 : Integer.parseInt(agentRetries);

        if (layout == null) {
            layout = RFC5424Layout.createLayout(null, null, null, "True", null, null, null, null, excludes, includes,
                required, null);
        }

        if (name == null) {
            logger.error("No name provided for Appender");
            return null;
        }

        FlumeAvroManager manager = FlumeAvroManager.getManager(agents);
        if (manager == null) {
            return null;
        }
        return new FlumeAvroAppender(name, filters, layout,  handleExceptions, hostname, includes,
            excludes, required, mdcPrefix, eventPrefix, compress, reconnectDelay, retries, manager);
    }
}
