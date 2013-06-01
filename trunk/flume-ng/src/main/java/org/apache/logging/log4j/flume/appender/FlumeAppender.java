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

import java.io.Serializable;
import java.util.Locale;

/**
 * An Appender that uses the Avro protocol to route events to Flume.
 */
@Plugin(name = "Flume", category = "Core", elementType = "appender", printObject = true)
public final class FlumeAppender<T extends Serializable> extends AbstractAppender<T> implements FlumeEventFactory {

    private final AbstractFlumeManager manager;

    private final String mdcIncludes;
    private final String mdcExcludes;
    private final String mdcRequired;

    private final String eventPrefix;

    private final String mdcPrefix;

    private final boolean compressBody;

    private final FlumeEventFactory factory;

    private enum ManagerType {
        AVRO, EMBEDDED, PERSISTENT;

        public static ManagerType getType(String type) {
            return valueOf(type.toUpperCase(Locale.US));
        }
    }

    private FlumeAppender(final String name, final Filter filter, final Layout<T> layout, final boolean handleException,
                          final String includes, final String excludes, final String required, final String mdcPrefix,
                          final String eventPrefix, final boolean compress,
                          final FlumeEventFactory factory, final AbstractFlumeManager manager) {
        super(name, filter, layout, handleException);
        this.manager = manager;
        this.mdcIncludes = includes;
        this.mdcExcludes = excludes;
        this.mdcRequired = required;
        this.eventPrefix = eventPrefix;
        this.mdcPrefix = mdcPrefix;
        this.compressBody = compress;
        this.factory = factory == null ? this : factory;
    }

    /**
     * Publish the event.
     * @param event The LogEvent.
     */
    @Override
    public void append(final LogEvent event) {

        final FlumeEvent flumeEvent = factory.createEvent(event, mdcIncludes, mdcExcludes, mdcRequired, mdcPrefix,
            eventPrefix, compressBody);
        flumeEvent.setBody(getLayout().toByteArray(flumeEvent));
        manager.send(flumeEvent);
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
    @Override
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
     * @param embedded true if the embedded agent manager should be used. otherwise the Avro manager will be used.
     * <b>Note: </b><i>The embedded attribute is deprecated in favor of specifying the type attribute.</i>
     * @param type Avro (default), Embedded, or Persistent.
     * @param dataDir The directory where the Flume FileChannel should write its data.
     * @param connectionTimeout The amount of time in milliseconds to wait before a connection times out. Minimum is
     *                          1000.
     * @param requestTimeout The amount of time in milliseconds to wait before a request times out. Minimum is 1000.
     * @param agentRetries The number of times to retry an agent before failing to the next agent.
     * @param maxDelay The maximum number of seconds to wait for a complete batch.
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
    public static <S extends Serializable> FlumeAppender<S> createAppender(@PluginElement("agents") Agent[] agents,
                                                   @PluginElement("properties") final Property[] properties,
                                                   @PluginAttr("embedded") final String embedded,
                                                   @PluginAttr("type") final String type,
                                                   @PluginAttr("dataDir") final String dataDir,
                                                   @PluginAttr("connectTimeout") final String connectionTimeout,
                                                   @PluginAttr("requestTimeout") final String requestTimeout,
                                                   @PluginAttr("agentRetries") final String agentRetries,
                                                   @PluginAttr("maxDelay") final String maxDelay,
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
                                                   @PluginElement("layout") Layout<S> layout,
                                                   @PluginElement("filters") final Filter filter) {

        final boolean embed = embedded != null ? Boolean.valueOf(embedded) :
            (agents == null || agents.length == 0) && properties != null && properties.length > 0;
        final boolean handleExceptions = suppress == null ? true : Boolean.valueOf(suppress);
        final boolean compress = compressBody == null ? true : Boolean.valueOf(compressBody);
        ManagerType managerType;
        if (type != null) {
            if (embed && embedded != null) {
                try {
                    managerType = ManagerType.getType(type);
                    LOGGER.warn("Embedded and type attributes are mutually exclusive. Using type " + type);
                } catch (Exception ex) {
                    LOGGER.warn("Embedded and type attributes are mutually exclusive and type " + type + " is invalid.");
                    managerType = ManagerType.EMBEDDED;
                }
            } else {
                try {
                    managerType = ManagerType.getType(type);
                } catch (Exception ex) {
                    LOGGER.warn("Type " + type + " is invalid.");
                    managerType = ManagerType.EMBEDDED;
                }
            }
        }  else if (embed) {
           managerType = ManagerType.EMBEDDED;
        }  else {
           managerType = ManagerType.AVRO;
        }

        final int batchCount = batchSize == null ? 1 : Integer.parseInt(batchSize);
        final int connectTimeout = connectionTimeout == null ? 0 : Integer.parseInt(connectionTimeout);
        final int reqTimeout = requestTimeout == null ? 0 : Integer.parseInt(requestTimeout);
        final int retries = agentRetries == null ? 0 : Integer.parseInt(agentRetries);
        final int delay = maxDelay == null ? 60000 : Integer.parseInt(maxDelay);

        if (layout == null) {
            @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
            Layout<S> l = (Layout<S>)RFC5424Layout.createLayout(null, null, null, "True", null, mdcPrefix, eventPrefix,
                    null, null, null, excludes, includes, required, null, null, null);
            layout = l;
        }

        if (name == null) {
            LOGGER.error("No name provided for Appender");
            return null;
        }

        AbstractFlumeManager manager;

        switch (managerType) {
            case EMBEDDED:
                manager = FlumeEmbeddedManager.getManager(name, agents, properties, batchCount, dataDir);
                break;
            case AVRO:
                if (agents == null || agents.length == 0) {
                    LOGGER.debug("No agents provided, using defaults");
                    agents = new Agent[] {Agent.createAgent(null, null)};
                }
                manager = FlumeAvroManager.getManager(name, agents, batchCount, retries, connectTimeout, reqTimeout);
                break;
            case PERSISTENT:
                if (agents == null || agents.length == 0) {
                    LOGGER.debug("No agents provided, using defaults");
                    agents = new Agent[] {Agent.createAgent(null, null)};
                }
                manager = FlumePersistentManager.getManager(name, agents, properties, batchCount, retries,
                    connectTimeout, reqTimeout, delay, dataDir);
                break;
            default:
                LOGGER.debug("No manager type specified. Defaulting to AVRO");
                if (agents == null || agents.length == 0) {
                    LOGGER.debug("No agents provided, using defaults");
                    agents = new Agent[] {Agent.createAgent(null, null)};
                }
                manager = FlumeAvroManager.getManager(name, agents, batchCount, retries, connectTimeout, reqTimeout);
        }

        if (manager == null) {
            return null;
        }

        return new FlumeAppender<S>(name, filter, layout,  handleExceptions, includes,
            excludes, required, mdcPrefix, eventPrefix, compress, factory, manager);
    }
}
