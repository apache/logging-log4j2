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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.agent.embedded.EmbeddedAgent;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 *
 */
public class FlumeEmbeddedManager extends AbstractFlumeManager {

    private static final String FILE_SEP = PropertiesUtil.getProperties().getStringProperty("file.separator");

    private static final String IN_MEMORY = "InMemory";

    private static FlumeManagerFactory factory = new FlumeManagerFactory();

    private final EmbeddedAgent agent;

    private final String shortName;


    /**
     * Constructor
     * @param name The unique name of this manager.
     * @param shortName The short version of the agent name.
     * @param agent The embedded agent.
     */
    protected FlumeEmbeddedManager(final String name, final String shortName, final EmbeddedAgent agent) {
        super(name);
        this.agent = agent;
        this.shortName = shortName;
    }

    /**
     * Returns a FlumeEmbeddedManager.
     * @param name The name of the manager.
     * @param agents The agents to use.
     * @param properties Properties for the embedded manager.
     * @param batchSize The number of events to include in a batch.
     * @param dataDir The directory where the Flume FileChannel should write to.
     * @return A FlumeAvroManager.
     */
    public static FlumeEmbeddedManager getManager(final String name, final Agent[] agents, final Property[] properties,
                                                  int batchSize, final String dataDir) {

        if (batchSize <= 0) {
            batchSize = 1;
        }

        if ((agents == null || agents.length == 0) && (properties == null || properties.length == 0)) {
            throw new IllegalArgumentException("Either an Agent or properties are required");
        } else if (agents != null && agents.length > 0 && properties != null && properties.length > 0) {
            throw new IllegalArgumentException("Cannot configure both Agents and Properties.");
        }

        final StringBuilder sb = new StringBuilder();
        boolean first = true;

        if (agents != null && agents.length > 0) {
            sb.append(name).append('[');
            for (final Agent agent : agents) {
                if (!first) {
                    sb.append('_');
                }
                sb.append(agent.getHost()).append('-').append(agent.getPort());
                first = false;
            }
            sb.append(']');
        } else {
            String sep = Strings.EMPTY;
            sb.append(name).append('-');
            final StringBuilder props = new StringBuilder();
            for (final Property prop : properties) {
                props.append(sep);
                props.append(prop.getName()).append('=').append(prop.getValue());
                sep = "_";
            }
            sb.append(NameUtil.md5(props.toString()));
        }
        return getManager(sb.toString(), factory,
                new FactoryData(name, agents, properties, batchSize, dataDir));
    }

    @Override
    public void send(final Event event) {
        try {
            agent.put(event);
        } catch (final EventDeliveryException ex) {
            throw new LoggingException("Unable to deliver event to Flume Appender " + shortName, ex);
        }
    }

    @Override
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        agent.stop();
        return true;
    }

    /**
     * Factory data.
     */
    private static class FactoryData {
        private final Agent[] agents;
        private final Property[] properties;
        private final int batchSize;
        private final String dataDir;
        private final String name;

        /**
         * Constructor.
         * @param name The name of the Appender.
         * @param agents The agents.
         * @param properties The Flume configuration properties.
         * @param batchSize The number of events to include in a batch.
         * @param dataDir The directory where Flume should write to.
         */
        public FactoryData(final String name, final Agent[] agents, final Property[] properties, final int batchSize,
                           final String dataDir) {
            this.name = name;
            this.agents = agents;
            this.batchSize = batchSize;
            this.properties = properties;
            this.dataDir = dataDir;
        }
    }

    /**
     * Avro Manager Factory.
     */
    private static class FlumeManagerFactory implements ManagerFactory<FlumeEmbeddedManager, FactoryData> {

        /**
         * Create the FlumeAvroManager.
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return The FlumeAvroManager.
         */
        @Override
        public FlumeEmbeddedManager createManager(final String name, final FactoryData data) {
            try {
                final Map<String, String> props = createProperties(data.name, data.agents, data.properties,
                    data.batchSize, data.dataDir);
                final EmbeddedAgent agent = new EmbeddedAgent(name);
                agent.configure(props);
                agent.start();
                LOGGER.debug("Created Agent " + name);
                return new FlumeEmbeddedManager(name, data.name, agent);
            } catch (final Exception ex) {
                LOGGER.error("Could not create FlumeEmbeddedManager", ex);
            }
            return null;
        }

        private Map<String, String> createProperties(final String name, final Agent[] agents,
                                                     final Property[] properties, final int batchSize, String dataDir) {
            final Map<String, String> props = new HashMap<>();

            if ((agents == null || agents.length == 0) && (properties == null || properties.length == 0)) {
                LOGGER.error("No Flume configuration provided");
                throw new ConfigurationException("No Flume configuration provided");
            }

            if (agents != null && agents.length > 0 && properties != null && properties.length > 0) {
                LOGGER.error("Agents and Flume configuration cannot both be specified");
                throw new ConfigurationException("Agents and Flume configuration cannot both be specified");
            }

            if (agents != null && agents.length > 0) {

                if (Strings.isNotEmpty(dataDir)) {
                    if (dataDir.equals(IN_MEMORY)) {
                        props.put("channel.type", "memory");
                    } else {
                        props.put("channel.type", "file");

                        if (!dataDir.endsWith(FILE_SEP)) {
                            dataDir = dataDir + FILE_SEP;
                        }

                        props.put("channel.checkpointDir", dataDir + "checkpoint");
                        props.put("channel.dataDirs", dataDir + "data");
                    }

                } else {
                    props.put("channel.type", "file");
                }

                final StringBuilder sb = new StringBuilder();
                String leading = Strings.EMPTY;
                final int priority = agents.length;
                for (int i = 0; i < priority; ++i) {
                    sb.append(leading).append("agent").append(i);
                    leading = " ";
                    final String prefix = "agent" + i;
                    props.put(prefix + ".type", "avro");
                    props.put(prefix + ".hostname", agents[i].getHost());
                    props.put(prefix + ".port", Integer.toString(agents[i].getPort()));
                    props.put(prefix + ".batch-size", Integer.toString(batchSize));
                    props.put("processor.priority." + prefix, Integer.toString(agents.length - i));
                }
                props.put("sinks", sb.toString());
                props.put("processor.type", "failover");
            } else {
                String[] sinks = null;

                for (final Property property : properties) {
                    final String key = property.getName();

                    if (Strings.isEmpty(key)) {
                        final String msg = "A property name must be provided";
                        LOGGER.error(msg);
                        throw new ConfigurationException(msg);
                    }

                    final String upperKey = key.toUpperCase(Locale.ENGLISH);

                    if (upperKey.startsWith(name.toUpperCase(Locale.ENGLISH))) {
                        final String msg =
                            "Specification of the agent name is not allowed in Flume Appender configuration: " + key;
                        LOGGER.error(msg);
                        throw new ConfigurationException(msg);
                    }

                    final String value = property.getValue();
                    if (Strings.isEmpty(value)) {
                        final String msg = "A value for property " + key + " must be provided";
                        LOGGER.error(msg);
                        throw new ConfigurationException(msg);
                    }

                    if (upperKey.equals("SINKS")) {
                        sinks = value.trim().split(" ");
                    }

                    props.put(key, value);
                }

                if (sinks == null || sinks.length == 0) {
                    final String msg = "At least one Sink must be specified";
                    LOGGER.error(msg);
                    throw new ConfigurationException(msg);
                }
            }
            return props;
        }

    }

}
