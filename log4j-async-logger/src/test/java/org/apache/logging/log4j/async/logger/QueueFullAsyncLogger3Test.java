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
package org.apache.logging.log4j.async.logger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.logging.log4j.core.test.TestConstants.ASYNC_FORMAT_MESSAGES_IN_BACKGROUND;
import static org.apache.logging.log4j.core.test.TestConstants.ASYNC_LOGGER_RING_BUFFER_SIZE;
import static org.apache.logging.log4j.core.test.TestConstants.ASYNC_QUEUE_FULL_POLICY_CLASS_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.GarbageCollectionHelper;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory;
import org.apache.logging.log4j.core.async.DiscardingAsyncQueueFullPolicy;
import org.apache.logging.log4j.core.impl.CoreProperties;
import org.apache.logging.log4j.core.test.async.BlockingAppender;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests queue full scenarios with pure AsyncLoggers (all loggers async).
 */
@ContextSelectorType(AsyncLoggerContextSelector.class)
@SetTestProperty(key = ASYNC_LOGGER_RING_BUFFER_SIZE, value = "128")
@SetTestProperty(key = ASYNC_FORMAT_MESSAGES_IN_BACKGROUND, value = "true")
@SetTestProperty(key = ASYNC_QUEUE_FULL_POLICY_CLASS_NAME, value = "Discard")
public class QueueFullAsyncLogger3Test extends QueueFullAsyncAbstractTest {

    @Override
    protected void checkConfig(final LoggerContext ctx) {
        assertAsyncLogger(ctx, 128);
        assertFormatMessagesInBackground();
        assertThat(AsyncQueueFullPolicyFactory.create(
                        ctx.getEnvironment().getProperty(CoreProperties.QueueFullPolicyProperties.class),
                        StatusLogger.getLogger()))
                .isInstanceOf(DiscardingAsyncQueueFullPolicy.class);
    }

    @Test
    @Timeout(value = 15, unit = SECONDS)
    @LoggerContextSource
    public void discardedMessagesShouldBeGarbageCollected(
            final LoggerContext ctx, final @Named(APPENDER_NAME) BlockingAppender blockingAppender)
            throws InterruptedException {
        checkConfig(ctx);
        final Logger logger = ctx.getLogger(getClass());

        blockingAppender.logEvents = null;
        blockingAppender.countDownLatch = new CountDownLatch(1);
        final int count = 200;
        final CountDownLatch garbageCollectionLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            logger.info(new CountdownOnGarbageCollectMessage(garbageCollectionLatch));
        }
        blockingAppender.countDownLatch.countDown();

        final GarbageCollectionHelper gcHelper = new GarbageCollectionHelper();
        gcHelper.run();
        try {
            assertTrue("Parameter should have been garbage collected", garbageCollectionLatch.await(30, SECONDS));
        } finally {
            gcHelper.close();
        }
    }

    private static final class CountdownOnGarbageCollectMessage implements Message {

        private final CountDownLatch latch;

        CountdownOnGarbageCollectMessage(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public String getFormattedMessage() {
            return "formatted";
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return org.apache.logging.log4j.util.Constants.EMPTY_OBJECT_ARRAY;
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }

        @Override
        protected void finalize() throws Throwable {
            latch.countDown();
            super.finalize();
        }
    }
}
