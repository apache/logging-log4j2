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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.layout.template.json.util.JsonReader;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class MessageResolverTest {

    /**
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-3080">LOG4J2-3080</a>
     */
    @Test
    @LoggerContextSource("messageFallbackKeyUsingJsonTemplateLayout.xml")
    void log4j1_logger_calls_should_use_fallbackKey(
            final @Named(value = "List") ListAppender appender) {

        // Log using legacy Log4j 1 API.
        final String log4j1Message =
                "Message logged using org.apache.log4j.Category.info(Object)";
        org.apache.log4j.LogManager
                .getLogger(MessageResolverTest.class)
                .info(log4j1Message);

        // Log using Log4j 2 API.
        final String log4j2Message =
                "Message logged using org.apache.logging.log4j.Logger.info(String)";
        org.apache.logging.log4j.LogManager
                .getLogger(MessageResolverTest.class)
                .info(log4j2Message);

        // Collect and parse logged messages.
        final List<Object> actualLoggedEvents = appender
                .getData()
                .stream()
                .map(jsonBytes -> {
                    final String json = new String(jsonBytes, StandardCharsets.UTF_8);
                    return JsonReader.read(json);
                })
                .collect(Collectors.toList());

        // Verify logged messages.
        final List<Object> expectedLoggedEvents = Stream
                .of(log4j1Message, log4j2Message)
                .map(message -> Collections.singletonMap(
                        "message", Collections.singletonMap(
                                "fallback", message)))
                .collect(Collectors.toList());
        Assertions.assertThat(actualLoggedEvents).isEqualTo(expectedLoggedEvents);

    }

}
