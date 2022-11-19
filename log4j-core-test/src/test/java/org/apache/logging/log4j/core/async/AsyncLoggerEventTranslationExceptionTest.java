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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ReusableSimpleMessage;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import com.lmax.disruptor.ExceptionHandler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test an exception thrown in {@link RingBufferLogEventTranslator#translateTo(RingBufferLogEvent, long)}
 * does not cause another exception to be thrown later in the background thread
 * in {@link RingBufferLogEventHandler#onEvent(RingBufferLogEvent, long, boolean)}.
 *
 * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-2816">LOG4J2-2816</a>
 */
@Tag("async")
@ContextSelectorType(AsyncLoggerContextSelector.class)
@SetSystemProperty(key = Log4jProperties.ASYNC_LOGGER_EXCEPTION_HANDLER_CLASS_NAME, value = "org.apache.logging.log4j.core.async.AsyncLoggerEventTranslationExceptionTest$TestExceptionHandler")
class AsyncLoggerEventTranslationExceptionTest {

    @Test
    void testEventTranslationExceptionDoesNotCauseAsyncEventException() {

        final Logger log = LogManager.getLogger("com.foo.Bar");

        assertTrue(
                TestExceptionHandler.INSTANTIATED,
                "TestExceptionHandler was not configured properly");

        final Message exceptionThrowingMessage = new ExceptionThrowingMessage();
        assertThrows(
                TestMessageException.class,
                () -> ((AbstractLogger) log).logMessage(
                        "com.foo.Bar",
                        Level.INFO,
                        null,
                        exceptionThrowingMessage,
                        null));

        CoreLoggerContexts.stopLoggerContext(); // stop async thread

        assertFalse(
                TestExceptionHandler.EVENT_EXCEPTION_ENCOUNTERED,
                "ExceptionHandler encountered an event exception");

    }

    public static final class TestExceptionHandler implements ExceptionHandler<RingBufferLogEvent> {

        private static boolean INSTANTIATED = false;

        private static boolean EVENT_EXCEPTION_ENCOUNTERED = false;

        public TestExceptionHandler() {
            INSTANTIATED = true;
        }

        @Override
        public void handleEventException(final Throwable error, final long sequence, final RingBufferLogEvent event) {
            EVENT_EXCEPTION_ENCOUNTERED = true;
        }

        @Override
        public void handleOnStartException(final Throwable error) {
            fail("Unexpected start exception: " + error.getMessage());
        }

        @Override
        public void handleOnShutdownException(final Throwable error) {
            fail("Unexpected shutdown exception: " + error.getMessage());
        }

    }

    private static class TestMessageException extends RuntimeException {}

    private static final class ExceptionThrowingMessage extends ReusableSimpleMessage {

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
