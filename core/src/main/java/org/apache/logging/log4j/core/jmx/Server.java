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
package org.apache.logging.log4j.core.jmx;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.JMException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Creates MBeans to instrument various classes in the log4j class hierarchy.
 * <p>
 * All instrumentation for Log4j 2 classes can be disabled by setting system
 * property {@code -Dlog4j2.disable.jmx=true}.
 */
public final class Server {

    private static final String PROPERTY_DISABLE_JMX = "log4j2.disable.jmx";
    
    private Server() {
    }

    /**
     * Either returns the specified name as is, or returns a quoted value
     * containing the specified name with the special characters (comma, equals,
     * colon, quote, asterisk, or question mark) preceded with a backslash.
     * 
     * @param name
     *            the name to escape so it can be used as a value in an
     *            {@link ObjectName}.
     * @return the escaped name
     */
    public static String escape(String name) {
        StringBuilder sb = new StringBuilder(name.length() * 2);
        boolean needsQuotes = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            switch (c) {
            case ',':
            case '=':
            case ':':
            case '\\':
            case '*':
            case '?':
                sb.append('\\');
                needsQuotes = true;
            }
            sb.append(c);
        }
        if (needsQuotes) {
            sb.insert(0, '\"');
            sb.append('\"');
        }
        return sb.toString();
    }

    /**
     * Creates MBeans to instrument the specified selector and other classes in
     * the log4j class hierarchy and registers the MBeans in the platform MBean
     * server so they can be accessed by remote clients.
     * 
     * @param selector
     *            starting point in the log4j class hierarchy
     * @throws JMException
     *             if a problem occurs during registration
     */
    public static void registerMBeans(ContextSelector selector)
            throws JMException {

        // avoid creating Platform MBean Server if JMX disabled
        if (Boolean.getBoolean(PROPERTY_DISABLE_JMX)) {
            StatusLogger.getLogger().debug(
                    "JMX disabled for log4j2. Not registering MBeans.");
            return;
        }
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        registerMBeans(selector, mbs);
    }

    /**
     * Creates MBeans to instrument the specified selector and other classes in
     * the log4j class hierarchy and registers the MBeans in the specified MBean
     * server so they can be accessed by remote clients.
     * 
     * @param selector
     *            starting point in the log4j class hierarchy
     * @param mbs
     *            the MBean Server to register the instrumented objects in
     * @throws JMException
     *             if a problem occurs during registration
     */
    public static void registerMBeans(ContextSelector selector,
            final MBeanServer mbs) throws JMException {

        if (Boolean.getBoolean(PROPERTY_DISABLE_JMX)) {
            StatusLogger.getLogger().debug(
                    "JMX disabled for log4j2. Not registering MBeans.");
            return;
        }
        final Executor executor = Executors.newFixedThreadPool(1);
        registerStatusLogger(mbs, executor);
        registerContextSelector(selector, mbs, executor);

        List<LoggerContext> contexts = selector.getLoggerContexts();
        registerContexts(contexts, mbs, executor);

        for (final LoggerContext context : contexts) {
            context.addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (!LoggerContext.PROPERTY_CONFIG.equals(evt
                            .getPropertyName())) {
                        return;
                    }
                    // first unregister the MBeans that instrument the
                    // previous instrumented LoggerConfigs and Appenders
                    unregisterLoggerConfigs(context, mbs);
                    unregisterAppenders(context, mbs);

                    // now provide instrumentation for the newly configured
                    // LoggerConfigs and Appenders
                    try {
                        registerLoggerConfigs(context, mbs, executor);
                        registerAppenders(context, mbs, executor);
                    } catch (Exception ex) {
                        StatusLogger.getLogger().error(
                                "Could not register mbeans", ex);
                    }
                }
            });
        }
    }

    private static void registerStatusLogger(MBeanServer mbs, Executor executor)
            throws MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {

        StatusLoggerAdmin mbean = new StatusLoggerAdmin(executor);
        mbs.registerMBean(mbean, mbean.getObjectName());
    }

    private static void registerContextSelector(ContextSelector selector,
            MBeanServer mbs, Executor executor)
            throws MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {

        ContextSelectorAdmin mbean = new ContextSelectorAdmin(selector);
        mbs.registerMBean(mbean, mbean.getObjectName());
    }

    private static void registerContexts(List<LoggerContext> contexts,
            MBeanServer mbs, Executor executor)
            throws MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {

        for (LoggerContext ctx : contexts) {
            LoggerContextAdmin mbean = new LoggerContextAdmin(ctx, executor);
            mbs.registerMBean(mbean, mbean.getObjectName());
        }
    }

    private static void unregisterLoggerConfigs(LoggerContext context,
            MBeanServer mbs) {
        String pattern = LoggerConfigAdminMBean.PATTERN;
        String search = String.format(pattern, context.getName(), "*");
        unregisterAllMatching(search, mbs);
    }

    private static void unregisterAppenders(LoggerContext context,
            MBeanServer mbs) {
        String pattern = AppenderAdminMBean.PATTERN;
        String search = String.format(pattern, context.getName(), "*");
        unregisterAllMatching(search, mbs);
    }

    private static void unregisterAllMatching(String search, MBeanServer mbs) {
        try {
            ObjectName pattern = new ObjectName(search);
            Set<ObjectName> found = mbs.queryNames(pattern, null);
            for (ObjectName objectName : found) {
                mbs.unregisterMBean(objectName);
            }
        } catch (Exception ex) {
            StatusLogger.getLogger()
                    .error("Could not unregister " + search, ex);
        }
    }

    private static void registerLoggerConfigs(LoggerContext ctx,
            MBeanServer mbs, Executor executor)
            throws MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {

        Map<String, LoggerConfig> map = ctx.getConfiguration().getLoggers();
        for (String name : map.keySet()) {
            LoggerConfig cfg = map.get(name);
            LoggerConfigAdmin mbean = new LoggerConfigAdmin(ctx.getName(), cfg);
            mbs.registerMBean(mbean, mbean.getObjectName());
        }
    }

    private static void registerAppenders(LoggerContext ctx, MBeanServer mbs,
            Executor executor) throws MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {

        Map<String, Appender<?>> map = ctx.getConfiguration().getAppenders();
        for (String name : map.keySet()) {
            Appender<?> appender = map.get(name);
            AppenderAdmin mbean = new AppenderAdmin(ctx.getName(), appender);
            mbs.registerMBean(mbean, mbean.getObjectName());
        }
    }
}
