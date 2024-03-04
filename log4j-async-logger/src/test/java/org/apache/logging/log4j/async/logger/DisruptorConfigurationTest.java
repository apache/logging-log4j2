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

import static org.assertj.core.api.Assertions.assertThat;

import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import org.apache.logging.log4j.async.logger.internal.DefaultAsyncWaitStrategyFactory;
import org.apache.logging.log4j.async.logger.internal.TimeoutBlockingWaitStrategy;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("async")
public class DisruptorConfigurationTest {

    @Test
    void testAttributePriority() {
        final DisruptorConfiguration disruptorConfig = DisruptorConfiguration.newBuilder()
                .setFactoryClassName(DefaultAsyncWaitStrategyFactory.class.getName())
                .setWaitFactory(YieldingWaitStrategyFactory.class.getName())
                .build();
        assertThat(disruptorConfig.getWaitStrategyFactory()).isInstanceOf(YieldingWaitStrategyFactory.class);
    }

    @Test
    @LoggerContextSource
    public void testConfigWaitStrategyFactory(final LoggerContext context) throws Exception {
        final DisruptorConfiguration disruptorConfig =
                context.getConfiguration().getExtension(DisruptorConfiguration.class);
        final AsyncWaitStrategyFactory asyncWaitStrategyFactory = disruptorConfig.getWaitStrategyFactory();
        assertThat(asyncWaitStrategyFactory.getClass()).isEqualTo(YieldingWaitStrategyFactory.class);
        assertThat(asyncWaitStrategyFactory).isInstanceOf(YieldingWaitStrategyFactory.class);
    }

    @Test
    @LoggerContextSource
    public void testWaitStrategy(final LoggerContext context) throws Exception {

        final org.apache.logging.log4j.Logger logger = context.getRootLogger();

        final AsyncLoggerConfig loggerConfig =
                (AsyncLoggerConfig) ((org.apache.logging.log4j.core.Logger) logger).get();
        final AsyncLoggerConfigDisruptor delegate = loggerConfig.getAsyncLoggerConfigDisruptor();
        assertThat(delegate.getWaitStrategy().getClass()).isEqualTo(YieldingWaitStrategy.class);
        assertThat(delegate.getWaitStrategy()).isInstanceOf(com.lmax.disruptor.YieldingWaitStrategy.class);
    }

    @Test
    @LoggerContextSource("AsyncWaitStrategyIncorrectFactoryConfigTest.xml")
    public void testIncorrectConfigWaitStrategyFactory(final LoggerContext context) throws Exception {
        final DisruptorConfiguration disruptorConfig =
                context.getConfiguration().getExtension(DisruptorConfiguration.class);
        final AsyncWaitStrategyFactory asyncWaitStrategyFactory = disruptorConfig.getWaitStrategyFactory();
        assertThat(asyncWaitStrategyFactory).isNull(); // because invalid configuration
    }

    @Test
    @LoggerContextSource("AsyncWaitStrategyIncorrectFactoryConfigTest.xml")
    public void testIncorrectWaitStrategyFallsBackToDefault(
            @Named("WaitStrategyAppenderList") final ListAppender list1, final LoggerContext context) throws Exception {
        final org.apache.logging.log4j.Logger logger = context.getRootLogger();

        final AsyncLoggerConfig loggerConfig =
                (AsyncLoggerConfig) ((org.apache.logging.log4j.core.Logger) logger).get();
        final AsyncLoggerConfigDisruptor delegate =
                (AsyncLoggerConfigDisruptor) loggerConfig.getAsyncLoggerConfigDisruptor();
        assertThat(delegate.getWaitStrategy().getClass()).isEqualTo(TimeoutBlockingWaitStrategy.class);
        assertThat(delegate.getWaitStrategy()).isInstanceOf(TimeoutBlockingWaitStrategy.class);
    }

    public static class YieldingWaitStrategyFactory implements AsyncWaitStrategyFactory {
        @Override
        public WaitStrategy createWaitStrategy() {
            return new YieldingWaitStrategy();
        }
    }
}
