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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;
import org.junit.jupiter.api.Test;

/**
 * Regression for JMX {@code setLayout}: instantiateByClassName may return null.
 */
class AppenderDynamicMBeanTest {

    @Test
    void setLayoutDoesNotNpeWhenClassCannotBeInstantiated() throws Exception {
        final ConsoleAppender appender = new ConsoleAppender();
        appender.setName("jmx-layout-test");
        final AppenderDynamicMBean mbean = new AppenderDynamicMBean(appender);

        final Object result = assertDoesNotThrow(
                () -> mbean.invoke("setLayout", new Object[] {"this.class.does.not.exist.MissingLayout"}, new String[] {
                    String.class.getName()
                }));
        assertTrue(result == null || result.toString().contains("Could not instantiate"));
        assertNull(appender.getLayout());
    }

    @Test
    void setLayoutStillAttachesValidLayout() throws Exception {
        final ConsoleAppender appender = new ConsoleAppender();
        appender.setName("jmx-layout-valid");
        final AppenderDynamicMBean mbean = new AppenderDynamicMBean(appender);

        mbean.invoke("setLayout", new Object[] {PatternLayout.class.getName()}, new String[] {String.class.getName()});
        assertNotNull(appender.getLayout());
        assertTrue(appender.getLayout() instanceof PatternLayout);
    }
}
