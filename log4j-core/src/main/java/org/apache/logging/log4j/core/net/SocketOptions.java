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

import java.net.Socket;
import java.net.SocketException;

import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.util.Builder;

/**
 * Holds all socket options settable via {@link Socket} methods.
 */
@Plugin(name = "SocketOptions", category = Core.CATEGORY_NAME, printObject = true)
public class SocketOptions implements Builder<SocketOptions>, Cloneable {

    @PluginBuilderFactory
    public static SocketOptions newBuilder() {
        return new SocketOptions();
    }

    @PluginBuilderAttribute
    private Boolean keepAlive;

    @PluginBuilderAttribute
    private Boolean oobInline;

    @PluginElement("PerformancePreferences")
    private SocketPerformancePreferences performancePreferences;

    @PluginBuilderAttribute
    private Integer receiveBufferSize;

    @PluginBuilderAttribute
    private Boolean reuseAddress;

    @PluginBuilderAttribute
    private Rfc1349TrafficClass rfc1349TrafficClass;

    @PluginBuilderAttribute
    private Integer sendBufferSize;

    @PluginBuilderAttribute
    private Integer soLinger;

    @PluginBuilderAttribute
    private Integer soTimeout;

    @PluginBuilderAttribute
    private Boolean tcpNoDelay;

    @PluginBuilderAttribute
    private Integer trafficClass;

    public void apply(final Socket socket) throws SocketException {
        if (keepAlive != null) {
            socket.setKeepAlive(keepAlive.booleanValue());
        }
        if (oobInline != null) {
            socket.setOOBInline(oobInline.booleanValue());
        }
        if (reuseAddress != null) {
            socket.setReuseAddress(reuseAddress.booleanValue());
        }
        if (performancePreferences != null) {
            performancePreferences.apply(socket);
        }
        if (receiveBufferSize != null) {
            socket.setReceiveBufferSize(receiveBufferSize.intValue());
        }
        if (soLinger != null) {
            socket.setSoLinger(true, soLinger.intValue());
        }
        if (soTimeout != null) {
            socket.setSoTimeout(soTimeout.intValue());
        }
        if (tcpNoDelay != null) {
            socket.setTcpNoDelay(tcpNoDelay.booleanValue());
        }
        final Integer actualTrafficClass = getActualTrafficClass();
        if (actualTrafficClass != null) {
            socket.setTrafficClass(actualTrafficClass);
        }
    }

    @Override
    public SocketOptions build() {
        try {
            return (SocketOptions) clone();
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public Integer getActualTrafficClass() {
        if (trafficClass != null && rfc1349TrafficClass != null) {
            throw new IllegalStateException("You MUST not set both customTrafficClass and trafficClass.");
        }
        if (trafficClass != null) {
            return trafficClass;
        }
        if (rfc1349TrafficClass != null) {
            return Integer.valueOf(rfc1349TrafficClass.value());
        }
        return null;
    }

    public SocketPerformancePreferences getPerformancePreferences() {
        return performancePreferences;
    }

    public Integer getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public Rfc1349TrafficClass getRfc1349TrafficClass() {
        return rfc1349TrafficClass;
    }

    public Integer getSendBufferSize() {
        return sendBufferSize;
    }

    public Integer getSoLinger() {
        return soLinger;
    }

    public Integer getSoTimeout() {
        return soTimeout;
    }

    public Integer getTrafficClass() {
        return trafficClass;
    }

    public Boolean isKeepAlive() {
        return keepAlive;
    }

    public Boolean isOobInline() {
        return oobInline;
    }

    public Boolean isReuseAddress() {
        return reuseAddress;
    }

    public Boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public SocketOptions setKeepAlive(final boolean keepAlive) {
        this.keepAlive = Boolean.valueOf(keepAlive);
        return this;
    }

    public SocketOptions setOobInline(final boolean oobInline) {
        this.oobInline = Boolean.valueOf(oobInline);
        return this;
    }

    public SocketOptions setPerformancePreferences(final SocketPerformancePreferences performancePreferences) {
        this.performancePreferences = performancePreferences;
        return this;
    }

    public SocketOptions setReceiveBufferSize(final int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
        return this;
    }

    public SocketOptions setReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = Boolean.valueOf(reuseAddress);
        return this;
    }

    public SocketOptions setRfc1349TrafficClass(final Rfc1349TrafficClass trafficClass) {
        this.rfc1349TrafficClass = trafficClass;
        return this;
    }

    public SocketOptions setSendBufferSize(final int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    public SocketOptions setSoLinger(final int soLinger) {
        this.soLinger = soLinger;
        return this;
    }

    public SocketOptions setSoTimeout(final int soTimeout) {
        this.soTimeout = soTimeout;
        return this;
    }

    public SocketOptions setTcpNoDelay(final boolean tcpNoDelay) {
        this.tcpNoDelay = Boolean.valueOf(tcpNoDelay);
        return this;
    }

    public SocketOptions setTrafficClass(final int trafficClass) {
        this.trafficClass = trafficClass;
        return this;
    }

    @Override
    public String toString() {
        return "SocketOptions [keepAlive=" + keepAlive + ", oobInline=" + oobInline + ", performancePreferences="
                + performancePreferences + ", receiveBufferSize=" + receiveBufferSize + ", reuseAddress=" + reuseAddress
                + ", rfc1349TrafficClass=" + rfc1349TrafficClass + ", sendBufferSize=" + sendBufferSize + ", soLinger="
                + soLinger + ", soTimeout=" + soTimeout + ", tcpNoDelay=" + tcpNoDelay + ", trafficClass="
                + trafficClass + "]";
    }
}
