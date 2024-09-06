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
package org.apache.logging.log4j.core.appender;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.net.TcpSocketManager;

/**
 * {@link TcpSocketManager.HostResolver} implementation always resolving to the given list of {@link #addresses}.
 */
final class FixedHostResolver extends TcpSocketManager.HostResolver {

    private final List<InetSocketAddress> addresses;

    private FixedHostResolver(final List<InetSocketAddress> addresses) {
        this.addresses = addresses;
    }

    static FixedHostResolver ofServers(final LineReadingTcpServer... servers) {
        final List<InetSocketAddress> addresses = Arrays.stream(servers)
                .map(server -> (InetSocketAddress) server.getServerSocket().getLocalSocketAddress())
                .collect(Collectors.toList());
        return new FixedHostResolver(addresses);
    }

    @Override
    public List<InetSocketAddress> resolveHost(final String ignoredHost, final int ignoredPort) {
        return addresses;
    }
}
