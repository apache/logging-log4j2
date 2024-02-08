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
package org.apache.logging.log4j.test.junit;

import static org.apache.logging.log4j.test.junit.ExtensionContextAnchor.getAttribute;
import static org.apache.logging.log4j.test.junit.ExtensionContextAnchor.setAttribute;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Replaces {@link StatusLogger} static instance with a mocked one.
 * <p>
 * <b>Warning!</b>
 * Many classes store the result of {@link StatusLogger#getLogger()} in {@code static} field.
 * Hence, the mock replacement must be performed before anybody tries to access it.
 * Similarly, we cannot replace the mock in between tests, since it is already stored in {@code static} fields.
 * That is why we only reset the mocked instance before each test.
 * </p>
 *
 * @see UsingStatusLoggerMock
 */
class StatusLoggerMockExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private static final String KEY_PREFIX = StatusLoggerMockExtension.class.getSimpleName() + '.';

    private static final String INITIAL_STATUS_LOGGER_KEY = KEY_PREFIX + "initialStatusLogger";

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        setAttribute(INITIAL_STATUS_LOGGER_KEY, StatusLogger.getLogger(), context);
        final StatusLogger statusLogger = mock(StatusLogger.class);
        stubFallbackListener(statusLogger);
        StatusLogger.setLogger(statusLogger);
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        final StatusLogger statusLogger = StatusLogger.getLogger();
        reset(statusLogger); // Stubs get reset too!
        stubFallbackListener(statusLogger);
    }

    private static void stubFallbackListener(final StatusLogger statusLogger) {
        final StatusConsoleListener fallbackListener = mock(StatusConsoleListener.class);
        when(statusLogger.getFallbackListener()).thenReturn(fallbackListener);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        final StatusLogger statusLogger = getAttribute(INITIAL_STATUS_LOGGER_KEY, StatusLogger.class, context);
        StatusLogger.setLogger(statusLogger);
    }
}
