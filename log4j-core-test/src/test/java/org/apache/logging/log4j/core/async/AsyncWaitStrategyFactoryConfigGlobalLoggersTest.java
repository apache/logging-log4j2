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
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.lmax.disruptor.YieldingWaitStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("async")
@ContextSelectorType(AsyncLoggerContextSelector.class)
@LoggerContextSource("AsyncWaitStrategyFactoryConfigGlobalLoggerTest.xml")
public class AsyncWaitStrategyFactoryConfigGlobalLoggersTest {

    @Disabled("This test succeeds when run individually but fails when run by Surefire with all other tests")
    @Test
    public void testConfigWaitStrategyAndFactory() throws Exception {
        final AsyncLogger logger = (AsyncLogger) LogManager.getLogger("com.foo.Bar");

        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        assertTrue(context instanceof AsyncLoggerContext, "context is AsyncLoggerContext");

        AsyncWaitStrategyFactory asyncWaitStrategyFactory = context.getConfiguration().getAsyncWaitStrategyFactory();
        assertEquals(AsyncWaitStrategyFactoryConfigTest.YieldingWaitStrategyFactory.class, asyncWaitStrategyFactory.getClass());
        assertTrue(asyncWaitStrategyFactory instanceof AsyncWaitStrategyFactoryConfigTest.YieldingWaitStrategyFactory, "factory is YieldingWaitStrategyFactory");

        AsyncLoggerDisruptor delegate = logger.getAsyncLoggerDisruptor();

        assertEquals(YieldingWaitStrategy.class, delegate.getWaitStrategy().getClass());
        assertTrue(delegate.getWaitStrategy() instanceof YieldingWaitStrategy, "waitstrategy is YieldingWaitStrategy");
    }
}
