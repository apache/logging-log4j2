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
package org.apache.logging.log4j.core.net;

import org.apache.logging.log4j.core.appender.ManagerFactory;

import java.io.OutputStream;

/**
 * Socket Manager for UDP connections.
 */
public class DatagramSocketManager extends AbstractSocketManager {

    private static final DatagramSocketManagerFactory FACTORY = new DatagramSocketManagerFactory();

    /**
     * The Constructor.
     * @param os The OutputStream.
     * @param name The unique name of the connection.
     * @param host The host to connect to.
     * @param port The port on the host.
     */
    protected DatagramSocketManager(final OutputStream os, final String name, final String host, final int port) {
        super(name, os, null, host, port);
    }

    /**
     * Obtain a SocketManager.
     * @param host The host to connect to.
     * @param port The port on the host.
     * @return A DatagramSocketManager.
     */
    public static DatagramSocketManager getSocketManager(final String host, final int port) {
        if (host == null || host.length() == 0) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("A port value is required");
        }
        return (DatagramSocketManager) getManager("UDP:" + host + ":" + port, new FactoryData(host, port), FACTORY
        );
    }

    /**
     * Data for the factory.
     */
    private static class FactoryData {
        private final String host;
        private final int port;

        public FactoryData(final String host, final int port) {
            this.host = host;
            this.port = port;
        }
    }

    /**
     * Factory to create the DatagramSocketManager.
     */
    private static class DatagramSocketManagerFactory implements ManagerFactory<DatagramSocketManager, FactoryData> {

        public DatagramSocketManager createManager(final String name, final FactoryData data) {
            final OutputStream os = new DatagramOutputStream(data.host, data.port);
            return new DatagramSocketManager(os, name, data.host, data.port);
        }
    }
}
