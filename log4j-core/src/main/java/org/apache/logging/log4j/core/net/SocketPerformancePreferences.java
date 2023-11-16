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

import java.net.Socket;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Builder;

/**
 * Holds all socket options settable via {@link Socket#setPerformancePreferences(int, int, int)}.
 * <p>
 * The {@link Socket#setPerformancePreferences(int, int, int)} API may not be implemented by a JRE.
 * </p>
 */
@Plugin(name = "SocketPerformancePreferences", category = Core.CATEGORY_NAME, printObject = true)
public class SocketPerformancePreferences implements Builder<SocketPerformancePreferences>, Cloneable {

    @PluginBuilderFactory
    public static SocketPerformancePreferences newBuilder() {
        return new SocketPerformancePreferences();
    }

    @PluginBuilderAttribute
    @Required
    private int bandwidth;

    @PluginBuilderAttribute
    @Required
    private int connectionTime;

    @PluginBuilderAttribute
    @Required
    private int latency;

    public void apply(final Socket socket) {
        socket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public SocketPerformancePreferences build() {
        try {
            return (SocketPerformancePreferences) clone();
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public int getConnectionTime() {
        return connectionTime;
    }

    public int getLatency() {
        return latency;
    }

    public void setBandwidth(final int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public void setConnectionTime(final int connectionTime) {
        this.connectionTime = connectionTime;
    }

    public void setLatency(final int latency) {
        this.latency = latency;
    }

    @Override
    public String toString() {
        return "SocketPerformancePreferences [bandwidth=" + bandwidth + ", connectionTime=" + connectionTime
                + ", latency=" + latency + "]";
    }
}
