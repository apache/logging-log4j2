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
import static org.mockito.Mockito.mock;

import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.MessageFactory2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class LoggerContextTest {

    @Test
    void newInstance_should_honor_name_and_message_factory(final TestInfo testInfo) {
        final String testName = testInfo.getDisplayName();
        try (final LoggerContext loggerContext = new LoggerContext(testName)) {
            final String loggerName = testName + "-loggerName";
            final MessageFactory2 messageFactory = mock(MessageFactory2.class);
            final Logger logger = loggerContext.newInstance(loggerContext, loggerName, messageFactory);
            assertThat(logger.getName()).isEqualTo(loggerName);
            assertThat((MessageFactory) logger.getMessageFactory()).isSameAs(messageFactory);
        }
    }
}
