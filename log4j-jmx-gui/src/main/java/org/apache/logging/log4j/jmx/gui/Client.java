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
package org.apache.logging.log4j.jmx.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.logging.log4j.core.jmx.LoggerContextAdminMBean;
import org.apache.logging.log4j.core.jmx.Server;
import org.apache.logging.log4j.core.jmx.StatusLoggerAdminMBean;
import org.apache.logging.log4j.core.util.Closer;

/**
 * This class allows client-side code to perform operations on remote
 * (server-side) MBeans via proxies.
 */
public class Client {
    private JMXConnector connector;
    private final MBeanServerConnection connection;

    /**
     * Constructs a new {@code Client} object and creates proxies for all known
     * remote MBeans.
     *
     * @param connector used to create the MBean server connection through which
     *            to communicate with the remote mbeans
     * @throws MalformedObjectNameException if a problem occurred identifying
     *             one of the remote mbeans
     * @throws IOException if the connection failed
     */
    public Client(final JMXConnector connector) throws MalformedObjectNameException, IOException {
        this.connector = Objects.requireNonNull(connector, "JMXConnector");
        this.connector.connect();
        this.connection = connector.getMBeanServerConnection();
        init();
    }

    /**
     * Constructs a new {@code Client} object and creates proxies for all known
     * remote MBeans.
     *
     * @param mBeanServerConnection the MBean server connection through which to
     *            communicate with the remote mbeans
     * @throws MalformedObjectNameException if a problem occurred identifying
     *             one of the remote mbeans
     * @throws IOException if the connection failed
     */
    public Client(final MBeanServerConnection mBeanServerConnection) throws MalformedObjectNameException, IOException {
        this.connection = mBeanServerConnection;
        init();
    }

    private void init() throws MalformedObjectNameException, IOException {
    }

    private Set<ObjectName> find(final String pattern) throws JMException, IOException {
        final ObjectName search = new ObjectName(String.format(pattern, "*"));
        final Set<ObjectName> result = connection.queryNames(search, null);
        return result;
    }

    /**
     * Returns a list of proxies that allow operations to be performed on the
     * remote {@code LoggerContextAdminMBean}s.
     *
     * @return a list of proxies to the remote {@code LoggerContextAdminMBean}s
     * @throws IOException If an I/O error occurred
     * @throws JMException If a management error occurred
     */
    public List<LoggerContextAdminMBean> getLoggerContextAdmins() throws JMException, IOException {
        final List<LoggerContextAdminMBean> result = new ArrayList<>();
        final Set<ObjectName> contextNames = find(LoggerContextAdminMBean.PATTERN);
        for (final ObjectName contextName : contextNames) {
            result.add(getLoggerContextAdmin(contextName));
        }
        return result;
    }

    public LoggerContextAdminMBean getLoggerContextAdmin(final ObjectName name) {
        final LoggerContextAdminMBean ctx = JMX.newMBeanProxy(connection, //
                name, //
                LoggerContextAdminMBean.class, false);
        return ctx;
    }

    /**
     * Closes the client connection to its server. Any ongoing or new requests
     * to the MBeanServerConnection will fail.
     */
    public void close() {
        Closer.closeSilently(connector);
    }

    /**
     * Returns the MBean server connection through which to communicate with the
     * remote mbeans.
     *
     * @return the MBean server connection
     */
    public MBeanServerConnection getConnection() {
        return connection;
    }

    /**
     * Returns the {@code StatusLoggerAdminMBean} associated with the specified
     * context name, or {@code null}.
     *
     * @param contextName search key
     * @return StatusLoggerAdminMBean or null
     * @throws MalformedObjectNameException If an object name is malformed
     * @throws IOException If an I/O error occurred
     */
    public StatusLoggerAdminMBean getStatusLoggerAdmin(final String contextName)
            throws MalformedObjectNameException, IOException {
        final String pattern = StatusLoggerAdminMBean.PATTERN;
        final String mbean = String.format(pattern, Server.escape(contextName));
        final ObjectName search = new ObjectName(mbean);
        final Set<ObjectName> result = connection.queryNames(search, null);
        if (result.size() == 0) {
            return null;
        }
        if (result.size() > 1) {
            System.err.println("WARN: multiple status loggers found for " + contextName + ": " + result);
        }
        final StatusLoggerAdminMBean proxy = JMX.newMBeanProxy(connection, //
                result.iterator().next(), //
                StatusLoggerAdminMBean.class, true); // notificationBroadcaster
        return proxy;
    }

    /**
     * Returns {@code true} if the specified {@code ObjectName} is for a
     * {@code LoggerContextAdminMBean}, {@code false} otherwise.
     *
     * @param mbeanName the {@code ObjectName} to check.
     * @return {@code true} if the specified {@code ObjectName} is for a
     *         {@code LoggerContextAdminMBean}, {@code false} otherwise
     */
    public boolean isLoggerContext(final ObjectName mbeanName) {
        return Server.DOMAIN.equals(mbeanName.getDomain()) //
                && mbeanName.getKeyPropertyList().containsKey("type") //
                && mbeanName.getKeyPropertyList().size() == 1;
    }

    /**
     * Returns the {@code ObjectName} of the {@code StatusLoggerAdminMBean}
     * associated with the specified {@code LoggerContextAdminMBean}.
     *
     * @param loggerContextObjName the {@code ObjectName} of a
     *            {@code LoggerContextAdminMBean}
     * @return {@code ObjectName} of the {@code StatusLoggerAdminMBean}
     */
    public ObjectName getStatusLoggerObjectName(final ObjectName loggerContextObjName) {
        if (!isLoggerContext(loggerContextObjName)) {
            throw new IllegalArgumentException("Not a LoggerContext: " + loggerContextObjName);
        }
        final String cxtName = loggerContextObjName.getKeyProperty("type");
        final String name = String.format(StatusLoggerAdminMBean.PATTERN, cxtName);
        try {
            return new ObjectName(name);
        } catch (final MalformedObjectNameException ex) {
            throw new IllegalStateException(name, ex);
        }
    }
}
