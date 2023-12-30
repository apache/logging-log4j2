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
package org.apache.logging.log4j.core.lookup;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MarkerLookup} with a configuration file.
 *
 * @since 2.4
 */
@UsingStatusListener
class MarkerLookupConfigTest {

    private static final Marker PAYLOAD = MarkerManager.getMarker("PAYLOAD");
    private static final String PAYLOAD_LOG = "Message in payload.log";

    private static final Marker PERFORMANCE = MarkerManager.getMarker("PERFORMANCE");

    private static final String PERFORMANCE_LOG = "Message in performance.log";
    private static final Marker SQL = MarkerManager.getMarker("SQL");
    private static final String SQL_LOG = "Message in sql.log";

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    void test(final LoggerContext context) throws IOException {
        final Logger logger = context.getLogger(getClass());
        logger.info(SQL, SQL_LOG);
        logger.info(PAYLOAD, PAYLOAD_LOG);
        logger.info(PERFORMANCE, PERFORMANCE_LOG);
        context.stop(1, TimeUnit.SECONDS);
        {
            final List<String> lines = Files.readAllLines(loggingPath.resolve("sql.log"));
            assertThat(lines).hasSize(1).contains(SQL_LOG);
        }
        {
            final List<String> lines = Files.readAllLines(loggingPath.resolve("payload.log"));
            assertThat(lines).hasSize(1).contains(PAYLOAD_LOG);
        }
        {
            final List<String> lines = Files.readAllLines(loggingPath.resolve("performance.log"));
            assertThat(lines).hasSize(1).contains(PERFORMANCE_LOG);
        }
    }
}
