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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MarkerLookup} with a configuration file.
 *
 * @since 2.4
 */
@LoggerContextSource("log4j-marker-lookup.yaml")
@Tag("yaml")
class MarkerLookupConfigTest {

    public static final Marker PAYLOAD = MarkerManager.getMarker("PAYLOAD");
    private static final String PAYLOAD_LOG = "Message in payload.log";

    public static final Marker PERFORMANCE = MarkerManager.getMarker("PERFORMANCE");

    private static final String PERFORMANCE_LOG = "Message in performance.log";
    public static final Marker SQL = MarkerManager.getMarker("SQL");
    private static final String SQL_LOG = "Message in sql.log";

    @Test
    void test() throws IOException {
        final Logger logger = LogManager.getLogger();
        logger.info(SQL, SQL_LOG);
        logger.info(PAYLOAD, PAYLOAD_LOG);
        logger.info(PERFORMANCE, PERFORMANCE_LOG);
        {
            final String log = FileUtils.readFileToString(new File("target/logs/sql.log"), StandardCharsets.UTF_8);
            assertTrue(log.contains(SQL_LOG));
            assertFalse(log.contains(PAYLOAD_LOG));
            assertFalse(log.contains(PERFORMANCE_LOG));
        }
        {
            final String log = FileUtils.readFileToString(new File("target/logs/payload.log"), StandardCharsets.UTF_8);
            assertFalse(log.contains(SQL_LOG));
            assertTrue(log.contains(PAYLOAD_LOG));
            assertFalse(log.contains(PERFORMANCE_LOG));
        }
        {
            final String log =
                    FileUtils.readFileToString(new File("target/logs/performance.log"), StandardCharsets.UTF_8);
            assertFalse(log.contains(SQL_LOG));
            assertFalse(log.contains(PAYLOAD_LOG));
            assertTrue(log.contains(PERFORMANCE_LOG));
        }
    }
}
