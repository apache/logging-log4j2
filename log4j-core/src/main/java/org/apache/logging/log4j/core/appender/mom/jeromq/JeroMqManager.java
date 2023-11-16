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
package org.apache.logging.log4j.core.appender.mom.jeromq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMonitor;
import org.zeromq.ZMonitor.Event;
import org.zeromq.ZMonitor.ZEvent;

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
    private static final ZContext CONTEXT;

    // Retained to avoid garbage collection of the hook
    private static final Cancellable SHUTDOWN_HOOK;

    static {
        LOGGER.trace("JeroMqManager using ZMQ version {}", ZMQ.getVersionString());

        final int ioThreads = PropertiesUtil.getProperties().getIntegerProperty(SYS_PROPERTY_IO_THREADS, 1);
        LOGGER.trace("JeroMqManager creating ZMQ context with ioThreads = {}", ioThreads);
        CONTEXT = new ZContext(ioThreads);

        final boolean enableShutdownHook =
                PropertiesUtil.getProperties().getBooleanProperty(SYS_PROPERTY_ENABLE_SHUTDOWN_HOOK, true);
        if (enableShutdownHook && LogManager.getFactory() instanceof ShutdownCallbackRegistry) {
            SHUTDOWN_HOOK = ((ShutdownCallbackRegistry) LogManager.getFactory()).addShutdownCallback(CONTEXT::close);
        } else {
            SHUTDOWN_HOOK = null;
        }
    }

    private final ZMQ.Socket publisher;
    private final List<String> endpoints;

    private JeroMqManager(final String name, final JeroMqConfiguration config) {
        super(null, name);
        publisher = CONTEXT.createSocket(SocketType.PUB);
        final ZMonitor monitor = new ZMonitor(CONTEXT, publisher);
        monitor.add(Event.LISTENING);
        monitor.start();
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
        final List<String> endpoints = new ArrayList<String>(config.endpoints.size());
        for (final String endpoint : config.endpoints) {
            publisher.bind(endpoint);
            // Retrieve the standardized list of endpoints,
            // this also converts port 0 to an ephemeral port.
            final ZEvent event = monitor.nextEvent();
            endpoints.add(event.address);
        }
        this.endpoints = Collections.unmodifiableList(endpoints);
        monitor.destroy();
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

    // not public, handy for testing
    Socket getSocket() {
        return publisher;
    }

    public List<String> getEndpoints() {
        return endpoints;
    }

    public static JeroMqManager getJeroMqManager(
            final String name,
            final long affinity,
            final long backlog,
            final boolean delayAttachOnConnect,
            final byte[] identity,
            final boolean ipv4Only,
            final long linger,
            final long maxMsgSize,
            final long rcvHwm,
            final long receiveBufferSize,
            final int receiveTimeOut,
            final long reconnectIVL,
            final long reconnectIVLMax,
            final long sendBufferSize,
            final int sendTimeOut,
            final long sndHwm,
            final int tcpKeepAlive,
            final long tcpKeepAliveCount,
            final long tcpKeepAliveIdle,
            final long tcpKeepAliveInterval,
            final boolean xpubVerbose,
            final List<String> endpoints) {
        return getManager(
                name,
                FACTORY,
                new JeroMqConfiguration(
                        affinity,
                        backlog,
                        delayAttachOnConnect,
                        identity,
                        ipv4Only,
                        linger,
                        maxMsgSize,
                        rcvHwm,
                        receiveBufferSize,
                        receiveTimeOut,
                        reconnectIVL,
                        reconnectIVLMax,
                        sendBufferSize,
                        sendTimeOut,
                        sndHwm,
                        tcpKeepAlive,
                        tcpKeepAliveCount,
                        tcpKeepAliveIdle,
                        tcpKeepAliveInterval,
                        xpubVerbose,
                        endpoints));
    }

    public static ZMQ.Context getContext() {
        return CONTEXT.getContext();
    }

    public static ZContext getZContext() {
        return CONTEXT;
    }

    private static final class JeroMqConfiguration {
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

        private JeroMqConfiguration(
                final long affinity,
                final long backlog,
                final boolean delayAttachOnConnect,
                final byte[] identity,
                final boolean ipv4Only,
                final long linger,
                final long maxMsgSize,
                final long rcvHwm,
                final long receiveBufferSize,
                final int receiveTimeOut,
                final long reconnectIVL,
                final long reconnectIVLMax,
                final long sendBufferSize,
                final int sendTimeOut,
                final long sndHwm,
                final int tcpKeepAlive,
                final long tcpKeepAliveCount,
                final long tcpKeepAliveIdle,
                final long tcpKeepAliveInterval,
                final boolean xpubVerbose,
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
            return "JeroMqConfiguration{" + "affinity="
                    + affinity + ", backlog="
                    + backlog + ", delayAttachOnConnect="
                    + delayAttachOnConnect + ", identity="
                    + Arrays.toString(identity) + ", ipv4Only="
                    + ipv4Only + ", linger="
                    + linger + ", maxMsgSize="
                    + maxMsgSize + ", rcvHwm="
                    + rcvHwm + ", receiveBufferSize="
                    + receiveBufferSize + ", receiveTimeOut="
                    + receiveTimeOut + ", reconnectIVL="
                    + reconnectIVL + ", reconnectIVLMax="
                    + reconnectIVLMax + ", sendBufferSize="
                    + sendBufferSize + ", sendTimeOut="
                    + sendTimeOut + ", sndHwm="
                    + sndHwm + ", tcpKeepAlive="
                    + tcpKeepAlive + ", tcpKeepAliveCount="
                    + tcpKeepAliveCount + ", tcpKeepAliveIdle="
                    + tcpKeepAliveIdle + ", tcpKeepAliveInterval="
                    + tcpKeepAliveInterval + ", xpubVerbose="
                    + xpubVerbose + ", endpoints="
                    + endpoints + '}';
        }
    }

    private static class JeroMqManagerFactory implements ManagerFactory<JeroMqManager, JeroMqConfiguration> {
        @Override
        public JeroMqManager createManager(final String name, final JeroMqConfiguration data) {
            return new JeroMqManager(name, data);
        }
    }
}
