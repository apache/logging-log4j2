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

import static org.apache.logging.log4j.async.logger.QueueFullAsyncAbstractTest.assertAsyncLogger;
import static org.apache.logging.log4j.core.test.TestConstants.ASYNC_FORMAT_MESSAGES_IN_BACKGROUND;
import static org.apache.logging.log4j.core.test.TestConstants.ASYNC_LOGGER_RING_BUFFER_SIZE;
import static org.apache.logging.log4j.core.test.TestConstants.ASYNC_QUEUE_FULL_POLICY_CLASS_NAME;
import static org.apache.logging.log4j.core.test.internal.GcHelper.awaitGarbageCollection;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncQueueFullPolicyFactory;
import org.apache.logging.log4j.core.async.DiscardingAsyncQueueFullPolicy;
import org.apache.logging.log4j.core.impl.CoreProperties;
import org.apache.logging.log4j.core.test.async.BlockingAppender;
import org.apache.logging.log4j.core.test.async.QueueFullAbstractTest;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;

/**
 * Tests queue full scenarios with pure AsyncLoggers (all loggers async).
 */
@ContextSelectorType(AsyncLoggerContextSelector.class)
@SetTestProperty(key = ASYNC_LOGGER_RING_BUFFER_SIZE, value = "128")
@SetTestProperty(key = ASYNC_FORMAT_MESSAGES_IN_BACKGROUND, value = "true")
@SetTestProperty(key = ASYNC_QUEUE_FULL_POLICY_CLASS_NAME, value = "Discard")
class QueueFullAsyncLogger3Test extends QueueFullAbstractTest {

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
    @LoggerContextSource
    void discarded_messages_should_be_garbage_collected(
            final LoggerContext ctx, final @Named(APPENDER_NAME) BlockingAppender blockingAppender)
            throws InterruptedException {
        awaitGarbageCollection(() -> {
            checkConfig(ctx);
            final Logger logger = ctx.getLogger(getClass());
            blockingAppender.logEvents = null;
            blockingAppender.countDownLatch = new CountDownLatch(1);
            final List<Message> messages = IntStream.range(0, 200)
                    .mapToObj(messageIndex -> new SimpleMessage("message " + messageIndex))
                    .collect(Collectors.toList());
            messages.forEach(logger::info);
            blockingAppender.countDownLatch.countDown();
            return messages;
        });
    }
}
