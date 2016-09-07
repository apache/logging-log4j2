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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.flume.Event;
import org.apache.flume.api.RpcClient;
import org.apache.flume.api.RpcClientFactory;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;

/**
 * Manager for FlumeAvroAppenders.
 */
public class FlumeAvroManager extends AbstractFlumeManager {

    private static final int MAX_RECONNECTS = 3;
    private static final int MINIMUM_TIMEOUT = 1000;

    private static AvroManagerFactory factory = new AvroManagerFactory();

    private final Agent[] agents;

    private final int batchSize;

    private final long delayNanos;
    private final int delayMillis;

    private final int retries;

    private final int connectTimeoutMillis;

    private final int requestTimeoutMillis;

    private final int current = 0;

    private RpcClient rpcClient = null;

    private BatchEvent batchEvent = new BatchEvent();
    private long nextSend = 0;

    /**
     * Constructor
     * @param name The unique name of this manager.
     * @param agents An array of Agents.
     * @param batchSize The number of events to include in a batch.
     * @param retries The number of times to retry connecting before giving up.
     * @param connectTimeout The connection timeout in ms.
     * @param requestTimeout The request timeout in ms.
     *
     */
    protected FlumeAvroManager(final String name, final String shortName, final Agent[] agents, final int batchSize,
                               final int delayMillis, final int retries, final int connectTimeout, final int requestTimeout) {
        super(name);
        this.agents = agents;
        this.batchSize = batchSize;
        this.delayMillis = delayMillis;
        this.delayNanos = TimeUnit.MILLISECONDS.toNanos(delayMillis);
        this.retries = retries;
        this.connectTimeoutMillis = connectTimeout;
        this.requestTimeoutMillis = requestTimeout;
        this.rpcClient = connect(agents, retries, connectTimeout, requestTimeout);
    }

    /**
     * Returns a FlumeAvroManager.
     * @param name The name of the manager.
     * @param agents The agents to use.
     * @param batchSize The number of events to include in a batch.
     * @param delayMillis The number of milliseconds to wait before sending an incomplete batch.
     * @param retries The number of times to retry connecting before giving up.
     * @param connectTimeoutMillis The connection timeout in ms.
     * @param requestTimeoutMillis The request timeout in ms.
     * @return A FlumeAvroManager.
     */
    public static FlumeAvroManager getManager(final String name, final Agent[] agents, int batchSize, final int delayMillis,
                                              final int retries, final int connectTimeoutMillis, final int requestTimeoutMillis) {
        if (agents == null || agents.length == 0) {
            throw new IllegalArgumentException("At least one agent is required");
        }

        if (batchSize <= 0) {
            batchSize = 1;
        }

        final StringBuilder sb = new StringBuilder("FlumeAvro[");
        boolean first = true;
        for (final Agent agent : agents) {
            if (!first) {
                sb.append(',');
            }
            sb.append(agent.getHost()).append(':').append(agent.getPort());
            first = false;
        }
        sb.append(']');
        return getManager(sb.toString(), factory,
                new FactoryData(name, agents, batchSize, delayMillis, retries, connectTimeoutMillis, requestTimeoutMillis));
    }

    /**
     * Returns the agents.
     * @return The agent array.
     */
    public Agent[] getAgents() {
        return agents;
    }

    /**
     * Returns the index of the current agent.
     * @return The index for the current agent.
     */
    public int getCurrent() {
        return current;
    }

