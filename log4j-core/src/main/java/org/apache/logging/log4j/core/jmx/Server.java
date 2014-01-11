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
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.async.AsyncLoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Creates MBeans to instrument various classes in the log4j class hierarchy.
 * <p>
 * All instrumentation for Log4j 2 classes can be disabled by setting system
 * property {@code -Dlog4j2.disable.jmx=true}.
 */
public final class Server {

    private static final String PROPERTY_DISABLE_JMX = "log4j2.disable.jmx";
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    static final Executor executor = Executors.newFixedThreadPool(1);

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
    public static String escape(final String name) {
        final StringBuilder sb = new StringBuilder(name.length() * 2);
        boolean needsQuotes = false;
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            switch (c) {
            case '\\':
            case '*':
            case '?':
            case '\"':
                sb.append('\\'); // quote, star, question & backslash must be escaped
                needsQuotes = true; // ... and can only appear in quoted value
                break;
            case ',':
            case '=':
            case ':':
                needsQuotes = true; // no need to escape these, but value must be quoted
                break;
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
    public static void registerMBeans(final ContextSelector selector)
            throws JMException {

        // avoid creating Platform MBean Server if JMX disabled
        if (Boolean.getBoolean(PROPERTY_DISABLE_JMX)) {
            LOGGER.debug("JMX disabled for log4j2. Not registering MBeans.");
            return;
        }
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
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
    public static void registerMBeans(final ContextSelector selector,
            final MBeanServer mbs) throws JMException {

        if (Boolean.getBoolean(PROPERTY_DISABLE_JMX)) {
            LOGGER.debug("JMX disabled for log4j2. Not registering MBeans.");
            return;
        }
        registerStatusLogger(mbs, executor);
        registerContextSelector(selector, mbs, executor);

        final List<LoggerContext> contexts = selector.getLoggerContexts();
        registerContexts(contexts, mbs, executor);
    }
    
    public static void reregisterMBeansAfterReconfigure() {
        // avoid creating Platform MBean Server if JMX disabled
        if (Boolean.getBoolean(PROPERTY_DISABLE_JMX)) {
            LOGGER.debug("JMX disabled for log4j2. Not registering MBeans.");
            return;
        }
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        reregisterMBeansAfterReconfigure(mbs);
    }

    public static void reregisterMBeansAfterReconfigure(MBeanServer mbs) {
        if (Boolean.getBoolean(PROPERTY_DISABLE_JMX)) {
            LOGGER.debug("JMX disabled for log4j2. Not registering MBeans.");
            return;
        }
        
        try {
            unregisterStatusLogger(mbs);
            registerStatusLogger(mbs, executor);
        } catch (Exception ex) {
            LOGGER.error("Could not register MBeans", ex);
        }

        final ContextSelector selector = getContextSelector();
        if (selector == null) {
            LOGGER.debug("Could not register MBeans: no ContextSelector found.");
            return;
        }

        // first unregister the old MBeans
        unregisterContextSelector(mbs);
        unregisterContexts(mbs);
        unregisterLoggerConfigs("*", mbs);
        unregisterAsyncLoggerConfigRingBufferAdmins("*", mbs);
        unregisterAppenders("*", mbs);
        unregisterAsyncAppenders("*", mbs);

        // now provide instrumentation for the newly configured
        // LoggerConfigs and Appenders
        try {
            registerContextSelector(selector, mbs, executor);
            final List<LoggerContext> contexts = selector.getLoggerContexts();
            registerContexts(contexts, mbs, executor);
            for (LoggerContext context : contexts) {
                registerLoggerConfigs(context, mbs, executor);
                registerAppenders(context, mbs, executor);
            }
        } catch (final Exception ex) {
            LOGGER.error("Could not register mbeans", ex);
        }
    }

    private static ContextSelector getContextSelector() {
        ContextSelector selector = null;
        final LoggerContextFactory factory = LogManager.getFactory();
        if (factory instanceof Log4jContextFactory) {
            selector = ((Log4jContextFactory) factory).getSelector();
        }
        return selector;
    }

    /**
     * Unregisters all MBeans associated with the specified logger context
     * (including MBeans for {@code LoggerConfig}s and {@code Appender}s from
     * the platform MBean server.
     *
     * @param loggerContextName
     *            name of the logger context to unregister
     */
    public static void unregisterContext(String loggerContextName) {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        unregisterContext(loggerContextName, mbs);
    }

    /**
     * Unregisters all MBeans associated with the specified logger context
     * (including MBeans for {@code LoggerConfig}s and {@code Appender}s from
     * the platform MBean server.
     *
     * @param loggerContextName
     *            name of the logger context to unregister
     * @param mbs
     *            the MBean Server to unregister the instrumented objects from
     */
    public static void unregisterContext(String contextName, MBeanServer mbs) {
        final String pattern = LoggerContextAdminMBean.PATTERN;
        final String search = String.format(pattern, contextName, "*");
        unregisterAllMatching(search, mbs); // unregister context mbean
        unregisterLoggerConfigs(contextName, mbs);
        unregisterAppenders(contextName, mbs);
        unregisterAsyncAppenders(contextName, mbs);
        unregisterAsyncLoggerRingBufferAdmins(contextName, mbs);
        unregisterAsyncLoggerConfigRingBufferAdmins(contextName, mbs);
    }

    private static void registerStatusLogger(final MBeanServer mbs, final Executor executor)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {

        final StatusLoggerAdmin mbean = new StatusLoggerAdmin(executor);
        register(mbs, mbean, mbean.getObjectName());
    }

    private static void registerContextSelector(final ContextSelector selector, final MBeanServer mbs,
            final Executor executor) throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {

        final ContextSelectorAdmin mbean = new ContextSelectorAdmin(selector);
        register(mbs, mbean, mbean.getObjectName());
    }

    /**
     * Registers MBeans for all contexts in the list.
     * First unregisters each context (and nested loggers, appender etc)
     * to prevent InstanceAlreadyExistsExceptions.
     */
    private static void registerContexts(final List<LoggerContext> contexts, final MBeanServer mbs,
            final Executor executor) throws InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException {

        for (final LoggerContext ctx : contexts) {
            // first unregister the context and all nested loggers & appenders
            unregisterContext(ctx.getName());
            
            final LoggerContextAdmin mbean = new LoggerContextAdmin(ctx, executor);
            register(mbs, mbean, mbean.getObjectName());
            
            if (ctx instanceof AsyncLoggerContext) {
                RingBufferAdmin rbmbean = AsyncLogger.createRingBufferAdmin(ctx.getName());
                register(mbs, rbmbean, rbmbean.getObjectName());
            }
        }
    }

    private static void unregisterStatusLogger(final MBeanServer mbs) {
        unregisterAllMatching(StatusLoggerAdminMBean.NAME, mbs);
    }

    private static void unregisterContextSelector(final MBeanServer mbs) {
        unregisterAllMatching(ContextSelectorAdminMBean.NAME, mbs);
    }

    private static void unregisterLoggerConfigs(final String contextName,
            final MBeanServer mbs) {
        final String pattern = LoggerConfigAdminMBean.PATTERN;
        final String search = String.format(pattern, contextName, "*");
        unregisterAllMatching(search, mbs);
    }

    private static void unregisterContexts(final MBeanServer mbs) {
        final String pattern = LoggerContextAdminMBean.PATTERN;
        final String search = String.format(pattern, "*");
        unregisterAllMatching(search, mbs);
    }

    private static void unregisterAppenders(final String contextName,
            final MBeanServer mbs) {
        final String pattern = AppenderAdminMBean.PATTERN;
        final String search = String.format(pattern, contextName, "*");
        unregisterAllMatching(search, mbs);
    }

    private static void unregisterAsyncAppenders(final String contextName,
            final MBeanServer mbs) {
        final String pattern = AsyncAppenderAdminMBean.PATTERN;
        final String search = String.format(pattern, contextName, "*");
        unregisterAllMatching(search, mbs);
    }

    private static void unregisterAsyncLoggerRingBufferAdmins(final String contextName,
            final MBeanServer mbs) {
        final String pattern1 = RingBufferAdminMBean.PATTERN_ASYNC_LOGGER;
        final String search1 = String.format(pattern1, contextName);
        unregisterAllMatching(search1, mbs);
    }

    private static void unregisterAsyncLoggerConfigRingBufferAdmins(final String contextName,
            final MBeanServer mbs) {
        final String pattern2 = RingBufferAdminMBean.PATTERN_ASYNC_LOGGER_CONFIG;
        final String search2 = String.format(pattern2, contextName, "*");
        unregisterAllMatching(search2, mbs);
    }

    private static void unregisterAllMatching(final String search, final MBeanServer mbs) {
        try {
            final ObjectName pattern = new ObjectName(search);
            final Set<ObjectName> found = mbs.queryNames(pattern, null);
            for (final ObjectName objectName : found) {
                LOGGER.debug("Unregistering MBean {}", objectName);
                mbs.unregisterMBean(objectName);
            }
        } catch (final Exception ex) {
            LOGGER.error("Could not unregister MBeans for " + search, ex);
        }
    }

    private static void registerLoggerConfigs(final LoggerContext ctx, final MBeanServer mbs, final Executor executor)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {

        final Map<String, LoggerConfig> map = ctx.getConfiguration().getLoggers();
        for (final String name : map.keySet()) {
            final LoggerConfig cfg = map.get(name);
            final LoggerConfigAdmin mbean = new LoggerConfigAdmin(ctx.getName(), cfg);
            register(mbs, mbean, mbean.getObjectName());
            
            if (cfg instanceof AsyncLoggerConfig) {
                AsyncLoggerConfig async = (AsyncLoggerConfig) cfg;
                RingBufferAdmin rbmbean = async.createRingBufferAdmin(ctx.getName());
                register(mbs, rbmbean, rbmbean.getObjectName());
            }
        }
    }

    private static void registerAppenders(final LoggerContext ctx, final MBeanServer mbs, final Executor executor)
            throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {

        final Map<String, Appender> map = ctx.getConfiguration().getAppenders();
        for (final String name : map.keySet()) {
            final Appender appender = map.get(name);
            
            if (appender instanceof AsyncAppender) {
                AsyncAppender async = ((AsyncAppender) appender);
                final AsyncAppenderAdmin mbean = new AsyncAppenderAdmin(ctx.getName(), async);
                register(mbs, mbean, mbean.getObjectName());
            } else {
                final AppenderAdmin mbean = new AppenderAdmin(ctx.getName(), appender);
                register(mbs, mbean, mbean.getObjectName());
            }
        }
    }
    
    private static void register(MBeanServer mbs, Object mbean, ObjectName objectName) 
            throws InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        LOGGER.debug("Registering MBean {}", objectName);
        mbs.registerMBean(mbean, objectName);
    }
}
