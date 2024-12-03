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

import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(Tags.ASYNC_LOGGERS)
class AsyncWaitStrategyFactoryConfigTest {

    @Test
    @LoggerContextSource("AsyncWaitStrategyFactoryConfigTest.xml")
    void testConfigWaitStrategyFactory(final LoggerContext context) {
        final AsyncWaitStrategyFactory asyncWaitStrategyFactory =
                context.getConfiguration().getAsyncWaitStrategyFactory();
        assertEquals(YieldingWaitStrategyFactory.class, asyncWaitStrategyFactory.getClass());
        assertThat(
                "factory is YieldingWaitStrategyFactory",
                asyncWaitStrategyFactory instanceof YieldingWaitStrategyFactory);
    }

    @Test
    @LoggerContextSource("AsyncWaitStrategyFactoryConfigTest.xml")
    void testWaitStrategy(final LoggerContext context) {

        final org.apache.logging.log4j.Logger logger = context.getRootLogger();

        final AsyncLoggerConfig loggerConfig =
                (AsyncLoggerConfig) ((org.apache.logging.log4j.core.Logger) logger).get();
        final AsyncLoggerConfigDisruptor delegate =
                (AsyncLoggerConfigDisruptor) loggerConfig.getAsyncLoggerConfigDelegate();
        assertEquals(YieldingWaitStrategy.class, delegate.getWaitStrategy().getClass());
        assertThat(
                "waitstrategy is YieldingWaitStrategy",
                delegate.getWaitStrategy() instanceof com.lmax.disruptor.YieldingWaitStrategy);
    }

    @Test
    @LoggerContextSource("AsyncWaitStrategyIncorrectFactoryConfigTest.xml")
    void testIncorrectConfigWaitStrategyFactory(final LoggerContext context) {
        final AsyncWaitStrategyFactory asyncWaitStrategyFactory =
                context.getConfiguration().getAsyncWaitStrategyFactory();
        assertNull(asyncWaitStrategyFactory);
    }

    @Test
    @LoggerContextSource("AsyncWaitStrategyIncorrectFactoryConfigTest.xml")
    void testIncorrectWaitStrategyFallsBackToDefault(
            @Named("WaitStrategyAppenderList") final ListAppender list1, final LoggerContext context) {
        final org.apache.logging.log4j.Logger logger = context.getRootLogger();

        final AsyncLoggerConfig loggerConfig =
                (AsyncLoggerConfig) ((org.apache.logging.log4j.core.Logger) logger).get();
        final AsyncLoggerConfigDisruptor delegate =
                (AsyncLoggerConfigDisruptor) loggerConfig.getAsyncLoggerConfigDelegate();

        if (DisruptorUtil.DISRUPTOR_MAJOR_VERSION == 3) {
            assertEquals(
                    org.apache.logging.log4j.core.async.TimeoutBlockingWaitStrategy.class,
                    delegate.getWaitStrategy().getClass());
            assertThat(
                    "waitstrategy is TimeoutBlockingWaitStrategy",
                    delegate.getWaitStrategy()
                            instanceof org.apache.logging.log4j.core.async.TimeoutBlockingWaitStrategy);
        } else if (DisruptorUtil.DISRUPTOR_MAJOR_VERSION == 4) {
            assertEquals(
                    com.lmax.disruptor.TimeoutBlockingWaitStrategy.class,
                    delegate.getWaitStrategy().getClass());
            assertThat(
                    "waitstrategy is TimeoutBlockingWaitStrategy",
                    delegate.getWaitStrategy() instanceof com.lmax.disruptor.TimeoutBlockingWaitStrategy);
        } else {
            fail("Unhandled Disruptor version " + DisruptorUtil.DISRUPTOR_MAJOR_VERSION);
        }
    }

    public static class YieldingWaitStrategyFactory implements AsyncWaitStrategyFactory {
        @Override
        public WaitStrategy createWaitStrategy() {
            return new YieldingWaitStrategy();
        }
    }
}
