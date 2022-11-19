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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.GarbageCollectionHelper;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("async")
@SetSystemProperty(key = Log4jProperties.GC_ENABLE_DIRECT_ENCODERS, value = "true")
@SetSystemProperty(key = Log4jProperties.ASYNC_LOGGER_FORMAT_MESSAGES_IN_BACKGROUND, value = "true")
@ContextSelectorType(AsyncLoggerContextSelector.class)
public class AsyncLoggerTestArgumentFreedOnErrorTest {

    // LOG4J2-2725: events are cleared even after failure
    @Test
    public void testMessageIsGarbageCollected() throws Exception {
        final AsyncLogger log = (AsyncLogger) LogManager.getLogger("com.foo.Bar");
        CountDownLatch garbageCollectionLatch = new CountDownLatch(1);
        log.fatal(new ThrowingMessage(garbageCollectionLatch));
        try (GarbageCollectionHelper gcHelper = new GarbageCollectionHelper()) {
            gcHelper.run();
            assertTrue(garbageCollectionLatch.await(30, TimeUnit.SECONDS),
                    "Parameter should have been garbage collected");
        }
    }

    private static class ThrowingMessage implements Message, StringBuilderFormattable {

        private final CountDownLatch latch;

        ThrowingMessage(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        protected void finalize() throws Throwable {
            latch.countDown();
            super.finalize();
        }

        @Override
        public String getFormattedMessage() {
            throw new Error("Expected");
        }

        @Override
        public String getFormat() {
            return "";
        }

        @Override
        public Object[] getParameters() {
            return new Object[0];
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }

        @Override
        public void formatTo(StringBuilder buffer) {
            throw new Error("Expected");
        }
    }
}
