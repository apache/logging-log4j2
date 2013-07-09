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
package org.apache.logging.log4j.core.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.UnavailableException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Log4jServletContextListenerTest {
    private ServletContextEvent event;
    private ServletContext servletContext;
    private Log4jWebInitializer initializer;

    private Log4jServletContextListener listener;

    @Before
    public void setUp() {
        this.event = createStrictMock(ServletContextEvent.class);
        this.servletContext = createStrictMock(ServletContext.class);
        this.initializer = createStrictMock(Log4jWebInitializer.class);

        this.listener = new Log4jServletContextListener();
    }

    @After
    public void tearDown() {
        verify(this.event, this.servletContext, this.initializer);
    }

    @Test
    public void testInitAndDestroy() throws Exception {
        expect(this.event.getServletContext()).andReturn(this.servletContext);
        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        expect(this.servletContext.getAttribute(Log4jWebInitializer.INITIALIZER_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.initialize();
        expectLastCall();
        this.initializer.setLoggerContext();
        expectLastCall();

        replay(this.event, this.servletContext, this.initializer);

        this.listener.contextInitialized(this.event);

        verify(this.event, this.servletContext, this.initializer);
        reset(this.event, this.servletContext, this.initializer);

        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        this.initializer.clearLoggerContext();
        expectLastCall();
        this.initializer.deinitialize();
        expectLastCall();

        replay(this.event, this.servletContext, this.initializer);

        this.listener.contextDestroyed(this.event);
    }

    @Test
    public void testInitFailure() throws Exception {
        expect(this.event.getServletContext()).andReturn(this.servletContext);
        this.servletContext.log(anyObject(String.class));
        expectLastCall();
        expect(this.servletContext.getAttribute(Log4jWebInitializer.INITIALIZER_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.initialize();
        expectLastCall().andThrow(new UnavailableException(""));

        replay(this.event, this.servletContext, this.initializer);

        try {
            this.listener.contextInitialized(this.event);
            fail("Expected a RuntimeException.");
        } catch (final RuntimeException e) {
            assertEquals("The message is not correct.", "Failed to initialize Log4j properly.", e.getMessage());
        }
    }

    @Test
    public void testDestroy() {
        replay(this.event, this.servletContext, this.initializer);

        try {
            this.listener.contextDestroyed(this.event);
            fail("Expected an IllegalStateException.");
        } catch (final IllegalStateException ignore) {

        }
    }
}