    public int getRetries() {
        return retries;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public int getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getDelayMillis() {
        return delayMillis;
    }

    public synchronized void send(final BatchEvent events) {
        if (rpcClient == null) {
            rpcClient = connect(agents, retries, connectTimeoutMillis, requestTimeoutMillis);
        }

        if (rpcClient != null) {
            try {
                LOGGER.trace("Sending batch of {} events", events.getEvents().size());
                rpcClient.appendBatch(events.getEvents());
            } catch (final Exception ex) {
                rpcClient.close();
                rpcClient = null;
                final String msg = "Unable to write to " + getName() + " at " + agents[current].getHost() + ':' +
                    agents[current].getPort();
                LOGGER.warn(msg, ex);
                throw new AppenderLoggingException("No Flume agents are available");
            }
        }  else {
            final String msg = "Unable to write to " + getName() + " at " + agents[current].getHost() + ':' +
                agents[current].getPort();
            LOGGER.warn(msg);
            throw new AppenderLoggingException("No Flume agents are available");
        }
    }

    @Override
    public synchronized void send(final Event event)  {
        if (batchSize == 1) {
            if (rpcClient == null) {
                rpcClient = connect(agents, retries, connectTimeoutMillis, requestTimeoutMillis);
            }

            if (rpcClient != null) {
                try {
                    rpcClient.append(event);
                } catch (final Exception ex) {
                    rpcClient.close();
                    rpcClient = null;
                    final String msg = "Unable to write to " + getName() + " at " + agents[current].getHost() + ':' +
                            agents[current].getPort();
                    LOGGER.warn(msg, ex);
                    throw new AppenderLoggingException("No Flume agents are available");
                }
            } else {
                final String msg = "Unable to write to " + getName() + " at " + agents[current].getHost() + ':' +
                        agents[current].getPort();
                LOGGER.warn(msg);
                throw new AppenderLoggingException("No Flume agents are available");
            }
        } else {
            batchEvent.addEvent(event);
            final int eventCount = batchEvent.getEvents().size();
            if (eventCount == 1) {
                nextSend = System.nanoTime() + delayNanos;
            }
            if (eventCount >= batchSize || System.nanoTime() >= nextSend) {
                send(batchEvent);
                batchEvent = new BatchEvent();
            }
        }
    }

    /**
     * There is a very good chance that this will always return the first agent even if it isn't available.
     * @param agents The list of agents to choose from
     * @return The FlumeEventAvroServer.
     */
    private RpcClient connect(final Agent[] agents, int retries, final int connectTimeoutMillis, final int requestTimeoutMillis) {
        try {
            final Properties props = new Properties();

            props.put("client.type", "default_failover");

            int agentCount = 1;
            final StringBuilder sb = new StringBuilder();
            for (final Agent agent : agents) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                final String hostName = "host" + agentCount++;
                props.put("hosts." + hostName, agent.getHost() + ':' + agent.getPort());
                sb.append(hostName);
            }
            props.put("hosts", sb.toString());
            if (batchSize > 0) {
                props.put("batch-size", Integer.toString(batchSize));
            }
            if (retries > 1) {
                if (retries > MAX_RECONNECTS) {
                    retries = MAX_RECONNECTS;
                }
                props.put("max-attempts", Integer.toString(retries * agents.length));
            }
            if (requestTimeoutMillis >= MINIMUM_TIMEOUT) {
                props.put("request-timeout", Integer.toString(requestTimeoutMillis));
            }
            if (connectTimeoutMillis >= MINIMUM_TIMEOUT) {
                props.put("connect-timeout", Integer.toString(connectTimeoutMillis));
            }
            return RpcClientFactory.getInstance(props);
        } catch (final Exception ex) {
            LOGGER.error("Unable to create Flume RPCClient: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
    	boolean closed = true;
        if (rpcClient != null) {
            try {
                synchronized(this) {
                    try {
                        if (batchSize > 1 && batchEvent.getEvents().size() > 0) {
                            send(batchEvent);
                        }
                    } catch (final Exception ex) {
                        LOGGER.error("Error sending final batch: {}", ex.getMessage());
                        closed = false;
                    }
                }
                rpcClient.close();
            } catch (final Exception ex) {
                LOGGER.error("Attempt to close RPC client failed", ex);
                closed = false;
            }
        }
        rpcClient = null;
        return closed;
    }

    /**
     * Factory data.
     */
    private static class FactoryData {
        private final String name;
        private final Agent[] agents;
        private final int batchSize;
        private final int delayMillis;
        private final int retries;
        private final int conntectTimeoutMillis;
        private final int requestTimeoutMillis;

        /**
         * Constructor.
         * @param name The name of the Appender.
         * @param agents The agents.
         * @param batchSize The number of events to include in a batch.
         */
        public FactoryData(final String name, final Agent[] agents, final int batchSize, final int delayMillis,
                final int retries, final int connectTimeoutMillis, final int requestTimeoutMillis) {
            this.name = name;
            this.agents = agents;
            this.batchSize = batchSize;
            this.delayMillis = delayMillis;
            this.retries = retries;
            this.conntectTimeoutMillis = connectTimeoutMillis;
            this.requestTimeoutMillis = requestTimeoutMillis;
        }
    }

    /**
     * Avro Manager Factory.
     */
    private static class AvroManagerFactory implements ManagerFactory<FlumeAvroManager, FactoryData> {

        /**
         * Create the FlumeAvroManager.
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return The FlumeAvroManager.
         */
        @Override
        public FlumeAvroManager createManager(final String name, final FactoryData data) {
            try {

                return new FlumeAvroManager(name, data.name, data.agents, data.batchSize, data.delayMillis,
                        data.retries, data.conntectTimeoutMillis, data.requestTimeoutMillis);
            } catch (final Exception ex) {
                LOGGER.error("Could not create FlumeAvroManager", ex);
            }
            return null;
        }
    }

}
