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
package org.apache.logging.log4j.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@RunWith(MockitoJUnitRunner.class)
public class Log4jServletContextListenerTest {
    @Mock
    private ServletContextEvent event;
    @Mock
    private ServletContext servletContext;
    @Mock
    private Log4jWebLifeCycle initializer;

    private Log4jServletContextListener listener;

    @Before
    public void setUp() {
        this.listener = new Log4jServletContextListener();
        given(event.getServletContext()).willReturn(servletContext);
        given(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).willReturn(initializer);
    }

    @Test
    public void testInitAndDestroy() throws Exception {
        this.listener.contextInitialized(this.event);

        then(initializer).should().start();
        then(initializer).should().setLoggerContext();

        this.listener.contextDestroyed(this.event);

        then(initializer).should().clearLoggerContext();
        then(initializer).should().stop();
    }

    @Test
    public void testInitFailure() throws Exception {
        willThrow(new IllegalStateException(Strings.EMPTY)).given(initializer).start();

        try {
            this.listener.contextInitialized(this.event);
            fail("Expected a RuntimeException.");
        } catch (final RuntimeException e) {
            assertEquals("The message is not correct.", "Failed to initialize Log4j properly.", e.getMessage());
        }
    }

}
