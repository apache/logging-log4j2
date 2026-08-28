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
package org.apache.logging.log4j.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.message.AbstractMessageFactory;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junitpioneer.jupiter.SetSystemProperty;

@SetSystemProperty(
        key = "log4j2.messageFactory",
        value = "org.apache.logging.log4j.core.LoggerMessageFactoryCustomizationTest$AlternativeTestMessageFactory")
@SetSystemProperty(
        key = "log4j2.flowMessageFactory",
        value = "org.apache.logging.log4j.core.LoggerMessageFactoryCustomizationTest$AlternativeTestFlowMessageFactory")
class LoggerMessageFactoryCustomizationTest {

    @Test
    void arguments_should_be_honored(TestInfo testInfo) {
        try (LoggerContext loggerContext =
                new LoggerContext(LoggerMessageFactoryCustomizationTest.class.getSimpleName())) {
            Logger logger = new Logger(
                    loggerContext, testInfo.getDisplayName(), new TestMessageFactory(), new TestFlowMessageFactory());
            assertTestMessageFactories(logger, TestMessageFactory.class, TestFlowMessageFactory.class);
        }
    }

    @Test
    void properties_should_be_honored(TestInfo testInfo) {
        try (LoggerContext loggerContext =
                new LoggerContext(LoggerMessageFactoryCustomizationTest.class.getSimpleName())) {
            Logger logger = loggerContext.getLogger(testInfo.getDisplayName());
            assertTestMessageFactories(
                    logger, AlternativeTestMessageFactory.class, AlternativeTestFlowMessageFactory.class);
        }
    }

    private static void assertTestMessageFactories(
            Logger logger,
            Class<? extends MessageFactory> messageFactoryClass,
            Class<? extends FlowMessageFactory> flowMessageFactoryClass) {
        assertThat(logger.getMessageFactory().getClass()).isEqualTo(messageFactoryClass);
        assertThat(logger.getFlowMessageFactory().getClass()).isEqualTo(flowMessageFactoryClass);
    }

    public static class TestMessageFactory extends AbstractMessageFactory {

        @Override
        public Message newMessage(final String message, final Object... params) {
            return ParameterizedMessageFactory.INSTANCE.newMessage(message, params);
        }
    }

    public static class AlternativeTestMessageFactory extends TestMessageFactory {}

    public static class TestFlowMessageFactory extends DefaultFlowMessageFactory {}

    public static class AlternativeTestFlowMessageFactory extends TestFlowMessageFactory {}
}
