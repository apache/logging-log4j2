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

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.logging.log4j.core.helpers.Assert;
import org.apache.logging.log4j.core.jmx.ContextSelectorAdminMBean;
import org.apache.logging.log4j.core.jmx.LoggerContextAdminMBean;
import org.apache.logging.log4j.core.jmx.StatusLoggerAdminMBean;

/**
 * This class allows client-side code to perform operations on remote
 * (server-side) MBeans via proxies.
 */
public class Client {
    private JMXConnector connector;
    private final MBeanServerConnection connection;
    private StatusLoggerAdminMBean statusLoggerAdmin;
    private ContextSelectorAdminMBean contextSelectorAdmin;
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
    public Client(final JMXConnector connector) throws MalformedObjectNameException,
            IOException {
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
    public Client(final MBeanServerConnection mBeanServerConnection)
            throws MalformedObjectNameException, IOException {
        this.connection = mBeanServerConnection;
        init();
    }

    private void init() throws MalformedObjectNameException, IOException {
        statusLoggerAdmin = JMX.newMBeanProxy(connection, //
                new ObjectName(StatusLoggerAdminMBean.NAME), //
                StatusLoggerAdminMBean.class, true);

        contextSelectorAdmin = JMX.newMBeanProxy(connection, //
                new ObjectName(ContextSelectorAdminMBean.NAME), //
                ContextSelectorAdminMBean.class, false);

        contextAdminList = new ArrayList<LoggerContextAdminMBean>();
        final String pattern = String.format(LoggerContextAdminMBean.PATTERN, "*");
        final ObjectName search = new ObjectName(pattern);
        final Set<ObjectName> found = connection.queryNames(search, null);
        for (final ObjectName contextName : found) {
            final LoggerContextAdminMBean ctx = JMX.newMBeanProxy(connection, //
                    contextName, //
                    LoggerContextAdminMBean.class, false);
            contextAdminList.add(ctx);

            // TODO Appenders, LoggerConfigs
        }
    }

    /**
     * Returns a proxy that allows operations to be performed on the remote
     * {@code ContextSelectorAdminMBean}.
     *
     * @return a proxy to the remote {@code ContextSelectorAdminMBean}
     */
    public ContextSelectorAdminMBean getContextSelectorAdmin() {
        return contextSelectorAdmin;
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
     * Returns a proxy that allows operations to be performed on the remote
     * {@code StatusLoggerAdminMBean}.
     *
     * @return a proxy to the remote {@code StatusLoggerAdminMBean}
     */
    public StatusLoggerAdminMBean getStatusLoggerAdmin() {
        return statusLoggerAdmin;
    }
}
