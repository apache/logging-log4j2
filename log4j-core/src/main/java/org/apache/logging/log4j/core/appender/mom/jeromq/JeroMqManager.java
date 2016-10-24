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

package org.apache.logging.log4j.core.appender.mom.jeromq;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.zeromq.ZMQ;

/**
 * Manager for publishing messages via JeroMq.
 *
 * @since 2.6
 */
public class JeroMqManager extends AbstractManager {

    /**
     * System property to enable shutdown hook.
     */
    public static final String SYS_PROPERTY_ENABLE_SHUTDOWN_HOOK = "log4j.jeromq.enableShutdownHook";

    /**
     * System property to control JeroMQ I/O thread count.
     */
    public static final String SYS_PROPERTY_IO_THREADS = "log4j.jeromq.ioThreads";

    private static final JeroMqManagerFactory FACTORY = new JeroMqManagerFactory();
    private static final ZMQ.Context CONTEXT;

    static {
        LOGGER.trace("JeroMqManager using ZMQ version {}", ZMQ.getVersionString());

        final int ioThreads = PropertiesUtil.getProperties().getIntegerProperty(SYS_PROPERTY_IO_THREADS, 1);
        LOGGER.trace("JeroMqManager creating ZMQ context with ioThreads = {}", ioThreads);
        CONTEXT = ZMQ.context(ioThreads);

        final boolean enableShutdownHook = PropertiesUtil.getProperties().getBooleanProperty(
            SYS_PROPERTY_ENABLE_SHUTDOWN_HOOK, true);
        if (enableShutdownHook) {
            ((ShutdownCallbackRegistry) LogManager.getFactory()).addShutdownCallback(new Runnable() {
                @Override
                public void run() {
                    CONTEXT.close();
                }
            });
        }
    }

    private final ZMQ.Socket publisher;

    private JeroMqManager(final String name, final JeroMqConfiguration config) {
        super(null, name);
        publisher = CONTEXT.socket(ZMQ.PUB);
        publisher.setAffinity(config.affinity);
        publisher.setBacklog(config.backlog);
        publisher.setDelayAttachOnConnect(config.delayAttachOnConnect);
        if (config.identity != null) {
            publisher.setIdentity(config.identity);
        }
        publisher.setIPv4Only(config.ipv4Only);
        publisher.setLinger(config.linger);
        publisher.setMaxMsgSize(config.maxMsgSize);
        publisher.setRcvHWM(config.rcvHwm);
        publisher.setReceiveBufferSize(config.receiveBufferSize);
        publisher.setReceiveTimeOut(config.receiveTimeOut);
        publisher.setReconnectIVL(config.reconnectIVL);
        publisher.setReconnectIVLMax(config.reconnectIVLMax);
        publisher.setSendBufferSize(config.sendBufferSize);
        publisher.setSendTimeOut(config.sendTimeOut);
        publisher.setSndHWM(config.sndHwm);
        publisher.setTCPKeepAlive(config.tcpKeepAlive);
        publisher.setTCPKeepAliveCount(config.tcpKeepAliveCount);
        publisher.setTCPKeepAliveIdle(config.tcpKeepAliveIdle);
        publisher.setTCPKeepAliveInterval(config.tcpKeepAliveInterval);
        publisher.setXpubVerbose(config.xpubVerbose);
        for (final String endpoint : config.endpoints) {
            publisher.bind(endpoint);
        }
        LOGGER.debug("Created JeroMqManager with {}", config);
    }

    public boolean send(final byte[] data) {
        return publisher.send(data);
    }

