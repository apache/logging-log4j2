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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

/**
 * Sends log events to one or more ZeroMQ (JeroMQ) endpoints.
 * <p>
 * Requires the JeroMQ jar (LGPL as of 0.3.5)
 * </p>
 */
// TODO
// Some methods are synchronized because a ZMQ.Socket is not thread-safe
// Using a ThreadLocal for the publisher hangs tests on shutdown. There must be
// some issue on threads owning certain resources as opposed to others.
@Plugin(name = "JeroMQ", category = "Core", elementType = "appender", printObject = true)
public final class JeroMqAppender extends AbstractAppender {

    /**
     * System property to enable shutdown hook.
     */
    static final String SYS_PROPERTY_ENABLE_SHUTDOWN_HOOK = "log4j.jeromq.enableShutdownHook";

    /**
     * System property to control JeroMQ I/O thread count.
     */
    static final String SYS_PROPERTY_IO_THREADS = "log4j.jeromq.ioThreads";

    // Per ZMQ docs, there should usually only be one ZMQ context per process.
    private static volatile ZMQ.Context context;

    private static final int DEFAULT_BACKLOG = 100;

    private static final int DEFAULT_IVL = 100;

    private static final int DEFAULT_RCV_HWM = 1000;

    private static final int DEFAULT_SND_HWM = 1000;

    private static Logger logger;

    // ZMQ sockets are not thread safe.
    private static ZMQ.Socket publisher;

    private static final String SIMPLE_NAME = JeroMqAppender.class.getSimpleName();

    static {
        logger = StatusLogger.getLogger();
        final PropertiesUtil managerProps = PropertiesUtil.getProperties();
        final int ioThreads = managerProps.getIntegerProperty(SYS_PROPERTY_IO_THREADS, 1);
        final boolean enableShutdownHook = managerProps.getBooleanProperty(SYS_PROPERTY_ENABLE_SHUTDOWN_HOOK, true);
        final String simpleName = SIMPLE_NAME;
        logger.trace("{} using ZMQ version {}", simpleName, ZMQ.getVersionString());
        logger.trace("{} creating ZMQ context with ioThreads={}", simpleName, ioThreads);
        context = ZMQ.context(ioThreads);
        logger.trace("{} created ZMQ context {}", simpleName, context);
        if (enableShutdownHook) {
            final Thread hook = new Log4jThread(simpleName + "-shutdown") {
                @Override
                public void run() {
                    shutdown();
                }
            };
            logger.trace("{} adding shutdown hook {}", simpleName, hook);
            Runtime.getRuntime().addShutdownHook(hook);
        }
    }

    private final long affinity;
    private final long backlog;
    private final boolean delayAttachOnConnect;
    private final List<String> endpoints;
    private final byte[] identity;
    private final int ioThreads = 1;
    private final boolean ipv4Only;
    private final long linger;
    private final long maxMsgSize;
    private final long rcvHwm;
    private final long receiveBufferSize;
    private final int receiveTimeOut;
    private final long reconnectIVL;
    private final long reconnectIVLMax;
    private final long sendBufferSize;
    private int sendRcFalse;
    private int sendRcTrue;
    private final int sendTimeOut;
    private final long sndHwm;
    private final int tcpKeepAlive;
    private final long tcpKeepAliveCount;
    private final long tcpKeepAliveIdle;
    private final long tcpKeepAliveInterval;
    private final boolean xpubVerbose;

    private JeroMqAppender(final String name, final Filter filter, final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions, final List<String> endpoints, final long affinity, final long backlog,
            final boolean delayAttachOnConnect, final byte[] identity, final boolean ipv4Only, final long linger,
            final long maxMsgSize, final long rcvHwm, final long receiveBufferSize, final int receiveTimeOut,
            final long reconnectIVL, final long reconnectIVLMax, final long sendBufferSize, final int sendTimeOut,
            final long sndHWM, final int tcpKeepAlive, final long tcpKeepAliveCount, final long tcpKeepAliveIdle,
            final long tcpKeepAliveInterval, final boolean xpubVerbose) {
        super(name, filter, layout, ignoreExceptions);
        this.endpoints = endpoints;
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
        this.sndHwm = sndHWM;
        this.tcpKeepAlive = tcpKeepAlive;
        this.tcpKeepAliveCount = tcpKeepAliveCount;
        this.tcpKeepAliveIdle = tcpKeepAliveIdle;
        this.tcpKeepAliveInterval = tcpKeepAliveInterval;
        this.xpubVerbose = xpubVerbose;
    }

