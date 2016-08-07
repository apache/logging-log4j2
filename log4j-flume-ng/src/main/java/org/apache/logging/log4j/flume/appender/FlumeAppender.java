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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.Rfc5424Layout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Integers;

/**
 * An Appender that uses the Avro protocol to route events to Flume.
 */
@Plugin(name = "Flume", category = "Core", elementType = "appender", printObject = true)
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
    @Deprecated
    public static FlumeAppender createAppender(Agent[] agents,
                                               final Property[] properties,
                                               final String embedded,
                                               final String type,
                                               final String dataDir,
                                               @PluginAliases("connectTimeout")
                                               final String connectionTimeoutMillis,
                                               @PluginAliases("requestTimeout")
                                               final String requestTimeoutMillis,
                                               final String agentRetries,
                                               // deprecated
                                               final String maxDelayMillis,
                                               final String name,
                                               final String ignore,
                                               final String excludes,
                                               final String includes,
                                               final String required,
                                               final String mdcPrefix,
                                               final String eventPrefix,
                                               final String compressBody,
                                               final String batchSize,
                                               final String lockTimeoutRetries,
                                               final FlumeEventFactory factory,
                                               Layout<? extends Serializable> layout,
                                               final Filter filter) {


      return newBuilder().setAgents(agents).setProperties(properties)
              .setEmbedded(embedded)
              .setType(type)
              .setDataDir(dataDir)
              .setConnectionTimeoutMillis(connectionTimeoutMillis)
              .setRequestTimeoutMillis(requestTimeoutMillis)
              .setAgentRetries(agentRetries)
              .setMaxDelayMillis(maxDelayMillis)
              .setName(name)
              .setIgnore(ignore)
              .setExcludes(excludes)
              .setIncludes(includes)
              .setMdcPrefix(mdcPrefix)
              .setEventPrefix(eventPrefix)
              .setCompressBody(compressBody)
              .setBatchSize(batchSize)
              .setLockTimeoutRetries(lockTimeoutRetries)
              .setFactory(factory)
              .setLayout(layout)
              .setFilter(filter)
              .build();
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builds FlumeAppender instances.
     */
    public static class Builder implements org.apache.logging.log4j.core.util.Builder<FlumeAppender> {

        @PluginElement("Layout")
        @Required
        private Layout<? extends Serializable> layout;

        @PluginElement("Filter")
        private Filter filter;

        @PluginBuilderAttribute
        @Required
        private String name;

        @PluginElement("Agents")
        private Agent[] agents;

        @PluginElement("Properties")
        private Property[] properties;

        @PluginBuilderAttribute
         private String hosts;

        @PluginBuilderAttribute
        private String embedded;

        @PluginBuilderAttribute
        private String type;

        @PluginBuilderAttribute
        private String dataDir;

        @PluginAliases("connectTimeout")
        @PluginBuilderAttribute
        private String connectionTimeoutMillis;

        @PluginAliases("requestTimeout")

        @PluginBuilderAttribute
        private String requestTimeoutMillis;

        @PluginBuilderAttribute
        private String agentRetries;

        @PluginAliases("maxDelay") // deprecated
        @PluginBuilderAttribute
        private String maxDelayMillis;

        @PluginBuilderAttribute
        private String ignore;

        @PluginBuilderAttribute
        private String excludes;

        @PluginBuilderAttribute
        private String includes;

        @PluginBuilderAttribute
        private String required;

        @PluginBuilderAttribute
        private String mdcPrefix;

        @PluginBuilderAttribute
        private String eventPrefix;

        @PluginBuilderAttribute
        private String compressBody;

        @PluginBuilderAttribute
        private String batchSize;

        @PluginBuilderAttribute
        private String lockTimeoutRetries;

        @PluginElement("FlumeEventFactory")
        private FlumeEventFactory factory;

        public Builder setLayout(final Layout<? extends Serializable> aLayout) {
            this.layout = aLayout;
            return this;
        }

        public Builder setFilter(final Filter aFilter) {
            this.filter = aFilter;
            return this;
        }

        public Builder setName(final String aName) {
            this.name = aName;
            return this;
        }

        public Builder setAgents(final Agent[] agents) {
            this.agents = agents;
            return this;
        }

        public Builder setProperties(final Property[] properties) {
            this.properties = properties;
            return this;
        }

        public Builder setHosts(final String hosts) {
            this.hosts = hosts;
            return this;
        }

        public Builder setEmbedded(final String embedded) {
            this.embedded = embedded;
            return this;
        }

        public Builder setType(final String type) {
            this.type = type;
            return this;
        }

        public Builder setDataDir(final String dataDir) {
            this.dataDir = dataDir;
            return this;
        }

        public Builder setConnectionTimeoutMillis(final String connectionTimeoutMillis) {
            this.connectionTimeoutMillis = connectionTimeoutMillis;
            return this;
        }

        public Builder setRequestTimeoutMillis(final String requestTimeoutMillis) {
            this.requestTimeoutMillis = requestTimeoutMillis;
            return this;
        }

        public Builder setAgentRetries(final String agentRetries) {
            this.agentRetries = agentRetries;
            return this;
        }

        public Builder setMaxDelayMillis(final String maxDelayMillis) {
            this.maxDelayMillis = maxDelayMillis;
            return this;
        }

        public Builder setIgnore(final String ignore) {
            this.ignore = ignore;
            return this;
        }

        public Builder setExcludes(final String excludes) {
            this.excludes = excludes;
            return this;
        }

        public Builder setIncludes(final String includes) {
            this.includes = includes;
            return this;
        }

        public Builder setRequired(final String required) {
            this.required = required;
            return this;
        }

        public Builder setMdcPrefix(final String mdcPrefix) {
            this.mdcPrefix = mdcPrefix;
            return this;
        }

        public Builder setEventPrefix(final String eventPrefix) {
            this.eventPrefix = eventPrefix;
            return this;
        }

        public Builder setCompressBody(final String compressBody) {
            this.compressBody = compressBody;
            return this;
        }

        public Builder setBatchSize(final String batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder setLockTimeoutRetries(final String lockTimeoutRetries) {
            this.lockTimeoutRetries = lockTimeoutRetries;
            return this;
        }

        public Builder setFactory(final FlumeEventFactory factory) {
            this.factory = factory;
            return this;
        }

        @Override
        public FlumeAppender build() {
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
    }
    
    private static Agent[] getAgents(Agent[] agents, String hosts) {
        if (agents == null || agents.length == 0) {
            if(hosts != null && !hosts.isEmpty()) {
                LOGGER.debug("Parsing agents from hosts parameter");
                String[] hostports = hosts.split(",");
                agents = new Agent[hostports.length];
                for(int i = 0; i < hostports.length; ++i) {
                    String[] h = hostports[i].split(":");
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
