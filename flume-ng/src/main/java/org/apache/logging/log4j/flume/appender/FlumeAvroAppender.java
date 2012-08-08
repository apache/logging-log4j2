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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderBase;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.RFC5424Layout;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * An Appender that uses the Avro protocol to route events to Flume.
 */
@Plugin(name = "Flume", type = "Core", elementType = "appender", printObject = true)
public final class FlumeAvroAppender extends AppenderBase implements FlumeEventFactory {

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

    private final FlumeEventFactory factory;

    private FlumeAvroAppender(String name, Filter filter, Layout layout, boolean handleException,
                              String hostname, String includes, String excludes, String required, String mdcPrefix,
                              String eventPrefix, boolean compress, int delay, int retries,
                              FlumeEventFactory factory, FlumeAvroManager manager) {
        super(name, filter, layout, handleException);
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
        this.factory = factory == null ? this : factory;
    }

    /**
     * Publish the event.
     * @param event The LogEvent.
     */
    public void append(LogEvent event) {

        FlumeEvent flumeEvent = factory.createEvent(event, mdcIncludes, mdcExcludes, mdcRequired, mdcPrefix,
            eventPrefix, compressBody);
        flumeEvent.setBody(getLayout().format(flumeEvent));
        manager.send(flumeEvent, reconnectDelay, retries);
    }

    @Override
    public void stop() {
        super.stop();
        manager.release();
    }

    /**
     * Create a Flume event.
     * @param event The Log4j LogEvent.
     * @param includes comma separated list of mdc elements to include.
     * @param excludes comma separated list of mdc elements to exclude.
     * @param required comma separated list of mdc elements that must be present with a value.
     * @param mdcPrefix The prefix to add to MDC key names.
     * @param eventPrefix The prefix to add to event fields.
     * @param compress If true the body will be compressed.
     * @return A Flume Event.
     */
    public FlumeEvent createEvent(LogEvent event, String includes, String excludes, String required,
                      String mdcPrefix, String eventPrefix, boolean compress) {
        return new FlumeEvent(event, mdcIncludes, mdcExcludes, mdcRequired, mdcPrefix,
            eventPrefix, compressBody);
    }

    /**
     * Create a Flume Avro Appender.
     * @param agents An array of Agents.
     * @param delay The amount of time in milliseconds to wait between retries.
     * @param agentRetries The number of times to retry an agent before failing to the next agent.
     * @param name The name of the Appender.
     * @param suppress If true exceptions will be handled in the appender.
     * @param excludes A comma separated list of MDC elements to exclude.
     * @param includes A comma separated list of MDC elements to include.
     * @param required A comma separated list of MDC elements that are required.
     * @param mdcPrefix The prefix to add to MDC key names.
     * @param eventPrefix The prefix to add to event key names.
     * @param compressBody If true the event body will be compressed.
     * @param batchSize Number of events to include in a batch. Defaults to 1.
     * @param factory The factory to use to create Flume events.
     * @param layout The layout to format the event.
     * @param filter A Filter to filter events.
     * @return A Flume Avro Appender.
     */
    @PluginFactory
    public static FlumeAvroAppender createAppender(@PluginElement("agents") Agent[] agents,
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
                                                   @PluginAttr("batchSize") String batchSize,
                                                   @PluginElement("flumeEventFactory") FlumeEventFactory factory,
                                                   @PluginElement("layout") Layout layout,
                                                   @PluginElement("filters") Filter filter) {

        String hostname;
        try {
            hostname = getHostName();
        } catch (Exception ex) {
            LOGGER.error("Unable to determine local hostname", ex);
            return null;
        }
        if (agents == null || agents.length == 0) {
            LOGGER.debug("No agents provided, using defaults");
            agents = new Agent[] {Agent.createAgent(null, null)};
        }

        boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        boolean compress = compressBody == null ? true : Boolean.valueOf(compressBody);

        int batchCount = batchSize == null ? 1 : Integer.parseInt(batchSize);
        int reconnectDelay = delay == null ? 0 : Integer.parseInt(delay);
        int retries = agentRetries == null ? 0 : Integer.parseInt(agentRetries);

        if (layout == null) {
            layout = RFC5424Layout.createLayout(null, null, null, "True", null, null, null, null, excludes,
                includes, required, null, null);
        }

        if (name == null) {
            LOGGER.error("No name provided for Appender");
            return null;
        }

        FlumeAvroManager manager = FlumeAvroManager.getManager(agents, batchCount);
        if (manager == null) {
            return null;
        }

        return new FlumeAvroAppender(name, filter, layout,  handleExceptions, hostname, includes,
            excludes, required, mdcPrefix, eventPrefix, compress, reconnectDelay, retries, factory, manager);
    }

    private static String getHostName() throws Exception {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            // Could not locate host the easy way.
        }

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface nic = interfaces.nextElement();
            Enumeration<InetAddress> addresses = nic.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (!address.isLoopbackAddress()) {
                    String hostname = address.getHostName();
                    if (hostname != null) {
                        return hostname;
                    }
                }
            }
        }
        throw new UnknownHostException("Unable to determine host name");

    }
}
