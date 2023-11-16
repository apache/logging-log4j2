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
package org.apache.logging.log4j.layout.template.json;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.layout.template.json.util.ThreadLocalRecycler;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests if logging while trying to encode an event causes {@link ThreadLocalRecycler} to incorrectly share buffers and end up overriding layout's earlier encoding work.
 *
 * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-2368">LOG4J2-2368</a>
 */
public class ThreadLocalRecyclerNestedLoggingTest {

    private static final class ThrowableLoggingInGetMessage extends RuntimeException {

        private final Logger logger;

        private ThrowableLoggingInGetMessage(final Logger logger) {
            this.logger = logger;
        }

        @Override
        public String getMessage() {
            logger.info("B");
            return "C";
        }
    }

    @Test
    @LoggerContextSource("threadLocalRecyclerNestedLogging.xml")
    public void nested_logging_should_not_pollute_thread_local(
            final LoggerContext loggerContext,
            final @Named(value = "List1") ListAppender appender1,
            final @Named(value = "List2") ListAppender appender2) {
        final Logger logger = loggerContext.getLogger(ThreadLocalRecyclerNestedLoggingTest.class);
        logger.error("A", new ThrowableLoggingInGetMessage(logger));
        final List<String> messages1 = readAppendedMessages(appender1);
        final List<String> messages2 = readAppendedMessages(appender2);
        Assertions.assertThat(messages1)
                .containsExactlyInAnyOrderElementsOf(messages2)
                .containsExactlyInAnyOrderElementsOf(Stream.of("['B',null]", "['A','C']")
                        .map(json -> json.replaceAll("'", "\""))
                        .collect(Collectors.toList()));
    }

    private static List<String> readAppendedMessages(final ListAppender appender) {
        return appender.getData().stream()
                .map(messageBytes -> new String(messageBytes, StandardCharsets.UTF_8))
                .collect(Collectors.toList());
    }
}
