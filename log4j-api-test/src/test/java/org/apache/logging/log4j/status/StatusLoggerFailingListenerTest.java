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
package org.apache.logging.log4j.status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import uk.org.webcompere.systemstubs.SystemStubs;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
@ResourceLock("log4j2.StatusLogger")
class StatusLoggerFailingListenerTest {

    public static final StatusLogger STATUS_LOGGER = StatusLogger.getLogger();

    private StatusListener listener;

    @BeforeEach
    void createAndRegisterListener() {
        listener = mock(StatusListener.class);
        STATUS_LOGGER.registerListener(listener);
    }

    @AfterEach
    void unregisterListener() {
        STATUS_LOGGER.removeListener(listener);
    }

    @Test
    void logging_with_failing_listener_should_not_cause_stack_overflow() throws Exception {

        // Set up a failing listener on `log(StatusData)`
        when(listener.getStatusLevel()).thenReturn(Level.ALL);
        final Exception listenerFailure = new RuntimeException("test failure " + Math.random());
        doThrow(listenerFailure).when(listener).log(any());

        // Log something and verify exception dump
        final String stderr = SystemStubs.tapSystemErr(() -> STATUS_LOGGER.error("foo"));
        final String listenerFailureClassName = listenerFailure.getClass().getCanonicalName();
        assertThat(stderr).contains(listenerFailureClassName + ": " + listenerFailure.getMessage());
    }
}
