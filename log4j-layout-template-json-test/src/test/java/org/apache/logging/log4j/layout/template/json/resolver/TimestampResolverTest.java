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
package org.apache.logging.log4j.layout.template.json.resolver;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TimestampResolverTest {

    /**
     * @see <a href="https://issues.apache.org/jira/browse/LOG4J2-3183">LOG4J2-3183</a>
     */
    @Test
    void epoch_nanos_should_not_overlap() {

        // Create the template.
        final Object eventTemplate = asMap("$resolver", "timestamp", "epoch", asMap("unit", "nanos"));

        // Create the logging context.
        withContextFromTemplate("TimestampResolverTest", eventTemplate, (loggerContext, appender) -> {

            // Log some.
            final Logger logger = loggerContext.getLogger(TimestampResolverTest.class);
            final int logEventCount = 5;
            for (int logEventIndex = 0; logEventIndex < logEventCount; logEventIndex++) {
                if (logEventIndex > 0) {
                    uncheckedSleep(1);
                }
                logger.info("message #{}", logEventIndex);
            }

            // Read logged events.
            final List<Long> logEvents = appender.getData().stream()
                    .map(jsonBytes -> {
                        final String json = new String(jsonBytes, StandardCharsets.UTF_8);
                        return (long) readJson(json);
                    })
                    .collect(Collectors.toList());

            // Verify logged events.
            Assertions.assertThat(logEvents).hasSize(logEventCount).doesNotHaveDuplicates();
        });
    }
}
