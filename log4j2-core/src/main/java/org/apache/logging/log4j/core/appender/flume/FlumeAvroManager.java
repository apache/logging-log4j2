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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.core.appender.ManagerFactory;

import com.cloudera.flume.handlers.avro.FlumeEventAvroServer;
import com.cloudera.flume.handlers.avro.AvroEventConvertUtil;
import org.apache.logging.log4j.internal.StatusLogger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 */
public class FlumeAvroManager extends AbstractManager {

    private final int reconnectionDelay;

    private FlumeEventAvroServer client;

    private final Agent[] agents;

    private final int retries;

    private static final int DEFAULT_RECONNECTS = 3;

    private int current = 0;

    /**
      The default reconnection delay (500 milliseconds or .5 seconds).
     */
    public static final int DEFAULT_RECONNECTION_DELAY   = 500;

    private static ManagerFactory factory = new AvroManagerFactory();

    private static Logger logger = StatusLogger.getLogger();

    public static FlumeAvroManager getManager(Agent[] agents, int delay, int retries) {
        if (agents == null || agents.length == 0) {
            throw new IllegalArgumentException("At least one agent is required");
        }
        if (delay == 0) {
            delay = DEFAULT_RECONNECTION_DELAY;
        }
        if (retries == 0) {
            retries = DEFAULT_RECONNECTS;
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
        return (FlumeAvroManager) getManager(sb.toString(), factory, new FactoryData(agents, delay, retries));
    }


    public FlumeAvroManager(String name, Agent[] agents, int delay, int retries) {
        super(name);
        this.agents = agents;
        this.client = connect(agents);
        this.reconnectionDelay = delay;
        this.retries = retries;
    }

    protected synchronized void send(FlumeEvent event)  {
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
                    logger.warn(msg, ex);
                    break;
                }
                sleep();
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
                        logger.warn(warnMsg, ex);
                        break;
                    }
                    sleep();
                }
            } while (++i < retries);
        }

        throw new AppenderRuntimeException(msg);

    }

    private void sleep() {
        try {
            Thread.sleep(reconnectionDelay);
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

    public void releaseSub() {
    }

    private FlumeEventAvroServer connect(String hostname, int port) {
        URL url;

        try {
            url = new URL("http", hostname, port, "/");
        } catch (MalformedURLException ex) {
            logger.error("Unable to create a URL for hostname " + hostname + " at port " + port, ex);
            return null;
        }

        try {
            return SpecificRequestor.getClient(FlumeEventAvroServer.class, new HttpTransceiver(url));
        } catch (IOException ioe) {
            logger.error("Unable to create Avro client");
            return null;
        }
    }

    private static class FactoryData {
        Agent[] agents;
        int delay;
        int retries;

        public FactoryData(Agent[] agents, int delay, int retries) {
            this.agents = agents;
            this.delay = delay;
            this.retries = retries;
        }
    }

    private static class AvroManagerFactory implements ManagerFactory<FlumeAvroManager, FactoryData> {

        public FlumeAvroManager createManager(String name, FactoryData data) {
            try {

                return new FlumeAvroManager(name, data.agents, data.delay, data.retries);
            } catch (Exception ex) {
                logger.error("Could not create FlumeAvroManager", ex);
            }
            return null;
        }
    }

}
