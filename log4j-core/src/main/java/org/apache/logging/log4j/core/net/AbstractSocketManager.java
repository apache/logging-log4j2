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
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.OutputStreamManager;

/**
 * Abstract base class for managing sockets.
 */
public abstract class AbstractSocketManager extends OutputStreamManager {

    /**
     * The Internet address of the host.
     */
    protected final InetAddress inetAddress;

    /**
     * The name of the host.
     */
    protected final String host;

    /**
     * The port on the host.
     */
    protected final int port;

    /**
     * Constructs a new instance.
     *
     * @param name The unique name of this connection.
     * @param os The OutputStream to manage.
     * @param inetAddress The Internet address.
     * @param host The target host name.
     * @param port The target port number.
     * @param bufferSize The buffer size.
     */
    public AbstractSocketManager(
            final String name,
            final OutputStream os,
            final InetAddress inetAddress,
            final String host,
            final int port,
            final Layout<? extends Serializable> layout,
            final boolean writeHeader,
            final int bufferSize) {
        super(os, name, layout, writeHeader, bufferSize);
        this.inetAddress = inetAddress;
        this.host = host;
        this.port = port;
    }

    /**
     * Gets this AbstractSocketManager's content format. Specified by:
     * <ul>
     * <li>Key: "port" Value: provided "port" param</li>
     * <li>Key: "address" Value: provided "address" param</li>
     * </ul>
     *
     * @return Map of content format keys supporting AbstractSocketManager
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>(super.getContentFormat());
        result.put("port", Integer.toString(port));
        result.put("address", inetAddress.getHostAddress());
        return result;
    }

    /**
     * Gets the host.
     *
     * @return the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port.
     *
     * @return the port.
     */
    public int getPort() {
        return port;
    }
}
