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

    /**
     * Constructs a new builder.
     *
     * @return a new builder.
     */
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

    /**
     * Applies the values in this builder to the given socket.
     *
     * @param socket The target Socket.
     * @throws SocketException if there is an error in the underlying protocol, such as a TCP error.
     */
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

    /**
     * See {@link Socket#setTrafficClass(int)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
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

    /**
     * See {@link Socket#setPerformancePreferences(int, int, int)}.
     *
     * @return this.
     */
    public SocketPerformancePreferences getPerformancePreferences() {
        return performancePreferences;
    }

    /**
     * See {@link Socket#setReceiveBufferSize(int)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Integer getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * See {@link Socket#setTrafficClass(int)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Rfc1349TrafficClass getRfc1349TrafficClass() {
        return rfc1349TrafficClass;
    }

    /**
     * See {@link Socket#setSendBufferSize(int)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Integer getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * See {@link Socket#setSoLinger(boolean, int)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Integer getSoLinger() {
        return soLinger;
    }

    /**
     * See {@link Socket#setSoTimeout(int)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Integer getSoTimeout() {
        return soTimeout;
    }

    /**
     * See {@link Socket#setTrafficClass(int)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Integer getTrafficClass() {
        return trafficClass;
    }

    /**
     * See {@link Socket#setKeepAlive(boolean)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * See {@link Socket#setOOBInline(boolean)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Boolean isOobInline() {
        return oobInline;
    }

    /**
     * See {@link Socket#setReuseAddress(boolean)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Boolean isReuseAddress() {
        return reuseAddress;
    }

    /**
     * See {@link Socket#setTcpNoDelay(boolean)}.
     *
     * @return the value to apply to a {@link Socket}.
     */
    public Boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * See {@link Socket#setKeepAlive(boolean)}.
     *
     * @param keepAlive See {@link Socket#setKeepAlive(boolean)}.
     * @return this.
     */
    public SocketOptions setKeepAlive(final boolean keepAlive) {
        this.keepAlive = Boolean.valueOf(keepAlive);
        return this;
    }

    /**
     * See {@link Socket#setOOBInline(boolean)}.
     *
     * @param oobInline See {@link Socket#setOOBInline(boolean)}.
     * @return this.
     */
    public SocketOptions setOobInline(final boolean oobInline) {
        this.oobInline = Boolean.valueOf(oobInline);
        return this;
    }

    /**
     * See {@link Socket#setPerformancePreferences(int, int, int)}.
     *
     * @param performancePreferences See {@link Socket#setPerformancePreferences(int, int, int)}.
     * @return this.
     */
    public SocketOptions setPerformancePreferences(final SocketPerformancePreferences performancePreferences) {
        this.performancePreferences = performancePreferences;
        return this;
    }

    /**
     * See {@link Socket#setReceiveBufferSize(int)}.
     *
     * @param receiveBufferSize See {@link Socket#setReceiveBufferSize(int)}.
     * @return this.
     */
    public SocketOptions setReceiveBufferSize(final int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
        return this;
    }

    /**
     * See {@link Socket#setReuseAddress(boolean)}.
     *
     * @param reuseAddress See {@link Socket#setReuseAddress(boolean)}.
     * @return this.
     */
    public SocketOptions setReuseAddress(final boolean reuseAddress) {
        this.reuseAddress = Boolean.valueOf(reuseAddress);
        return this;
    }

    /**
     * See {@link Socket#setTrafficClass(int)}.
     *
     * @param trafficClass See {@link Socket#setTrafficClass(int)}.
     * @return the value to apply to a {@link Socket}.
     */
    public SocketOptions setRfc1349TrafficClass(final Rfc1349TrafficClass trafficClass) {
        this.rfc1349TrafficClass = trafficClass;
        return this;
    }

    /**
     * See {@link Socket#setSendBufferSize(int)}.
     *
     * @param sendBufferSize See {@link Socket#setSendBufferSize(int)}.
     * @return this.
     */
    public SocketOptions setSendBufferSize(final int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
        return this;
    }

    /**
     * See {@link Socket#setSoLinger(boolean, int)}.
     *
     * @param soLinger See {@link Socket#setSoLinger(boolean, int)}.
     * @return this.
     */
    public SocketOptions setSoLinger(final int soLinger) {
        this.soLinger = soLinger;
        return this;
    }

    /**
     * See {@link Socket#setSoTimeout(int)}.
     *
     * @param soTimeout See {@link Socket#setSoTimeout(int)}.
     * @return this.
     */
    public SocketOptions setSoTimeout(final int soTimeout) {
        this.soTimeout = soTimeout;
        return this;
    }

    /**
     * See {@link Socket#setTcpNoDelay(boolean)}.
     *
     * @param tcpNoDelay See {@link Socket#setTcpNoDelay(boolean)}.
     * @return this.
     */
    public SocketOptions setTcpNoDelay(final boolean tcpNoDelay) {
        this.tcpNoDelay = Boolean.valueOf(tcpNoDelay);
        return this;
    }

    /**
     * See {@link Socket#setTrafficClass(int)}.
     *
     * @param trafficClass See {@link Socket#setTrafficClass(int)}.
     * @return this.
     */
    public SocketOptions setTrafficClass(final int trafficClass) {
        this.trafficClass = trafficClass;
        return this;
    }

    @Override
    public String toString() {
        return "SocketOptions [keepAlive=" + keepAlive + ", oobInline=" + oobInline + ", performancePreferences="
                + performancePreferences
                + ", receiveBufferSize=" + receiveBufferSize + ", reuseAddress=" + reuseAddress
                + ", rfc1349TrafficClass=" + rfc1349TrafficClass
                + ", sendBufferSize=" + sendBufferSize + ", soLinger=" + soLinger + ", soTimeout=" + soTimeout
                + ", tcpNoDelay=" + tcpNoDelay + ", trafficClass="
                + trafficClass + "]";
    }
}
