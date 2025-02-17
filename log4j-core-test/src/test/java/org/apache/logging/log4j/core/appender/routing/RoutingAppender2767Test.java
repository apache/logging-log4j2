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
package org.apache.logging.log4j.core.appender.routing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.CleanFiles;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 *
 */
@LoggerContextSource("log4j-routing-2767.xml")
public class RoutingAppender2767Test {
    private static final String ACTIVITY_LOG_FILE = "target/routing1/routingtest-Service.log";

    private LoggerContext context = null;

    RoutingAppender2767Test(LoggerContext context) {
        this.context = context;
    }

    @RegisterExtension
    CleanFiles cleanFiles = new CleanFiles(false, true, 10, ACTIVITY_LOG_FILE);

    @AfterEach
    public void tearDown() throws Exception {
        this.context.stop();
    }

    @Test
    public void routingTest() throws Exception {
        final StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        EventLogger.logEvent(msg);
        final File file = new File(ACTIVITY_LOG_FILE);
        assertTrue(file.exists(), "Activity file was not created");
        final List<String> lines = Files.lines(file.toPath()).collect(Collectors.toList());
        assertEquals(1, lines.size(), "Incorrect number of lines");
        assertTrue(lines.get(0).contains("This is a test"), "Incorrect content");
    }
}
