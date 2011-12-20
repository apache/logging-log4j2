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

import com.cloudera.flume.handlers.avro.AvroFlumeEvent;
import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.core.appender.ManagerFactory;

import com.cloudera.flume.handlers.avro.FlumeEventAvroServer;
import com.cloudera.flume.handlers.avro.AvroEventConvertUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manager for FlumeAvroAppenders.
 */
public class FlumeAvroManager extends AbstractManager {

    /**
      The default reconnection delay (500 milliseconds or .5 seconds).
     */
    public static final int DEFAULT_RECONNECTION_DELAY   = 500;

    private static final int DEFAULT_RECONNECTS = 3;

    private static ManagerFactory factory = new AvroManagerFactory();

    private FlumeEventAvroServer client;

    private final Agent[] agents;

    private int current = 0;

    protected FlumeAvroManager(String name, Agent[] agents) {
        super(name);
        this.agents = agents;
        this.client = connect(agents);
    }

    /**
     * Return a FlumeAvroManager.
     * @param agents The agents to use.
     * @return A FlumeAvroManager.
     */
    public static FlumeAvroManager getManager(Agent[] agents) {
        if (agents == null || agents.length == 0) {
            throw new IllegalArgumentException("At least one agent is required");
        }

        StringBuilder sb = new StringBuilder("FlumeAvro[");
        boolean first = true;
        for (Agent agent : agents) {
            if (!first) {
                sb.append(",");
            }
            sb.append(agent.getHost()).append(":").append(agent.getPort());
            first = false;
        }
        sb.append("]");
        return (FlumeAvroManager) getManager(sb.toString(), factory, new FactoryData(agents));
    }

    /**
     * Return the agents.
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

    protected synchronized void send(FlumeEvent event, int delay, int retries)  {
        if (delay == 0) {
            delay = DEFAULT_RECONNECTION_DELAY;
        }
        if (retries == 0) {
            retries = DEFAULT_RECONNECTS;
        }
        AvroFlumeEvent avroEvent = AvroEventConvertUtil.toAvroEvent(event);
        int i = 0;

        String msg = "Error writing to " + getName();

        do {
            try {
                client.append(avroEvent);
                return;
            } catch (Exception ex) {
                if (i == retries - 1) {
                    msg = "Error writing to " + getName() + " at " + agents[0].getHost() + ":" + agents[0].getPort();
                    LOGGER.warn(msg, ex);
                    break;
                }
                sleep(delay);
            }
        } while (++i < retries);

        for (int index = 0; index < agents.length; ++index) {
            if (index == current) {
                continue;
            }
            Agent agent = agents[index];
            i = 0;
            do {
                try {

                    FlumeEventAvroServer c = connect(agent.getHost(), agent.getPort());
                    c.append(avroEvent);
                    client = c;
                    current = i;
                    return;
                } catch (Exception ex) {
                    if (i == retries - 1) {
                        String warnMsg = "Error writing to " + getName() + " at " + agent.getHost() + ":" +
                            agent.getPort();
                        LOGGER.warn(warnMsg, ex);
                        break;
                    }
                    sleep(delay);
                }
            } while (++i < retries);
        }

        throw new AppenderRuntimeException(msg);

    }

    private void sleep(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * There is a very good chance that this will always return the first agent even if it isn't available.
     * @param agents The list of agents to choose from
     * @return The FlumeEventAvroServer.
     */
    private FlumeEventAvroServer connect(Agent[] agents) {
        int i = 0;
        for (Agent agent : agents) {
            FlumeEventAvroServer server = connect(agent.getHost(), agent.getPort());
            if (server != null) {
                current = i;
                return server;
            }
            ++i;
        }
        throw new AppenderRuntimeException("Unable to connect to any agents");
    }

    private FlumeEventAvroServer connect(String hostname, int port) {
        URL url;

        try {
            url = new URL("http", hostname, port, "/");
        } catch (MalformedURLException ex) {
            LOGGER.error("Unable to create a URL for hostname " + hostname + " at port " + port, ex);
            return null;
        }

        try {
            return SpecificRequestor.getClient(FlumeEventAvroServer.class, new HttpTransceiver(url));
        } catch (IOException ioe) {
            LOGGER.error("Unable to create Avro client");
            return null;
        }
    }

    /**
     * Factory data.
     */
    private static class FactoryData {
        Agent[] agents;

        /**
         * Constructor.
         * @param agents The agents.
         */
        public FactoryData(Agent[] agents) {
            this.agents = agents;
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
        public FlumeAvroManager createManager(String name, FactoryData data) {
            try {

                return new FlumeAvroManager(name, data.agents);
            } catch (Exception ex) {
                LOGGER.error("Could not create FlumeAvroManager", ex);
            }
            return null;
        }
    }

}