    // The ZMQ.Socket class has other set methods that we do not cover because
    // they throw unsupported operation exceptions.
    @PluginFactory
    public static JeroMqAppender createAppender(
            // @formatter:off
            @Required(message = "No name provided for JeroMqAppender") @PluginAttribute("name") final String name,
            @PluginElement("Layout") Layout<?> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginElement("Properties") final Property[] properties,
            // Super attributes
            @PluginAttribute("ignoreExceptions") final boolean ignoreExceptions,
            // ZMQ attributes; defaults picked from zmq.Options.
            @PluginAttribute(value = "affinity", defaultLong = 0) final long affinity,
            @PluginAttribute(value = "backlog", defaultLong = DEFAULT_BACKLOG) final long backlog,
            @PluginAttribute(value = "delayAttachOnConnect", defaultBoolean = false) final boolean delayAttachOnConnect,
            @PluginAttribute(value = "identity") final byte[] identity,
            @PluginAttribute(value = "ipv4Only", defaultBoolean = true) final boolean ipv4Only,
            @PluginAttribute(value = "linger", defaultLong = -1) final long linger,
            @PluginAttribute(value = "maxMsgSize", defaultLong = -1) final long maxMsgSize,
            @PluginAttribute(value = "rcvHwm", defaultLong = DEFAULT_RCV_HWM) final long rcvHwm,
            @PluginAttribute(value = "receiveBufferSize", defaultLong = 0) final long receiveBufferSize,
            @PluginAttribute(value = "receiveTimeOut", defaultLong = -1) final int receiveTimeOut,
            @PluginAttribute(value = "reconnectIVL", defaultLong = DEFAULT_IVL) final long reconnectIVL,
            @PluginAttribute(value = "reconnectIVLMax", defaultLong = 0) final long reconnectIVLMax,
            @PluginAttribute(value = "sendBufferSize", defaultLong = 0) final long sendBufferSize,
            @PluginAttribute(value = "sendTimeOut", defaultLong = -1) final int sendTimeOut,
            @PluginAttribute(value = "sndHwm", defaultLong = DEFAULT_SND_HWM) final long sndHwm,
            @PluginAttribute(value = "tcpKeepAlive", defaultInt = -1) final int tcpKeepAlive,
            @PluginAttribute(value = "tcpKeepAliveCount", defaultLong = -1) final long tcpKeepAliveCount,
            @PluginAttribute(value = "tcpKeepAliveIdle", defaultLong = -1) final long tcpKeepAliveIdle,
            @PluginAttribute(value = "tcpKeepAliveInterval", defaultLong = -1) final long tcpKeepAliveInterval,
            @PluginAttribute(value = "xpubVerbose", defaultBoolean = false) final boolean xpubVerbose
            // @formatter:on
    ) {
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        List<String> endpoints;
        if (properties == null) {
            endpoints = new ArrayList<>(0);
        } else {
            endpoints = new ArrayList<>(properties.length);
            for (final Property property : properties) {
                if ("endpoint".equalsIgnoreCase(property.getName())) {
                    final String value = property.getValue();
                    if (Strings.isNotEmpty(value)) {
                        endpoints.add(value);
                    }
                }
            }
        }
        logger.debug("Creating JeroMqAppender with name={}, filter={}, layout={}, ignoreExceptions={}, endpoints={}",
                name, filter, layout, ignoreExceptions, endpoints);
        return new JeroMqAppender(name, filter, layout, ignoreExceptions, endpoints, affinity, backlog,
                delayAttachOnConnect, identity, ipv4Only, linger, maxMsgSize, rcvHwm, receiveBufferSize,
                receiveTimeOut, reconnectIVL, reconnectIVLMax, sendBufferSize, sendTimeOut, sndHwm, tcpKeepAlive,
                tcpKeepAliveCount, tcpKeepAliveIdle, tcpKeepAliveInterval, xpubVerbose);
    }

