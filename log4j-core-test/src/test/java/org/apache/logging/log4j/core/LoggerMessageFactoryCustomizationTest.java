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
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty;

class LoggerMessageFactoryCustomizationTest {

    @Test
    @ClearSystemProperty(key = "log4j2.messageFactory")
    @ClearSystemProperty(key = "log4j2.flowMessageFactory")
    void arguments_should_be_honored() {
        final LoggerContext loggerContext =
                new LoggerContext(LoggerMessageFactoryCustomizationTest.class.getSimpleName());
        final Logger logger = new Logger(
                loggerContext, "arguments_should_be_honored", new TestMessageFactory(), new TestFlowMessageFactory());
        assertTestMessageFactories(logger);
    }

    @Test
    @SetSystemProperty(
            key = "log4j2.messageFactory",
            value = "org.apache.logging.log4j.core.LoggerMessageFactoryCustomizationTest$TestMessageFactory")
    @SetSystemProperty(
            key = "log4j2.flowMessageFactory",
            value = "org.apache.logging.log4j.core.LoggerMessageFactoryCustomizationTest$TestFlowMessageFactory")
    void properties_should_be_honored() {
        final LoggerContext loggerContext =
                new LoggerContext(LoggerMessageFactoryCustomizationTest.class.getSimpleName());
        final Logger logger = new Logger(loggerContext, "properties_should_be_honored", null, null);
        assertTestMessageFactories(logger);
    }

    private static void assertTestMessageFactories(Logger logger) {
        assertThat((MessageFactory) logger.getMessageFactory()).isInstanceOf(TestMessageFactory.class);
        assertThat(logger.getFlowMessageFactory()).isInstanceOf(TestFlowMessageFactory.class);
    }

    public static final class TestMessageFactory extends AbstractMessageFactory {

        @Override
        public Message newMessage(final String message, final Object... params) {
            return ParameterizedMessageFactory.INSTANCE.newMessage(message, params);
        }
    }

    public static final class TestFlowMessageFactory extends DefaultFlowMessageFactory {}
}
