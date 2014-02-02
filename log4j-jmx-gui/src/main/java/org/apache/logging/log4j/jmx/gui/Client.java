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
import java.util.Set;

import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.logging.log4j.core.helpers.Assert;
import org.apache.logging.log4j.core.jmx.ContextSelectorAdminMBean;
import org.apache.logging.log4j.core.jmx.LoggerContextAdminMBean;
import org.apache.logging.log4j.core.jmx.Server;
import org.apache.logging.log4j.core.jmx.StatusLoggerAdminMBean;

/**
 * This class allows client-side code to perform operations on remote
 * (server-side) MBeans via proxies.
 */
public class Client {
    private JMXConnector connector;
    private final MBeanServerConnection connection;
    private List<StatusLoggerAdminMBean> statusLoggerAdminList;
    private List<ContextSelectorAdminMBean> contextSelectorAdminList;
    private List<LoggerContextAdminMBean> contextAdminList;

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
    public Client(final JMXConnector connector) throws JMException, IOException {
        this.connector = Assert.isNotNull(connector, "JMXConnector");
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
    public Client(final MBeanServerConnection mBeanServerConnection) throws JMException, IOException {
        this.connection = mBeanServerConnection;
        init();
    }

    private void init() throws JMException, IOException {
        statusLoggerAdminList = new ArrayList<StatusLoggerAdminMBean>();
        final Set<ObjectName> statusLogNames = find(StatusLoggerAdminMBean.PATTERN);
        for (final ObjectName statusLogName : statusLogNames) {
            final StatusLoggerAdminMBean ctx = JMX.newMBeanProxy(connection, //
                    statusLogName, //
                    StatusLoggerAdminMBean.class, true); // notificationBroadcaster
            statusLoggerAdminList.add(ctx);
        }

        contextSelectorAdminList = new ArrayList<ContextSelectorAdminMBean>();
        final Set<ObjectName> selectorNames = find(ContextSelectorAdminMBean.PATTERN);
        for (final ObjectName selectorName : selectorNames) {
            final ContextSelectorAdminMBean ctx = JMX.newMBeanProxy(connection, //
                    selectorName, //
                    ContextSelectorAdminMBean.class, false);
            contextSelectorAdminList.add(ctx);
        }

        contextAdminList = new ArrayList<LoggerContextAdminMBean>();
        final Set<ObjectName> contextNames = find(LoggerContextAdminMBean.PATTERN);
        for (final ObjectName contextName : contextNames) {
            final LoggerContextAdminMBean ctx = JMX.newMBeanProxy(connection, //
                    contextName, //
                    LoggerContextAdminMBean.class, false);
            contextAdminList.add(ctx);

            // TODO Appenders, LoggerConfigs
        }
    }

    private Set<ObjectName> find(String pattern) throws JMException, IOException {
        final ObjectName search = new ObjectName(String.format(pattern, "*"));
        final Set<ObjectName> result = connection.queryNames(search, null);
        return result;
    }

    /**
     * Returns a list of proxies that allows operations to be performed on the
     * remote {@code ContextSelectorAdminMBean}s.
     * 
     * @return a list of proxies to the remote {@code ContextSelectorAdminMBean}
     *         s
     */
    public List<ContextSelectorAdminMBean> getContextSelectorAdminList() {
        return contextSelectorAdminList;
    }

    /**
     * Returns a list of proxies that allow operations to be performed on the
     * remote {@code LoggerContextAdminMBean}s.
     * 
     * @return a list of proxies to the remote {@code LoggerContextAdminMBean}s
     */
    public List<LoggerContextAdminMBean> getLoggerContextAdmins() {
        return new ArrayList<LoggerContextAdminMBean>(contextAdminList);
    }

    /**
     * Closes the client connection to its server. Any ongoing or new requests
     * to the MBeanServerConnection will fail.
     */
    public void close() {
        try {
            connector.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
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
     * Returns a list of proxies that allows operations to be performed on the
     * remote {@code StatusLoggerAdminMBean}s.
     * 
     * @return a list of proxies to the remote {@code StatusLoggerAdminMBean}s
     */
    public List<StatusLoggerAdminMBean> getStatusLoggerAdminList() {
        return statusLoggerAdminList;
    }

    /**
     * Returns the {@code StatusLoggerAdminMBean} associated with the specified
     * context name, or {@code null}.
     * 
     * @param contextName search key
     * @return StatusLoggerAdminMBean or null
     * @throws MalformedObjectNameException
     * @throws IOException
     */
    public StatusLoggerAdminMBean getStatusLoggerAdmin(String contextName) throws MalformedObjectNameException,
            IOException {
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
}
