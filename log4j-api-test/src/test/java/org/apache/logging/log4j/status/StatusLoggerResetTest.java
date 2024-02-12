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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.junit.jupiter.api.Test;

class StatusLoggerResetTest {

    @Test
    void test_reset() throws IOException {

        // Create the fallback listener
        final PrintStream fallbackListenerInitialStream = mock(PrintStream.class);
        final Level fallbackListenerInitialLevel = Level.INFO;
        final StatusConsoleListener fallbackListener =
                new StatusConsoleListener(fallbackListenerInitialLevel, fallbackListenerInitialStream);

        // Create the `StatusLogger`
        final StatusLogger.Config loggerConfig = new StatusLogger.Config(false, 0, null);
        final StatusLogger logger = new StatusLogger(
                StatusLoggerResetTest.class.getSimpleName(),
                ParameterizedNoReferenceMessageFactory.INSTANCE,
                loggerConfig,
                fallbackListener);

        // Create and register a listener
        final Level listenerLevel = Level.DEBUG;
        assertThat(listenerLevel.isLessSpecificThan(fallbackListenerInitialLevel))
                .isTrue();
        final StatusListener listener = mock(StatusListener.class);
        when(listener.getStatusLevel()).thenReturn(listenerLevel);
        logger.registerListener(listener);

        // Verify the `StatusLogger` state
        assertThat(logger.getLevel()).isEqualTo(listenerLevel);
        assertThat(logger.getListeners()).containsExactly(listener);

        // Update the fallback listener
        final PrintStream fallbackListenerNewStream = mock(PrintStream.class);
        fallbackListener.setStream(fallbackListenerNewStream);
        verify(fallbackListenerInitialStream).close();
        final Level fallbackListenerNewLevel = Level.TRACE;
        assertThat(fallbackListenerNewLevel.isLessSpecificThan(listenerLevel)).isTrue();
        fallbackListener.setLevel(fallbackListenerNewLevel);

        // Verify the `StatusLogger` state
        assertThat(logger.getLevel()).isEqualTo(fallbackListenerNewLevel);

        // Reset the `StatusLogger` and verify the state
        logger.reset();
        verify(listener).close();
        verify(fallbackListenerNewStream).close();
        assertThat(logger.getLevel()).isEqualTo(fallbackListenerInitialLevel);
        assertThat(logger.getListeners()).isEmpty();
    }
}
