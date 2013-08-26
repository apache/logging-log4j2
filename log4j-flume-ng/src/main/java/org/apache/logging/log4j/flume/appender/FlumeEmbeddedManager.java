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

import java.util.Locale;
import java.util.Properties;

import org.apache.flume.Event;
import org.apache.flume.SourceRunner;
import org.apache.flume.node.NodeConfiguration;
import org.apache.flume.node.nodemanager.DefaultLogicalNodeManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.helpers.NameUtil;
import org.apache.logging.log4j.core.helpers.Strings;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 *
 */
public class FlumeEmbeddedManager extends AbstractFlumeManager {

    /** Name for the Flume source */
    protected static final String SOURCE_NAME = "log4j-source";

    private static final String FILE_SEP = PropertiesUtil.getProperties().getStringProperty("file.separator");

    private static final String IN_MEMORY = "InMemory";

    private static FlumeManagerFactory factory = new FlumeManagerFactory();

    private final FlumeNode node;

    private NodeConfiguration conf;

    private final Log4jEventSource source;

    private final String shortName;


    /**
     * Constructor
     * @param name The unique name of this manager.
     * @param node The Flume Node.
     */
    protected FlumeEmbeddedManager(final String name, final String shortName, final FlumeNode node) {
        super(name);
        this.node = node;
        this.shortName = shortName;
        final SourceRunner runner = node.getConfiguration().getSourceRunners().get(SOURCE_NAME);
        if (runner == null || runner.getSource() == null) {
            throw new IllegalStateException("No Source has been created for Appender " + shortName);
        }
        source  = (Log4jEventSource) runner.getSource();
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
            sb.append("FlumeEmbedded[");
            for (final Agent agent : agents) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(agent.getHost()).append(":").append(agent.getPort());
                first = false;
            }
            sb.append("]");
        } else {
            String sep = "";
            sb.append(name).append(":");
            final StringBuilder props = new StringBuilder();
            for (final Property prop : properties) {
                props.append(sep);
                props.append(prop.getName()).append("=").append(prop.getValue());
                sep = ",";
            }
            sb.append(NameUtil.md5(props.toString()));
        }
        return getManager(sb.toString(), factory,
                new FactoryData(name, agents, properties, batchSize, dataDir));
    }

    @Override
    public void send(final Event event) {
        source.send(event);
    }

    @Override
    protected void releaseSub() {
        node.stop();
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
        private static final String SOURCE_TYPE = Log4jEventSource.class.getName();

        /**
         * Create the FlumeAvroManager.
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return The FlumeAvroManager.
         */
        @Override
        public FlumeEmbeddedManager createManager(final String name, final FactoryData data) {
            try {
                final DefaultLogicalNodeManager nodeManager = new DefaultLogicalNodeManager();
                final Properties props = createProperties(data.name, data.agents, data.properties, data.batchSize,
                    data.dataDir);
                final FlumeConfigurationBuilder builder = new FlumeConfigurationBuilder();
                final NodeConfiguration conf = builder.load(data.name, props, nodeManager);

                final FlumeNode node = new FlumeNode(nodeManager, nodeManager, conf);

                node.start();

                return new FlumeEmbeddedManager(name, data.name, node);
            } catch (final Exception ex) {
                LOGGER.error("Could not create FlumeEmbeddedManager", ex);
            }
            return null;
        }

        private Properties createProperties(final String name, final Agent[] agents, final Property[] properties,
                                            final int batchSize, String dataDir) {
            final Properties props = new Properties();

            if ((agents == null || agents.length == 0) && (properties == null || properties.length == 0)) {
                LOGGER.error("No Flume configuration provided");
                throw new ConfigurationException("No Flume configuration provided");
            }

            if ((agents != null && agents.length > 0 && properties != null && properties.length > 0)) {
                LOGGER.error("Agents and Flume configuration cannot both be specified");
                throw new ConfigurationException("Agents and Flume configuration cannot both be specified");
            }

            if (agents != null && agents.length > 0) {
                props.put(name + ".sources", FlumeEmbeddedManager.SOURCE_NAME);
                props.put(name + ".sources." + FlumeEmbeddedManager.SOURCE_NAME + ".type", SOURCE_TYPE);

                if (dataDir != null && dataDir.length() > 0) {
                    if (dataDir.equals(IN_MEMORY)) {
                        props.put(name + ".channels", "primary");
                        props.put(name + ".channels.primary.type", "memory");
                    } else {
                        props.put(name + ".channels", "primary");
                        props.put(name + ".channels.primary.type", "file");

                        if (!dataDir.endsWith(FILE_SEP)) {
                            dataDir = dataDir + FILE_SEP;
                        }

                        props.put(name + ".channels.primary.checkpointDir", dataDir + "checkpoint");
                        props.put(name + ".channels.primary.dataDirs", dataDir + "data");
                    }

                } else {
                    props.put(name + ".channels", "primary");
                    props.put(name + ".channels.primary.type", "file");
                }

                final StringBuilder sb = new StringBuilder();
                String leading = "";
                int priority = agents.length;
                for (int i = 0; i < agents.length; ++i) {
                    sb.append(leading).append("agent").append(i);
                    leading = " ";
                    final String prefix = name + ".sinks.agent" + i;
                    props.put(prefix + ".channel", "primary");
                    props.put(prefix + ".type", "avro");
                    props.put(prefix + ".hostname", agents[i].getHost());
                    props.put(prefix + ".port", Integer.toString(agents[i].getPort()));
                    props.put(prefix + ".batch-size", Integer.toString(batchSize));
                    props.put(name + ".sinkgroups.group1.processor.priority.agent" + i, Integer.toString(priority));
                    --priority;
                }
                props.put(name + ".sinks", sb.toString());
                props.put(name + ".sinkgroups", "group1");
                props.put(name + ".sinkgroups.group1.sinks", sb.toString());
                props.put(name + ".sinkgroups.group1.processor.type", "failover");
                final String sourceChannels = "primary";
                props.put(name + ".channels", sourceChannels);
                props.put(name + ".sources." + FlumeEmbeddedManager.SOURCE_NAME + ".channels", sourceChannels);
            } else {
                String channels = null;
                String[] sinks = null;

                props.put(name + ".sources", FlumeEmbeddedManager.SOURCE_NAME);
                props.put(name + ".sources." + FlumeEmbeddedManager.SOURCE_NAME + ".type", SOURCE_TYPE);

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

                    // Prohibit setting the sources as they are set above but allow interceptors to be set
                    if (upperKey.startsWith("SOURCES.") && !upperKey.startsWith("SOURCES.LOG4J-SOURCE.INTERCEPTORS")) {
                        final String msg = "Specification of Sources is not allowed in Flume Appender: " + key;
                        LOGGER.error(msg);
                        throw new ConfigurationException(msg);
                    }

                    final String value = property.getValue();
                    if (Strings.isEmpty(value)) {
                        final String msg = "A value for property " + key + " must be provided";
                        LOGGER.error(msg);
                        throw new ConfigurationException(msg);
                    }

                    if (upperKey.equals("CHANNELS")) {
                        channels = value.trim();
                    } else if (upperKey.equals("SINKS")) {
                        sinks = value.trim().split(" ");
                    }

                    props.put(name + '.' + key, value);
                }

                String sourceChannels = channels;

                if (channels == null) {
                    sourceChannels = "primary";
                    props.put(name + ".channels", sourceChannels);
                }

                props.put(name + ".sources." + FlumeEmbeddedManager.SOURCE_NAME + ".channels", sourceChannels);

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
