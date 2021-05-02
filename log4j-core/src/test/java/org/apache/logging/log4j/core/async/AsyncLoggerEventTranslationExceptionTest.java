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
package org.apache.logging.log4j.core.async;

import com.lmax.disruptor.ExceptionHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.categories.AsyncLoggers;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.*;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.*;

/**
 * Test for https://issues.apache.org/jira/browse/LOG4J2-2816: an exception thrown in
 * RingBufferLogEventTranslator#translateTo should not cause another exception to be thrown later in the background
 * thread in RingBufferLogEventHandler#onEvent.
 */
@Category(AsyncLoggers.class)
public class AsyncLoggerEventTranslationExceptionTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
        System.setProperty("AsyncLogger.ExceptionHandler", TestExceptionHandler.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
        System.clearProperty("AsyncLogger.ExceptionHandler");
    }

    @Test
    public void testEventTranslationExceptionDoesNotCauseAsyncEventException() {
        final Logger log = LogManager.getLogger("com.foo.Bar");

        assertTrue("TestExceptionHandler was not configured properly", TestExceptionHandler.wasInstantiated);

        final Message exceptionThrowingMessage = new ExceptionThrowingMessage();

        try {
            ((AbstractLogger) log).logMessage("com.foo.Bar", Level.INFO, null, exceptionThrowingMessage, null);

            fail("TestMessageException should have been thrown");
        } catch (TestMessageException e) {
            // We expect the TestMessageException to be propagated, so ignore it.
            // The point of this test is to ensure that the ExceptionHandler is not called in the background thread.
        }

        CoreLoggerContexts.stopLoggerContext(); // stop async thread

        assertFalse("ExceptionHandler encountered an event exception",
                TestExceptionHandler.encounteredEventException);
    }

    public static class TestExceptionHandler implements ExceptionHandler<RingBufferLogEvent> {

        public static boolean wasInstantiated = false;
        public static boolean encounteredEventException = false;

        public TestExceptionHandler() {
            wasInstantiated = true;
        }

        @Override
        public void handleEventException(final Throwable ex, final long sequence, final RingBufferLogEvent event) {
            encounteredEventException = true;
        }

        @Override
        public void handleOnStartException(final Throwable ex) {
            fail("Unexpected start exception: " + ex.getMessage());
        }

        @Override
        public void handleOnShutdownException(final Throwable ex) {
            fail("Unexpected shutdown exception: " + ex.getMessage());
        }
    }

    private static class TestMessageException extends RuntimeException {

    }

    private static class ExceptionThrowingMessage extends ReusableSimpleMessage {

        @Override
        public String getFormattedMessage() {
            throw new TestMessageException();
        }

        @Override
        public String getFormat() {
            throw new TestMessageException();
        }

        @Override
        public Object[] getParameters() {
            throw new TestMessageException();
        }

        @Override
        public void formatTo(final StringBuilder buffer) {
            throw new TestMessageException();
        }

        @Override
        public Object[] swapParameters(final Object[] emptyReplacement) {
            throw new TestMessageException();
        }

        @Override
        public short getParameterCount() {
            throw new TestMessageException();
        }
    }

}
