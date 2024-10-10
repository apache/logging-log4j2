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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("AsyncLoggers")
public class AsyncWaitStrategyFactoryIncorrectConfigGlobalLoggersTest {

    @BeforeAll
    public static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
        System.setProperty(
                ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "AsyncWaitStrategyIncorrectFactoryConfigGlobalLoggerTest.xml");
    }

    @AfterAll
    public static void afterClass() {
        System.clearProperty(Constants.LOG4J_CONTEXT_SELECTOR);
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
    }

    @Test
    public void testIncorrectConfigWaitStrategyFactory() throws Exception {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        assertThat("context is AsyncLoggerContext", context instanceof AsyncLoggerContext);

        final AsyncWaitStrategyFactory asyncWaitStrategyFactory =
                context.getConfiguration().getAsyncWaitStrategyFactory();
        assertNull(asyncWaitStrategyFactory);

        final AsyncLogger logger = (AsyncLogger) context.getRootLogger();
        final AsyncLoggerDisruptor delegate = logger.getAsyncLoggerDisruptor();
        if (DisruptorUtil.DISRUPTOR_MAJOR_VERSION == 3) {
            assertEquals(
                    TimeoutBlockingWaitStrategy.class,
                    delegate.getWaitStrategy().getClass());
        } else {
            assertEquals(
                    Class.forName("com.lmax.disruptor.TimeoutBlockingWaitStrategy"),
                    delegate.getWaitStrategy().getClass());
        }
    }
}
