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
package org.apache.logging.log4j.core.async;

import static org.apache.logging.log4j.core.GcHelper.awaitGarbageCollection;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests queue full scenarios with pure AsyncLoggers (all loggers async).
 */
@SetTestProperty(
        key = Constants.LOG4J_CONTEXT_SELECTOR,
        value = "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
@SetTestProperty(key = "log4j2.asyncLoggerRingBufferSize", value = "128")
@SetTestProperty(key = "log4j2.formatMsgAsync", value = "true")
@SetTestProperty(key = "log4j2.asyncQueueFullPolicy", value = "Discard")
@Tag(Tags.ASYNC_LOGGERS)
class QueueFullAsyncLogger3Test extends QueueFullAbstractTest {

    @Override
    protected void checkConfig(final LoggerContext ctx) {
        assertAsyncLogger(ctx, 128);
        assertFormatMessagesInBackground();
        assertThat(AsyncQueueFullPolicyFactory.create()).isInstanceOf(DiscardingAsyncQueueFullPolicy.class);
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
