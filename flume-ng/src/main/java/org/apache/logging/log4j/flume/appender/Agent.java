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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Agent Specification for FlumeAvroAppender.
 */
@Plugin(name = "Agent", type = "Core", printObject = true)
public final class Agent {

    private static final String DEFAULT_HOST = "localhost";

    private static final int DEFAULT_PORT = 35853;

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String host;

    private final int port;

    private Agent(final String host, final int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Retrieve the host name.
     * @return The name of the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Retrieve the port number.
     * @return The port number.
     */
    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "host=" + host + " port=" + port;
    }

    /**
     * Create an Agent.
     * @param host The host name.
     * @param port The port number.
     * @return The Agent.
     */
    @PluginFactory
    public static Agent createAgent(@PluginAttr("host") String host,
                                    @PluginAttr("port") final String port) {
        if (host == null) {
            host = DEFAULT_HOST;
        }

        int portNum;
        if (port != null) {
            try {
                portNum = Integer.parseInt(port);
            } catch (final Exception ex) {
                LOGGER.error("Error parsing port number " + port, ex);
                return null;
            }
        } else {
            portNum = DEFAULT_PORT;
        }
        return new Agent(host, portNum);
    }
}
