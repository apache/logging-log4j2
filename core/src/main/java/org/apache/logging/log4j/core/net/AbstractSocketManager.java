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

import org.apache.logging.log4j.core.appender.OutputStreamManager;

import java.io.OutputStream;
import java.net.InetAddress;

/**
 * Abstract base class for managing sockets.
 */
public abstract class AbstractSocketManager extends OutputStreamManager {

    /**
     * The internet address of the host.
     */
    protected final InetAddress address;
    /**
     * The name of the host.
     */
    protected final String host;
    /**
     * The port on the host.
     */
    protected final int port;

    /**
     * The Constructor.
     * @param name The unique name of this connection.
     * @param os The OutputStream to manage.
     * @param addr The internet address.
     * @param host The target host name.
     * @param port The target port number.
     */
    public AbstractSocketManager(final String name, final OutputStream os, final InetAddress addr, final String host,
                                 final int port) {
        super(os, name);
        this.address = addr;
        this.host = host;
        this.port = port;
    }

}
