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
package org.apache.logging.log4j.status;

import static org.apache.logging.log4j.status.StatusLogger.DEFAULT_FALLBACK_LISTENER_BUFFER_CAPACITY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import uk.org.webcompere.systemstubs.SystemStubs;

class StatusLoggerBufferCapacityTest {

    @Test
    void valid_buffer_capacity_should_be_effective() {

        // Create a `StatusLogger` configuration
        final Properties statusLoggerConfigProperties = new Properties();
        final int bufferCapacity = 10;
        assertThat(bufferCapacity).isNotEqualTo(DEFAULT_FALLBACK_LISTENER_BUFFER_CAPACITY);
        statusLoggerConfigProperties.put(StatusLogger.MAX_STATUS_ENTRIES, "" + bufferCapacity);
        final StatusLogger.Config statusLoggerConfig = new StatusLogger.Config(statusLoggerConfigProperties);

        // Verify the buffer capacity
        assertThat(statusLoggerConfig.bufferCapacity).isEqualTo(bufferCapacity);
    }

    @Test
    void invalid_buffer_capacity_should_cause_fallback_to_defaults() throws Exception {

        // Create a `StatusLogger` configuration using an invalid buffer capacity
        final Properties statusLoggerConfigProperties = new Properties();
        final int invalidBufferCapacity = -10;
        statusLoggerConfigProperties.put(StatusLogger.MAX_STATUS_ENTRIES, "" + invalidBufferCapacity);
        final StatusLogger.Config[] statusLoggerConfigRef = {null};
        final String stderr = SystemStubs.tapSystemErr(
                () -> statusLoggerConfigRef[0] = new StatusLogger.Config(statusLoggerConfigProperties));
        final StatusLogger.Config statusLoggerConfig = statusLoggerConfigRef[0];

        // Verify the stderr dump
        assertThat(stderr).contains("Failed reading the buffer capacity");

        // Verify the buffer capacity
        assertThat(statusLoggerConfig.bufferCapacity).isEqualTo(DEFAULT_FALLBACK_LISTENER_BUFFER_CAPACITY);
    }
}
