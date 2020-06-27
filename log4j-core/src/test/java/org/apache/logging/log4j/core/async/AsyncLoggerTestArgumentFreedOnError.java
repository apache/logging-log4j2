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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.GarbageCollectionHelper;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

@Category(AsyncLoggers.class)
public class AsyncLoggerTestArgumentFreedOnError {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.enable.direct.encoders", "true");
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty("log4j.format.msg.async", "true");
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                AsyncLoggerContextSelector.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
    }

    // LOG4J2-2725: events are cleared even after failure
    @Test
    public void testMessageIsGarbageCollected() throws Exception {
        final AsyncLogger log = (AsyncLogger) LogManager.getLogger("com.foo.Bar");
        CountDownLatch garbageCollectionLatch = new CountDownLatch(1);
        log.fatal(new ThrowingMessage(garbageCollectionLatch));
        GarbageCollectionHelper gcHelper = new GarbageCollectionHelper();
        gcHelper.run();
        try {
            assertTrue("Parameter should have been garbage collected",
                    garbageCollectionLatch.await(30, TimeUnit.SECONDS));
        } finally {
            gcHelper.close();
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

        @Override
        public StackTraceElement getSource() {
            return null;
        }
    }
}