    static ZMQ.Context getContext() {
        return context;
    }

    private static ZMQ.Socket getPublisher() {
        return publisher;
    }

    private static ZMQ.Socket newPublisher() {
        logger.trace("{} creating a new ZMQ PUB socket with context {}", SIMPLE_NAME, context);
        final Socket socketPub = context.socket(ZMQ.PUB);
        logger.trace("{} created new ZMQ PUB socket {}", SIMPLE_NAME, socketPub);
        return socketPub;
    }

    static void shutdown() {
        if (context != null) {
            logger.trace("{} terminating JeroMQ context {}", SIMPLE_NAME, context);
            context.term();
            context = null;
        }
    }

    @Override
    public synchronized void append(final LogEvent event) {
        final String formattedMessage = event.getMessage().getFormattedMessage();
        if (getPublisher().send(formattedMessage, 0)) {
            sendRcTrue++;
        } else {
            sendRcFalse++;
            logger.error("Appender {} could not send message {} to JeroMQ {}", getName(), sendRcFalse, formattedMessage);
        }
    }

    // not public, handy for testing
    int getSendRcFalse() {
        return sendRcFalse;
    }

    // not public, handy for testing
    int getSendRcTrue() {
        return sendRcTrue;
    }

    // not public, handy for testing
    void resetSendRcs() {
        sendRcTrue = sendRcFalse = 0;
    }

