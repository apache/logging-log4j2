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
package org.apache.logging.log4j.core.test.junit;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Log4jEventRecorderEnabled
class Log4jEventRecorderTest {

    @Test
    void should_succeed_when_run_even_in_parallel(final Log4jEventRecorder eventRecorder) {

        // Log events
        final int eventCount = 3;//1 + (int) (Math.random() * 1000D);
        final Logger logger = eventRecorder.getLoggerContext().getLogger(Log4jEventRecorderTest.class);
        for (int eventIndex = 0; eventIndex < eventCount; eventIndex++) {
            logger.trace("test message {}", eventIndex);
        }

        // Verify logged levels
        final List<LogEvent> events = eventRecorder.getEvents();
        assertThat(events).allMatch(event -> Level.TRACE.equals(event.getLevel()));

        // Verify logged messages
        final List<String> expectedMessages = IntStream
                .range(0, eventCount)
                .mapToObj(eventIndex -> String.format("test message %d", eventIndex))
                .collect(Collectors.toList());
        final List<String> actualMessages = events
                .stream()
                .map(event -> event.getMessage().getFormattedMessage())
                .collect(Collectors.toList());
        assertThat(actualMessages).containsExactlyElementsOf(expectedMessages);

    }

}
