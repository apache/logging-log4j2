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

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.plugins.validation.constraints.ValidPort;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Plugin to hold a hostname and port (socket address).
 *
 * @since 2.8
 */
@Configurable(printObject = true)
@Plugin
public class SocketAddress {

    /**
     * Creates a SocketAddress corresponding to {@code localhost:0}.
     *
     * @return a SocketAddress for {@code localhost:0}
     */
    public static SocketAddress getLoopback() {
        return new SocketAddress(InetAddress.getLoopbackAddress(), 0);
    }

    // never null
    private final InetSocketAddress socketAddress;

    private SocketAddress(final InetAddress host, final int port) {
        this.socketAddress = new InetSocketAddress(host, port);
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public int getPort() {
        return socketAddress.getPort();
    }

    public InetAddress getAddress() {
        return socketAddress.getAddress();
    }

    public String getHostName() {
        return socketAddress.getHostName();
    }

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<SocketAddress> {

        @PluginBuilderAttribute
        @ValidHost
        private InetAddress host;

        @PluginBuilderAttribute
        @ValidPort
        private int port;

        public Builder setHost(final InetAddress host) {
            this.host = host;
            return this;
        }

        public Builder setPort(final int port) {
            this.port = port;
            return this;
        }

        @Override
        public SocketAddress build() {
            return new SocketAddress(host, port);
        }
    }

    @Override
    public String toString() {
        return socketAddress.toString();
    }

}
