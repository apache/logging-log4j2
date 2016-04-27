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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

public class Log4jServletContextListenerTest {
    private ServletContextEvent event;
    private ServletContext servletContext;
    private Log4jWebLifeCycle initializer;

    private Log4jServletContextListener listener;

    @Before
    public void setUp() {
        this.event = createStrictMock(ServletContextEvent.class);
        this.servletContext = createStrictMock(ServletContext.class);
        this.initializer = createStrictMock(Log4jWebLifeCycle.class);

        this.listener = new Log4jServletContextListener();
    }

    @After
    public void tearDown() {
        verify(this.event, this.servletContext, this.initializer);
    }

    @Test
    public void testInitAndDestroy() throws Exception {
        expect(this.event.getServletContext()).andReturn(this.servletContext);
        expect(this.servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.start();
        expectLastCall();
        this.initializer.setLoggerContext();
        expectLastCall();

        replay(this.event, this.servletContext, this.initializer);

        this.listener.contextInitialized(this.event);

        verify(this.event, this.servletContext, this.initializer);
        reset(this.event, this.servletContext, this.initializer);

        this.initializer.clearLoggerContext();
        expectLastCall();
        this.initializer.stop();
        expectLastCall();

        replay(this.event, this.servletContext, this.initializer);

        this.listener.contextDestroyed(this.event);
    }

    @Test
    public void testInitFailure() throws Exception {
        expect(this.event.getServletContext()).andReturn(this.servletContext);
        expect(this.servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).andReturn(this.initializer);
        this.initializer.start();
        expectLastCall().andThrow(new IllegalStateException(Strings.EMPTY));

        replay(this.event, this.servletContext, this.initializer);

        try {
            this.listener.contextInitialized(this.event);
            fail("Expected a RuntimeException.");
        } catch (final RuntimeException e) {
            assertEquals("The message is not correct.", "Failed to initialize Log4j properly.", e.getMessage());
        }
    }

}
