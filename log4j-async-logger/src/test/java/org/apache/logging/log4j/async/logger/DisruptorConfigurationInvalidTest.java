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
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.async.logger.internal.TimeoutBlockingWaitStrategy;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("async")
@ContextSelectorType(AsyncLoggerContextSelector.class)
@LoggerContextSource
public class DisruptorConfigurationInvalidTest {

    @Test
    public void testIncorrectConfigWaitStrategyFactory(final LoggerContext context) {
        assertThat(context).isInstanceOf(AsyncLoggerContext.class);

        final DisruptorConfiguration disruptorConfig =
                context.getConfiguration().getExtension(DisruptorConfiguration.class);
        assertThat(disruptorConfig).isNotNull();
        final AsyncWaitStrategyFactory asyncWaitStrategyFactory = disruptorConfig.getWaitStrategyFactory();
        assertThat(asyncWaitStrategyFactory).isNull();

        final AsyncLogger logger = (AsyncLogger) context.getRootLogger();
        final AsyncLoggerDisruptor delegate = logger.getAsyncLoggerDisruptor();
        assertEquals(
                TimeoutBlockingWaitStrategy.class, delegate.getWaitStrategy().getClass());
        assertThat(delegate.getWaitStrategy()).isInstanceOf(TimeoutBlockingWaitStrategy.class);
    }
}
