/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.net;

import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.util.Strings;

/**
 * Socket Manager for UDP connections.
 */
public class DatagramSocketManager extends AbstractSocketManager {

    private static final DatagramSocketManagerFactory FACTORY = new DatagramSocketManagerFactory();

    /**
     * The Constructor.
     * @param name the unique name of the connection
     * @param os the OutputStream
     * @param inetAddress the Internet Protocol (IP) address
     * @param host the host to connect to
     * @param port the port on the host
     * @param layout the layout
     * @param bufferSize the buffer size
     */
    protected DatagramSocketManager(
            final String name,
            final OutputStream os,
            final InetAddress inetAddress,
            final String host,
            final int port,
            final Layout<? extends Serializable> layout,
            final int bufferSize) {
        super(name, os, inetAddress, host, port, layout, true, bufferSize);
    }

    /**
     * Obtain a SocketManager.
     * @param host The host to connect to.
     * @param port The port on the host.
     * @param layout The layout.
     * @param bufferSize The buffer size.
     * @return A DatagramSocketManager.
     */
    public static DatagramSocketManager getSocketManager(
            final String host, final int port, final Layout<? extends Serializable> layout, final int bufferSize) {
        if (Strings.isEmpty(host)) {
            throw new IllegalArgumentException("A host name is required");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("A port value is required");
        }
        return (DatagramSocketManager)
                getManager("UDP:" + host + ':' + port, new FactoryData(host, port, layout, bufferSize), FACTORY);
    }

    /**
     * Gets this DatagramSocketManager's content format. Specified by:
     * <ul>
     * <li>Key: "protocol" Value: "udp"</li>
     * <li>Key: "direction" Value: "out"</li>
     * </ul>
     *
     * @return Map of content format keys supporting DatagramSocketManager
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>(super.getContentFormat());
        result.put("protocol", "udp");
        result.put("direction", "out");
        return result;
    }

    /**
     * Data for the factory.
     */
    private static class FactoryData {
        private final String host;
        private final int port;
        private final Layout<? extends Serializable> layout;
        private final int bufferSize;

        public FactoryData(
                final String host, final int port, final Layout<? extends Serializable> layout, final int bufferSize) {
            this.host = host;
            this.port = port;
            this.layout = layout;
            this.bufferSize = bufferSize;
        }
    }

    /**
     * Factory to create the DatagramSocketManager.
     */
    private static class DatagramSocketManagerFactory implements ManagerFactory<DatagramSocketManager, FactoryData> {

        @Override
        public DatagramSocketManager createManager(final String name, final FactoryData data) {
            InetAddress inetAddress;
            try {
                inetAddress = InetAddress.getByName(data.host);
            } catch (final UnknownHostException ex) {
                LOGGER.error("Could not find address of " + data.host, ex);
                return null;
            }
            final OutputStream os =
                    new DatagramOutputStream(data.host, data.port, data.layout.getHeader(), data.layout.getFooter());
            return new DatagramSocketManager(name, os, inetAddress, data.host, data.port, data.layout, data.bufferSize);
        }
    }
}
