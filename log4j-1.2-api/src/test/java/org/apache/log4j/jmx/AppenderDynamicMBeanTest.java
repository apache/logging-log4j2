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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression for JMX {@code setLayout}: instantiateByClassName may return null.
 */
class AppenderDynamicMBeanTest {

    private static final String[] SET_LAYOUT_SIGNATURE = {String.class.getName()};

    private MBeanServer server;

    @BeforeEach
    void createMBeanServer() {
        server = MBeanServerFactory.newMBeanServer();
    }

    /**
     * Registration is required: {@code preRegister} injects the server that
     * {@code registerLayoutMBean} dereferences on the success path.
     */
    private AppenderDynamicMBean registerAppenderMBean(final ConsoleAppender appender) throws Exception {
        final AppenderDynamicMBean mbean = new AppenderDynamicMBean(appender);
        server.registerMBean(mbean, new ObjectName("log4j:appender=" + appender.getName()));
        return mbean;
    }

    @Test
    void setLayoutFailsWhenClassCannotBeInstantiated() throws Exception {
        final ConsoleAppender appender = new ConsoleAppender();
        appender.setName("jmx-layout-test");
        final AppenderDynamicMBean mbean = registerAppenderMBean(appender);

        final MBeanException thrown = assertThrows(
                MBeanException.class,
                () -> mbean.invoke(
                        "setLayout", new Object[] {"this.class.does.not.exist.MissingLayout"}, SET_LAYOUT_SIGNATURE));
        assertTrue(thrown.getMessage().contains("Could not instantiate layout class"));
        assertInstanceOf(IllegalArgumentException.class, thrown.getTargetException());
        assertNull(appender.getLayout());
    }

    @Test
    void setLayoutStillAttachesValidLayout() throws Exception {
        final ConsoleAppender appender = new ConsoleAppender();
        appender.setName("jmx-layout-valid");
        final AppenderDynamicMBean mbean = registerAppenderMBean(appender);

        mbean.invoke("setLayout", new Object[] {PatternLayout.class.getName()}, SET_LAYOUT_SIGNATURE);
        assertInstanceOf(PatternLayout.class, appender.getLayout());
        assertTrue(server.isRegistered(
                new ObjectName("log4j:appender=" + appender.getName() + ",layout=" + PatternLayout.class.getName())));
    }
}