    @Override
    public synchronized void start() {
        super.start();
        publisher = newPublisher();
        final String name = getName();
        final String prefix = "JeroMQ Appender";
        logger.debug("Starting {} {} using ZMQ version {}", prefix, name, ZMQ.getVersionString());
        logger.debug("{} {} context {} with ioThreads={}", prefix, name, context, ioThreads);
        //
        final ZMQ.Socket socketPub = getPublisher();
        logger.trace("{} {} setting {} publisher properties for instance {}", prefix, name, socketPub.getClass()
                .getName(), socketPub);
        logger.trace("{} {} publisher setAffinity({})", prefix, name, affinity);
        socketPub.setAffinity(affinity);
        logger.trace("{} {} publisher setBacklog({})", prefix, name, backlog);
        socketPub.setBacklog(backlog);
        logger.trace("{} {} publisher setDelayAttachOnConnect({})", prefix, name, delayAttachOnConnect);
        socketPub.setDelayAttachOnConnect(delayAttachOnConnect);
        if (identity != null) {
            logger.trace("{} {} publisher setIdentity({})", prefix, name, Arrays.toString(identity));
            socketPub.setIdentity(identity);
        }
        logger.trace("{} {} publisher setIPv4Only({})", prefix, name, ipv4Only);
        socketPub.setIPv4Only(ipv4Only);
        logger.trace("{} {} publisher setLinger({})", prefix, name, linger);
        socketPub.setLinger(linger);
        logger.trace("{} {} publisher setMaxMsgSize({})", prefix, name, maxMsgSize);
        socketPub.setMaxMsgSize(maxMsgSize);
        logger.trace("{} {} publisher setRcvHWM({})", prefix, name, rcvHwm);
        socketPub.setRcvHWM(rcvHwm);
        logger.trace("{} {} publisher setReceiveBufferSize({})", prefix, name, receiveBufferSize);
        socketPub.setReceiveBufferSize(receiveBufferSize);
        logger.trace("{} {} publisher setReceiveTimeOut({})", prefix, name, receiveTimeOut);
        socketPub.setReceiveTimeOut(receiveTimeOut);
        logger.trace("{} {} publisher setReconnectIVL({})", prefix, name, reconnectIVL);
        socketPub.setReconnectIVL(reconnectIVL);
        logger.trace("{} {} publisher setReconnectIVLMax({})", prefix, name, reconnectIVLMax);
        socketPub.setReconnectIVLMax(reconnectIVLMax);
        logger.trace("{} {} publisher setSendBufferSize({})", prefix, name, sendBufferSize);
        socketPub.setSendBufferSize(sendBufferSize);
        logger.trace("{} {} publisher setSendTimeOut({})", prefix, name, sendTimeOut);
        socketPub.setSendTimeOut(sendTimeOut);
        logger.trace("{} {} publisher setSndHWM({})", prefix, name, sndHwm);
        socketPub.setSndHWM(sndHwm);
        logger.trace("{} {} publisher setTCPKeepAlive({})", prefix, name, tcpKeepAlive);
        socketPub.setTCPKeepAlive(tcpKeepAlive);
        logger.trace("{} {} publisher setTCPKeepAliveCount({})", prefix, name, tcpKeepAliveCount);
        socketPub.setTCPKeepAliveCount(tcpKeepAliveCount);
        logger.trace("{} {} publisher setTCPKeepAliveIdle({})", prefix, name, tcpKeepAliveIdle);
        socketPub.setTCPKeepAliveIdle(tcpKeepAliveIdle);
        logger.trace("{} {} publisher setTCPKeepAliveInterval({})", prefix, name, tcpKeepAliveInterval);
        socketPub.setTCPKeepAliveInterval(tcpKeepAliveInterval);
        logger.trace("{} {} publisher setXpubVerbose({})", prefix, name, xpubVerbose);
        socketPub.setXpubVerbose(xpubVerbose);
        //
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Created JeroMQ {} publisher {} type {}, affinity={}, backlog={}, delayAttachOnConnect={}, events={}, IPv4Only={}, linger={}, maxMsgSize={}, multicastHops={}, "
                            + "rate={}, rcvHWM={}, receiveBufferSize={}, receiveTimeOut={}, reconnectIVL={}, reconnectIVLMax={}, recoveryInterval={}, sendBufferSize={}, "
                            + "sendTimeOut={}, sndHWM={}, TCPKeepAlive={}, TCPKeepAliveCount={}, TCPKeepAliveIdle={}, TCPKeepAliveInterval={}, TCPKeepAliveSetting={}",
                    name, socketPub, socketPub.getType(), socketPub.getAffinity(), socketPub.getBacklog(),
                    socketPub.getDelayAttachOnConnect(), socketPub.getEvents(), socketPub.getIPv4Only(),
                    socketPub.getLinger(), socketPub.getMaxMsgSize(), socketPub.getMulticastHops(),
                    socketPub.getRate(), socketPub.getRcvHWM(), socketPub.getReceiveBufferSize(),
                    socketPub.getReceiveTimeOut(), socketPub.getReconnectIVL(), socketPub.getReconnectIVLMax(),
                    socketPub.getRecoveryInterval(), socketPub.getSendBufferSize(), socketPub.getSendTimeOut(),
                    socketPub.getSndHWM(), socketPub.getTCPKeepAlive(), socketPub.getTCPKeepAliveCount(),
                    socketPub.getTCPKeepAliveIdle(), socketPub.getTCPKeepAliveInterval(),
                    socketPub.getTCPKeepAliveSetting());
        }
        for (final String endpoint : endpoints) {
            logger.debug("Binding {} appender {} to endpoint {}", SIMPLE_NAME, name, endpoint);
            socketPub.bind(endpoint);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
        final Socket socketPub = getPublisher();
        if (socketPub != null) {
            logger.debug("Closing {} appender {} publisher {}", SIMPLE_NAME, getName(), socketPub);
            socketPub.close();
            publisher = null;
        }
    }

    @Override
    public String toString() {
        return "JeroMqAppender [context=" + context + ", publisher=" + publisher + ", endpoints=" + endpoints + "]";
    }

}
