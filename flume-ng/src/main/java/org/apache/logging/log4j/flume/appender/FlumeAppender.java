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
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.RFC5424Layout;

/**
 * An Appender that uses the Avro protocol to route events to Flume.
 */
@Plugin(name = "Flume", type = "Core", elementType = "appender", printObject = true)
public final class FlumeAppender extends AbstractAppender implements FlumeEventFactory {

    private final AbstractFlumeManager manager;

    private final String mdcIncludes;
    private final String mdcExcludes;
    private final String mdcRequired;

    private final String eventPrefix;

    private final String mdcPrefix;

    private final boolean compressBody;

    private final int reconnectDelay;

    private final int retries;

    private final FlumeEventFactory factory;

    private FlumeAppender(final String name, final Filter filter, final Layout layout, final boolean handleException,
                          final String includes, final String excludes, final String required, final String mdcPrefix,
                          final String eventPrefix, final boolean compress, final int delay, final int retries,
                          final FlumeEventFactory factory, final AbstractFlumeManager manager) {
        super(name, filter, layout, handleException);
        this.manager = manager;
        this.mdcIncludes = includes;
        this.mdcExcludes = excludes;
        this.mdcRequired = required;
        this.eventPrefix = eventPrefix;
        this.mdcPrefix = mdcPrefix;
        this.compressBody = compress;
        this.reconnectDelay = delay;
        this.retries = retries;
        this.factory = factory == null ? this : factory;
    }

    /**
     * Publish the event.
     * @param event The LogEvent.
     */
    public void append(final LogEvent event) {

        final FlumeEvent flumeEvent = factory.createEvent(event, mdcIncludes, mdcExcludes, mdcRequired, mdcPrefix,
            eventPrefix, compressBody);
        flumeEvent.setBody(getLayout().toByteArray(flumeEvent));
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
    public FlumeEvent createEvent(final LogEvent event, final String includes, final String excludes,
                                  final String required, final String mdcPrefix, final String eventPrefix,
                                  final boolean compress) {
        return new FlumeEvent(event, mdcIncludes, mdcExcludes, mdcRequired, mdcPrefix,
            eventPrefix, compressBody);
    }

    /**
     * Create a Flume Avro Appender.
     * @param agents An array of Agents.
     * @param properties Properties to pass to the embedded agent.
     * @param embedded true if the embedded agent manager should be used. otherwise the Avro mangaer will be used.
     * @param dataDir The directory where the Flume FileChannel should write its data.
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
    public static FlumeAppender createAppender(@PluginElement("agents") Agent[] agents,
                                                   @PluginElement("properties") final Property[] properties,
                                                   @PluginAttr("embedded") final String embedded,
                                                   @PluginAttr("dataDir") final String dataDir,
                                                   @PluginAttr("reconnectionDelay") final String delay,
                                                   @PluginAttr("agentRetries") final String agentRetries,
                                                   @PluginAttr("name") final String name,
                                                   @PluginAttr("suppressExceptions") final String suppress,
                                                   @PluginAttr("mdcExcludes") final String excludes,
                                                   @PluginAttr("mdcIncludes") final String includes,
                                                   @PluginAttr("mdcRequired") final String required,
                                                   @PluginAttr("mdcPrefix") final String mdcPrefix,
                                                   @PluginAttr("eventPrefix") final String eventPrefix,
                                                   @PluginAttr("compress") final String compressBody,
                                                   @PluginAttr("batchSize") final String batchSize,
                                                   @PluginElement("flumeEventFactory") final FlumeEventFactory factory,
                                                   @PluginElement("layout") Layout layout,
                                                   @PluginElement("filters") final Filter filter) {

        final boolean embed = embedded != null ? Boolean.valueOf(embedded) :
            (agents == null || agents.length == 0) && properties != null && properties.length > 0;
        final boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        final boolean compress = compressBody == null ? true : Boolean.valueOf(compressBody);

        final int batchCount = batchSize == null ? 1 : Integer.parseInt(batchSize);
        final int reconnectDelay = delay == null ? 0 : Integer.parseInt(delay);
        final int retries = agentRetries == null ? 0 : Integer.parseInt(agentRetries);

        if (layout == null) {
            layout = RFC5424Layout.createLayout(null, null, null, "True", null, null, null, null, excludes,
                includes, required, null, null, null, null);
        }

        if (name == null) {
            LOGGER.error("No name provided for Appender");
            return null;
        }

        AbstractFlumeManager manager;

        if (embed) {
            manager = FlumeEmbeddedManager.getManager(name, agents, properties, batchCount, dataDir);
        } else {
            if (agents == null || agents.length == 0) {
                LOGGER.debug("No agents provided, using defaults");
                agents = new Agent[] {Agent.createAgent(null, null)};
            }
            manager = FlumeAvroManager.getManager(name, agents, batchCount);
        }

        if (manager == null) {
            return null;
        }

        return new FlumeAppender(name, filter, layout,  handleExceptions, includes,
            excludes, required, mdcPrefix, eventPrefix, compress, reconnectDelay, retries, factory, manager);
    }
}
