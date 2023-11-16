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
package org.apache.logging.log4j.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class Log4jShutdownOnContextDestroyedListenerTest {
    @Mock(lenient = true)
    private ServletContextEvent event;

    @Mock(lenient = true)
    private ServletContext servletContext;

    @Mock
    private Log4jWebLifeCycle initializer;

    private Log4jShutdownOnContextDestroyedListener listener;

    public void setUp(final boolean mockInitializer) {
        this.listener = new Log4jShutdownOnContextDestroyedListener();
        given(event.getServletContext()).willReturn(servletContext);
        if (mockInitializer) {
            given(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE))
                    .willReturn(initializer);
        }
    }

    @Test
    public void testInitAndDestroy() throws Exception {
        setUp(true);
        this.listener.contextInitialized(this.event);

        then(initializer).should(never()).start();
        then(initializer).should(never()).setLoggerContext();

        this.listener.contextDestroyed(this.event);

        then(initializer).should().clearLoggerContext();
        then(initializer).should().stop();
    }

    @Test
    public void testDestroy() throws Exception {
        setUp(true);
        this.listener.contextDestroyed(this.event);

        then(initializer).should(never()).clearLoggerContext();
        then(initializer).should(never()).stop();
    }

    @Test
    public void whenNoInitializerInContextTheContextInitializedShouldThrowAnException() {
        setUp(false);

        assertThrows(IllegalStateException.class, () -> {
            this.listener.contextInitialized(this.event);
        });
    }
}
