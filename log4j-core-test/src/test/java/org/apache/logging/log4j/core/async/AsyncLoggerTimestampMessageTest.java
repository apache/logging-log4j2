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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirms that if you log a {@link TimestampMessage} then there are no unnecessary calls to {@link Clock}.
 * <p>
 * See LOG4J2-744.
 * </p>
 */
@Tag("async")
@ContextSelectorType(AsyncLoggerContextSelector.class)
@LoggerContextSource("AsyncLoggerTimestampMessageTest.xml")
public class AsyncLoggerTimestampMessageTest {

    @Factory
    public static Clock getClock() {
        return new PoisonClock();
    }

    @Test
    public void testAsyncLogWritesToLog(final LoggerContext context, @Named final ListAppender list)
            throws InterruptedException {
        final Logger log = context.getLogger("com.foo.Bar");
        assertThat(PoisonClock.invoked).hasValue(null);
        log.info((Message) new TimeMsg("Async logger msg with embedded timestamp", 123456789000L));
        assertThat(list.getMessages(1, 1, TimeUnit.SECONDS)).containsExactly("123456789000 Async logger msg with embedded timestamp");
        assertThat(PoisonClock.invoked).hasValue(null);
    }

    public static class PoisonClock implements Clock {
        static final AtomicReference<AssertionFailedError> invoked = new AtomicReference<>();

        @Override
        public long currentTimeMillis() {
            invoked.set(new AssertionFailedError("PoisonClock should not have been invoked"));
            return 987654321L;
        }
    }

    static class TimeMsg extends SimpleMessage implements TimestampMessage {
        private static final long serialVersionUID = 1L;
        private final long timestamp;

        public TimeMsg(final String msg, final long timestamp) {
            super(msg);
            this.timestamp = timestamp;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
}
