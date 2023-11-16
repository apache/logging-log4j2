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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.atomic.AtomicReference;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class Log4jServletContextListenerTest {
    /* event and servletContext are marked lenient because they aren't used in the
     * testDestroyWithNoInit but are only accessed during initialization
     */
    @Mock(strictness = Strictness.LENIENT)
    private ServletContextEvent event;

    @Mock(strictness = Strictness.LENIENT)
    private ServletContext servletContext;

    @Mock
    private Log4jWebLifeCycle initializer;

    private final AtomicReference<Object> count = new AtomicReference<>();

    @BeforeEach
    public void setUp() {
        given(event.getServletContext()).willReturn(servletContext);
        given(servletContext.getAttribute(Log4jWebSupport.SUPPORT_ATTRIBUTE)).willReturn(initializer);

        doAnswer(answerVoid((k, v) -> count.set(v)))
                .when(servletContext)
                .setAttribute(eq(Log4jServletContextListener.START_COUNT_ATTR), any());
        doAnswer(__ -> count.get()).when(servletContext).getAttribute(Log4jServletContextListener.START_COUNT_ATTR);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    public void testInitAndDestroy(final int listenerCount) throws Exception {
        final Log4jServletContextListener[] listeners = new Log4jServletContextListener[listenerCount];
        for (int idx = 0; idx < listenerCount; idx++) {
            final Log4jServletContextListener listener = new Log4jServletContextListener();
            listeners[idx] = listener;

            listener.contextInitialized(event);
            if (idx == 0) {
                then(initializer).should().start();
                then(initializer).should().setLoggerContext();
            } else {
                then(initializer).shouldHaveNoMoreInteractions();
            }
        }

        for (int idx = listenerCount - 1; idx >= 0; idx--) {
            final Log4jServletContextListener listener = listeners[idx];

            listener.contextDestroyed(event);
            if (idx == 0) {
                then(initializer).should().clearLoggerContext();
                then(initializer).should().stop();
            } else {
                then(initializer).shouldHaveNoMoreInteractions();
            }
        }
    }

    @Test
    public void testInitFailure() throws Exception {
        willThrow(new IllegalStateException(Strings.EMPTY)).given(initializer).start();
        final Log4jServletContextListener listener = new Log4jServletContextListener();

        assertThrows(
                RuntimeException.class,
                () -> listener.contextInitialized(this.event),
                "Failed to initialize Log4j properly.");
    }

    @Test
    public void initializingLog4jServletContextListenerShouldFaileWhenAutoShutdownIsTrue() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_AUTO_SHUTDOWN_DISABLED)))
                .willReturn("true");
        ensureInitializingFailsWhenAuthShutdownIsEnabled();
    }

    @Test
    public void initializingLog4jServletContextListenerShouldFaileWhenAutoShutdownIsTRUE() throws Exception {
        given(servletContext.getInitParameter(eq(Log4jWebSupport.IS_LOG4J_AUTO_SHUTDOWN_DISABLED)))
                .willReturn("TRUE");
        ensureInitializingFailsWhenAuthShutdownIsEnabled();
    }

    private void ensureInitializingFailsWhenAuthShutdownIsEnabled() {
        final Log4jServletContextListener listener = new Log4jServletContextListener();
        final String message = "Do not use " + Log4jServletContextListener.class.getSimpleName() + " when "
                + Log4jWebSupport.IS_LOG4J_AUTO_SHUTDOWN_DISABLED + " is true. Please use "
                + Log4jShutdownOnContextDestroyedListener.class.getSimpleName() + " instead of "
                + Log4jServletContextListener.class.getSimpleName() + ".";

        assertThrows(RuntimeException.class, () -> listener.contextInitialized(event), message);
    }
}
