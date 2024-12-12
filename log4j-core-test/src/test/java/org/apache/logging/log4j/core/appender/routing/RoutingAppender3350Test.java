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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.CleanFiles;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@LoggerContextSource("log4j-routing3350.xml")
public class RoutingAppender3350Test {
    private static final String LOG_FILE = "target/tmp/test.log";

    private LoggerContext context = null;

    RoutingAppender3350Test(LoggerContext context) {
        this.context = context;
    }

    @RegisterExtension
    CleanFiles cleanFiles = new CleanFiles(false, true, 10, LOG_FILE);

    @AfterEach
    public void tearDown() throws Exception {
        this.context.stop();
    }

    @Test
    public void routingTest(final LoggerContext loggerContext) throws IOException {
        final String expected = "expectedValue";
        final StringMapMessage message = new StringMapMessage().with("data", expected);
        final Logger logger = loggerContext.getLogger(getClass());
        logger.error(message);
        final File file = new File(LOG_FILE);
        try (final InputStream inputStream = new FileInputStream(file);
                final InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                final BufferedReader reader = new BufferedReader(streamReader)) {
            final String actual = reader.readLine();
            assertEquals(expected, actual);
        }
    }
}
