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
package org.apache.logging.log4j.core.config.plugins.validation;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidHost;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.ValidPort;

@Plugin(name = "HostAndPort", category = "Test")
public class HostAndPort {

    private final InetSocketAddress address;

    private HostAndPort(final InetSocketAddress address) {
        this.address = address;
    }

    public boolean isValid() {
        return !address.isUnresolved();
    }

    @PluginFactory
    public static HostAndPort createPlugin(
        @ValidHost(message = "Unit test (host)") @PluginAttribute("host") final String host,
        @ValidPort(message = "Unit test (port)") @PluginAttribute("port") final int port) {
        return new HostAndPort(new InetSocketAddress(host, port));
    }

    @Override
    public String toString() {
        return "HostAndPort{" +
            "address=" + address +
            '}';
    }
}
