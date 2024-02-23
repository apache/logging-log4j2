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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.message.ParameterizedNoReferenceMessageFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

class StatusLoggerDateTest {

    private static final String INSTANT_YEAR = "1970";

    private static final String INSTANT_MONTH = "12";

    private static final String INSTANT_DAY = "27";

    private static final String INSTANT_HOUR = "12";

    private static final String INSTANT_MINUTE = "34";

    private static final String INSTANT_SECOND = "56";

    private static final String INSTANT_FRACTION = "789";

    private static final Instant INSTANT = Instant.parse(INSTANT_YEAR
            + '-'
            + INSTANT_MONTH
            + '-'
            + INSTANT_DAY
            + 'T'
            + INSTANT_HOUR
            + ':'
            + INSTANT_MINUTE
            + ':'
            + INSTANT_SECOND
            + '.'
            + INSTANT_FRACTION
            + 'Z');

    private static final Supplier<Instant> CLOCK = () -> INSTANT;

    @ParameterizedTest
    @CsvSource({
        "yyyy-MM-dd," + (INSTANT_YEAR + '-' + INSTANT_MONTH + '-' + INSTANT_DAY),
        "HH:mm:ss," + (INSTANT_HOUR + ':' + INSTANT_MINUTE + ':' + INSTANT_SECOND),
        "HH:mm:ss.SSS," + (INSTANT_HOUR + ':' + INSTANT_MINUTE + ':' + INSTANT_SECOND + '.' + INSTANT_FRACTION)
    })
    void common_date_patterns_should_work(final String instantPattern, final String formattedInstant) {

        // Create a `StatusLogger` configuration
        final Properties statusLoggerConfigProperties = new Properties();
        statusLoggerConfigProperties.put(StatusLogger.STATUS_DATE_FORMAT, instantPattern);
        statusLoggerConfigProperties.put(StatusLogger.STATUS_DATE_FORMAT_ZONE, "UTC");
        final StatusLogger.Config statusLoggerConfig = new StatusLogger.Config(statusLoggerConfigProperties);

        // Create a `StatusConsoleListener` recording `StatusData`
        final StatusConsoleListener statusConsoleListener = mock(StatusConsoleListener.class);
        when(statusConsoleListener.getStatusLevel()).thenReturn(Level.ALL);
        final List<StatusData> loggedStatusData = new ArrayList<>();
        doAnswer((Answer<Void>) invocation -> {
                    final StatusData statusData = invocation.getArgument(0, StatusData.class);
                    loggedStatusData.add(statusData);
                    return null;
                })
                .when(statusConsoleListener)
                .log(Mockito.any());

        // Create the `StatusLogger`
        final StatusLogger logger = new StatusLogger(
                StatusLoggerDateTest.class.getSimpleName(),
                ParameterizedNoReferenceMessageFactory.INSTANCE,
                statusLoggerConfig,
                CLOCK,
                statusConsoleListener);

        // Log a message
        final String message = "test message";
        final Level level = Level.ERROR;
        final Throwable throwable = new RuntimeException("test failure");
        logger.log(level, message, throwable);

        // Verify the logging
        assertThat(loggedStatusData).hasSize(1);
        final StatusData statusData = loggedStatusData.get(0);
        assertThat(statusData.getLevel()).isEqualTo(level);
        assertThat(statusData.getThrowable()).isSameAs(throwable);
        assertThat(statusData.getFormattedStatus())
                .matches("(?s)^" + formattedInstant + " .+ " + level + ' ' + message + ".*" + throwable.getMessage()
                        + ".*");
    }
}