    @Override
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        publisher.close();
        return true;
    }

    public static JeroMqManager getJeroMqManager(final String name, final long affinity, final long backlog,
                                                 final boolean delayAttachOnConnect, final byte[] identity,
                                                 final boolean ipv4Only, final long linger, final long maxMsgSize,
                                                 final long rcvHwm, final long receiveBufferSize,
                                                 final int receiveTimeOut, final long reconnectIVL,
                                                 final long reconnectIVLMax, final long sendBufferSize,
                                                 final int sendTimeOut, final long sndHwm, final int tcpKeepAlive,
                                                 final long tcpKeepAliveCount, final long tcpKeepAliveIdle,
                                                 final long tcpKeepAliveInterval, final boolean xpubVerbose,
                                                 final List<String> endpoints) {
        return getManager(name, FACTORY,
            new JeroMqConfiguration(affinity, backlog, delayAttachOnConnect, identity, ipv4Only, linger, maxMsgSize,
                rcvHwm, receiveBufferSize, receiveTimeOut, reconnectIVL, reconnectIVLMax, sendBufferSize, sendTimeOut,
                sndHwm, tcpKeepAlive, tcpKeepAliveCount, tcpKeepAliveIdle, tcpKeepAliveInterval, xpubVerbose,
                endpoints));
    }

    public static ZMQ.Context getContext() {
        return CONTEXT;
    }

    private static class JeroMqConfiguration {
        private final long affinity;
        private final long backlog;
        private final boolean delayAttachOnConnect;
        private final byte[] identity;
        private final boolean ipv4Only;
        private final long linger;
        private final long maxMsgSize;
        private final long rcvHwm;
        private final long receiveBufferSize;
        private final int receiveTimeOut;
        private final long reconnectIVL;
        private final long reconnectIVLMax;
        private final long sendBufferSize;
        private final int sendTimeOut;
        private final long sndHwm;
        private final int tcpKeepAlive;
        private final long tcpKeepAliveCount;
        private final long tcpKeepAliveIdle;
        private final long tcpKeepAliveInterval;
        private final boolean xpubVerbose;
        private final List<String> endpoints;

        private JeroMqConfiguration(final long affinity, final long backlog, final boolean delayAttachOnConnect,
                                    final byte[] identity, final boolean ipv4Only, final long linger,
                                    final long maxMsgSize, final long rcvHwm, final long receiveBufferSize,
                                    final int receiveTimeOut, final long reconnectIVL, final long reconnectIVLMax,
                                    final long sendBufferSize, final int sendTimeOut, final long sndHwm,
                                    final int tcpKeepAlive, final long tcpKeepAliveCount, final long tcpKeepAliveIdle,
                                    final long tcpKeepAliveInterval, final boolean xpubVerbose,
                                    final List<String> endpoints) {
            this.affinity = affinity;
            this.backlog = backlog;
            this.delayAttachOnConnect = delayAttachOnConnect;
            this.identity = identity;
            this.ipv4Only = ipv4Only;
            this.linger = linger;
            this.maxMsgSize = maxMsgSize;
            this.rcvHwm = rcvHwm;
            this.receiveBufferSize = receiveBufferSize;
            this.receiveTimeOut = receiveTimeOut;
            this.reconnectIVL = reconnectIVL;
            this.reconnectIVLMax = reconnectIVLMax;
            this.sendBufferSize = sendBufferSize;
            this.sendTimeOut = sendTimeOut;
            this.sndHwm = sndHwm;
            this.tcpKeepAlive = tcpKeepAlive;
            this.tcpKeepAliveCount = tcpKeepAliveCount;
            this.tcpKeepAliveIdle = tcpKeepAliveIdle;
            this.tcpKeepAliveInterval = tcpKeepAliveInterval;
            this.xpubVerbose = xpubVerbose;
            this.endpoints = endpoints;
        }

        @Override
        public String toString() {
            return "JeroMqConfiguration{" +
                "affinity=" + affinity +
                ", backlog=" + backlog +
                ", delayAttachOnConnect=" + delayAttachOnConnect +
                ", identity=" + Arrays.toString(identity) +
                ", ipv4Only=" + ipv4Only +
                ", linger=" + linger +
                ", maxMsgSize=" + maxMsgSize +
                ", rcvHwm=" + rcvHwm +
                ", receiveBufferSize=" + receiveBufferSize +
                ", receiveTimeOut=" + receiveTimeOut +
                ", reconnectIVL=" + reconnectIVL +
                ", reconnectIVLMax=" + reconnectIVLMax +
                ", sendBufferSize=" + sendBufferSize +
                ", sendTimeOut=" + sendTimeOut +
                ", sndHwm=" + sndHwm +
                ", tcpKeepAlive=" + tcpKeepAlive +
                ", tcpKeepAliveCount=" + tcpKeepAliveCount +
                ", tcpKeepAliveIdle=" + tcpKeepAliveIdle +
                ", tcpKeepAliveInterval=" + tcpKeepAliveInterval +
                ", xpubVerbose=" + xpubVerbose +
                ", endpoints=" + endpoints +
                '}';
        }
    }

    private static class JeroMqManagerFactory implements ManagerFactory<JeroMqManager, JeroMqConfiguration> {
        @Override
        public JeroMqManager createManager(final String name, final JeroMqConfiguration data) {
            return new JeroMqManager(name, data);
        }
    }
}
