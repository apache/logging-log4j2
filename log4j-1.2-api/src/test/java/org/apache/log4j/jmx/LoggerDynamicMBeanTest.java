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
package org.apache.log4j.jmx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Enumeration;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression for invalid JMX {@code addAppender} class names: instantiation can
 * return null, which must not NPE when calling {@code setName} and must not be
 * reported to the client as a successful invocation.
 */
class LoggerDynamicMBeanTest {

    private static final String[] ADD_APPENDER_SIGNATURE = {String.class.getName(), String.class.getName()};

    private MBeanServer server;

    @BeforeEach
    void createMBeanServer() {
        server = MBeanServerFactory.newMBeanServer();
    }

    /**
     * Register through an {@link MBeanServer} so the bean goes through
     * {@code preRegister}, matching real JMX use (and {@code AppenderDynamicMBeanTest}).
     */
    private LoggerDynamicMBean registerLoggerMBean(final Logger logger) throws Exception {
        final LoggerDynamicMBean mbean = new LoggerDynamicMBean(logger);
        final String name = logger.getName().isEmpty() ? "root" : logger.getName();
        // Same ObjectName shape as HierarchyDynamicMBean.addLoggerMBean.
        server.registerMBean(mbean, new ObjectName("log4j", "logger", name));
        return mbean;
    }

    @Test
    void addAppenderFailsWhenClassCannotBeInstantiated() throws Exception {
        final Logger logger = Logger.getLogger("jmx.LoggerDynamicMBeanTest.invalid");
        final LoggerDynamicMBean mbean = registerLoggerMBean(logger);

        final MBeanException thrown = assertThrows(
                MBeanException.class,
                () -> mbean.invoke(
                        "addAppender",
                        new Object[] {"this.class.does.not.exist.MissingAppender", "should-not-attach"},
                        ADD_APPENDER_SIGNATURE));
        assertTrue(thrown.getMessage().contains("Could not instantiate appender class"));
        assertInstanceOf(IllegalArgumentException.class, thrown.getTargetException());
        assertFalse(hasAppenderNamed(logger, "should-not-attach"));
    }

    @Test
    void addAppenderStillAttachesValidAppender() throws Exception {
        final Logger logger = Logger.getLogger("jmx.LoggerDynamicMBeanTest.valid");
        final LoggerDynamicMBean mbean = registerLoggerMBean(logger);

        final Object result = mbean.invoke(
                "addAppender", new Object[] {ConsoleAppender.class.getName(), "console-jmx"}, ADD_APPENDER_SIGNATURE);
        assertTrue(hasAppenderNamed(logger, "console-jmx"));
        // Legacy success return value, pinned so the fix cannot silently change it.
        assertEquals("Hello world.", result);
    }

    private static boolean hasAppenderNamed(final Logger logger, final String name) {
        final Enumeration enumeration = logger.getAllAppenders();
        while (enumeration.hasMoreElements()) {
            final Appender appender = (Appender) enumeration.nextElement();
            if (name.equals(appender.getName())) {
                return true;
            }
        }
        return false;
    }
}
