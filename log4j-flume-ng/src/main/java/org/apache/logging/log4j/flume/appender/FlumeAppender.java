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

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.Rfc5424Layout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

/**
 * An Appender that uses the Avro protocol to route events to Flume.
 */
@Plugin(name = "Flume", category = "Core", elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class FlumeAppender extends AbstractAppender implements FlumeEventFactory {

    private static final String[] EXCLUDED_PACKAGES = {"org.apache.flume", "org.apache.avro"};
    private static final int DEFAULT_MAX_DELAY = 60000;

    private static final int DEFAULT_LOCK_TIMEOUT_RETRY_COUNT = 5;

    private final AbstractFlumeManager manager;

    private final String mdcIncludes;
    private final String mdcExcludes;
    private final String mdcRequired;

    private final String eventPrefix;

    private final String mdcPrefix;

    private final boolean compressBody;

    private final FlumeEventFactory factory;

    /**
     * Which Manager will be used by the appender instance.
     */
    private enum ManagerType {
        AVRO, EMBEDDED, PERSISTENT;

        public static ManagerType getType(final String type) {
            return valueOf(type.toUpperCase(Locale.US));
        }
    }

    private FlumeAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
                          final boolean ignoreExceptions, final String includes, final String excludes,
                          final String required, final String mdcPrefix, final String eventPrefix,
                          final boolean compress, final FlumeEventFactory factory, final AbstractFlumeManager manager) {
        super(name, filter, layout, ignoreExceptions);
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
        final String name = event.getLoggerName();
        if (name != null) {
            for (final String pkg : EXCLUDED_PACKAGES) {
                if (name.startsWith(pkg)) {
                    return;
                }
            }
        }
        final FlumeEvent flumeEvent = factory.createEvent(event, mdcIncludes, mdcExcludes, mdcRequired, mdcPrefix,
            eventPrefix, compressBody);
        flumeEvent.setBody(getLayout().toByteArray(flumeEvent));
        manager.send(flumeEvent);
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        boolean stopped = super.stop(timeout, timeUnit, false);
        stopped &= manager.stop(timeout, timeUnit);
        setStopped();
        return stopped;
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
     * @param connectionTimeoutMillis The amount of time in milliseconds to wait before a connection times out. Minimum is
     *                          1000.
     * @param requestTimeoutMillis The amount of time in milliseconds to wait before a request times out. Minimum is 1000.
     * @param agentRetries The number of times to retry an agent before failing to the next agent.
     * @param maxDelayMillis The maximum number of milliseconds to wait for a complete batch.
     * @param name The name of the Appender.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param excludes A comma separated list of MDC elements to exclude.
     * @param includes A comma separated list of MDC elements to include.
     * @param required A comma separated list of MDC elements that are required.
     * @param mdcPrefix The prefix to add to MDC key names.
     * @param eventPrefix The prefix to add to event key names.
     * @param compressBody If true the event body will be compressed.
     * @param batchSize Number of events to include in a batch. Defaults to 1.
     * @param lockTimeoutRetries Times to retry a lock timeout when writing to Berkeley DB.
     * @param factory The factory to use to create Flume events.
     * @param layout The layout to format the event.
     * @param filter A Filter to filter events.
     *
     * @return A Flume Avro Appender.
     */
    @PluginFactory
    public static FlumeAppender createAppender(@PluginElement("Agents") final Agent[] agents,
                                               @PluginElement("Properties") final Property[] properties,
                                               @PluginAttribute("hosts") final String hosts,
                                               @PluginAttribute("embedded") final String embedded,
                                               @PluginAttribute("type") final String type,
                                               @PluginAttribute("dataDir") final String dataDir,
                                               @PluginAliases("connectTimeout")
                                               @PluginAttribute("connectTimeoutMillis") final String connectionTimeoutMillis,
                                               @PluginAliases("requestTimeout")
                                               @PluginAttribute("requestTimeoutMillis") final String requestTimeoutMillis,
                                               @PluginAttribute("agentRetries") final String agentRetries,
                                               @PluginAliases("maxDelay") // deprecated
                                               @PluginAttribute("maxDelayMillis") final String maxDelayMillis,
                                               @PluginAttribute("name") final String name,
                                               @PluginAttribute("ignoreExceptions") final String ignore,
                                               @PluginAttribute("mdcExcludes") final String excludes,
                                               @PluginAttribute("mdcIncludes") final String includes,
                                               @PluginAttribute("mdcRequired") final String required,
                                               @PluginAttribute("mdcPrefix") final String mdcPrefix,
                                               @PluginAttribute("eventPrefix") final String eventPrefix,
                                               @PluginAttribute("compress") final String compressBody,
                                               @PluginAttribute("batchSize") final String batchSize,
                                               @PluginAttribute("lockTimeoutRetries") final String lockTimeoutRetries,
                                               @PluginElement("FlumeEventFactory") final FlumeEventFactory factory,
                                               @PluginElement("Layout") Layout<? extends Serializable> layout,
                                               @PluginElement("Filter") final Filter filter) {

        final boolean embed = embedded != null ? Boolean.parseBoolean(embedded) :
            (agents == null || agents.length == 0 || hosts == null || hosts.isEmpty()) && properties != null && properties.length > 0;
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        final boolean compress = Booleans.parseBoolean(compressBody, true);
        ManagerType managerType;
        if (type != null) {
            if (embed && embedded != null) {
                try {
                    managerType = ManagerType.getType(type);
                    LOGGER.warn("Embedded and type attributes are mutually exclusive. Using type " + type);
                } catch (final Exception ex) {
                    LOGGER.warn("Embedded and type attributes are mutually exclusive and type " + type +
                        " is invalid.");
                    managerType = ManagerType.EMBEDDED;
                }
            } else {
                try {
                    managerType = ManagerType.getType(type);
                } catch (final Exception ex) {
                    LOGGER.warn("Type " + type + " is invalid.");
                    managerType = ManagerType.EMBEDDED;
                }
            }
        }  else if (embed) {
           managerType = ManagerType.EMBEDDED;
        }  else {
           managerType = ManagerType.AVRO;
        }

        final int batchCount = Integers.parseInt(batchSize, 1);
        final int connectTimeoutMillis = Integers.parseInt(connectionTimeoutMillis, 0);
        final int reqTimeoutMillis = Integers.parseInt(requestTimeoutMillis, 0);
        final int retries = Integers.parseInt(agentRetries, 0);
        final int lockTimeoutRetryCount = Integers.parseInt(lockTimeoutRetries, DEFAULT_LOCK_TIMEOUT_RETRY_COUNT);
        final int delayMillis = Integers.parseInt(maxDelayMillis, DEFAULT_MAX_DELAY);

        if (layout == null) {
            final int enterpriseNumber = Rfc5424Layout.DEFAULT_ENTERPRISE_NUMBER;
            layout = Rfc5424Layout.createLayout(Facility.LOCAL0, null, enterpriseNumber, true, Rfc5424Layout.DEFAULT_MDCID,
                    mdcPrefix, eventPrefix, false, null, null, null, excludes, includes, required, null, false, null,
                    null);
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
                manager = FlumeAvroManager.getManager(name, getAgents(agents, hosts), batchCount, delayMillis, retries, connectTimeoutMillis, reqTimeoutMillis);
                break;
            case PERSISTENT:
                manager = FlumePersistentManager.getManager(name, getAgents(agents, hosts), properties, batchCount, retries,
                    connectTimeoutMillis, reqTimeoutMillis, delayMillis, lockTimeoutRetryCount, dataDir);
                break;
            default:
                LOGGER.debug("No manager type specified. Defaulting to AVRO");
                manager = FlumeAvroManager.getManager(name, getAgents(agents, hosts), batchCount, delayMillis, retries, connectTimeoutMillis, reqTimeoutMillis);
        }

        if (manager == null) {
            return null;
        }

        return new FlumeAppender(name, filter, layout,  ignoreExceptions, includes,
            excludes, required, mdcPrefix, eventPrefix, compress, factory, manager);
    }

    private static Agent[] getAgents(Agent[] agents, final String hosts) {
        if (agents == null || agents.length == 0) {
            if (hosts != null && !hosts.isEmpty()) {
                LOGGER.debug("Parsing agents from hosts parameter");
                final String[] hostports = hosts.split(",");
                agents = new Agent[hostports.length];
                for(int i = 0; i < hostports.length; ++i) {
                    final String[] h = hostports[i].split(":");
                    agents[i] = Agent.createAgent(h[0], h.length > 1 ? h[1] : null);
                }
            } else {
                LOGGER.debug("No agents provided, using defaults");
                agents = new Agent[] {Agent.createAgent(null, null)};
            }
        }

        LOGGER.debug("Using agents {}", agents);
        return agents;
    }
}
