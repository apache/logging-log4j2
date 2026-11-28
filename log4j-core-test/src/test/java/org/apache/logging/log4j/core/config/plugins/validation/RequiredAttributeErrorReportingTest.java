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
package org.apache.logging.log4j.core.config.plugins.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

/**
 * Tests that a fatal plugin validation error does not produce follow-up errors for attributes and
 * child elements that were never examined.
 *
 * @see <a href="https://github.com/apache/logging-log4j2/issues/4488">#4488</a>
 */
@UsingStatusListener
class RequiredAttributeErrorReportingTest {

    @Test
    @LoggerContextSource("log4j2-4488-missing-required.xml")
    void reportsOnlyTheValidationErrors(final ListStatusListener listener) {
        final List<String> errors = listener.findStatusData(Level.ERROR)
                .map(statusData -> statusData.getMessage().getFormattedMessage())
                .toList();

        assertThat(errors)
                .anyMatch(message -> message.contains("Validation failed for fileName"))
                .anyMatch(message -> message.contains("Validation failed for text"))
                .anyMatch(message -> message.contains("Validation failed for ref"))
                .contains("Console contains an invalid element or attribute \"bogusAttr\"");
        assertThat(errors)
                .contains("Skipping null child object with name File in element Appenders")
                .contains("Skipping null child object with name AppenderRef in element Logger");
        assertThat(errors)
                .filteredOn(message -> message.startsWith("Could not configure plugin element"))
                .hasSize(3);
        assertThat(errors)
                .filteredOn(message -> message.startsWith("File contains")
                        || message.startsWith("StringMatchFilter contains")
                        || message.startsWith("AppenderRef contains"))
                .isEmpty();
        assertThat(errors)
                .filteredOn(message -> message.contains("has no field or parameter that matches element"))
                .isEmpty();
    }

    @Test
    @LoggerContextSource("log4j2-4488-valid.xml")
    void validConfigurationDoesNotReportErrors(final Configuration configuration, final ListStatusListener listener) {
        assertThat(configuration.getAppenders()).containsKeys("File", "Host", "Console");
        assertThat(listener.findStatusData(Level.ERROR)).isEmpty();
    }
}
