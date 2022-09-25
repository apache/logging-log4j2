/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.appender.routing;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.message.StringMapMessage;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class RoutingAppender3350Test {
    private static final String CONFIG = "log4j-routing3350.xml";
    private static final String LOG_FILE = "target/tmp/test.log";

    private final LoggerContextRule loggerContextRule = new LoggerContextRule(CONFIG);

    @Rule
    public RuleChain rules = loggerContextRule.withCleanFilesRule(LOG_FILE);

    @After
    public void tearDown() throws Exception {
        this.loggerContextRule.getLoggerContext().stop();
    }

    @Test
    public void routingTest() throws IOException {
        String expected = "expectedValue";
        StringMapMessage message = new StringMapMessage().with("data", expected);
        Logger logger = loggerContextRule.getLoggerContext().getLogger(getClass());
        logger.error(message);
        File file = new File(LOG_FILE);
        try (InputStream inputStream = new FileInputStream(file);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {
            String actual = reader.readLine();
            assertEquals(expected, actual);
        }
    }
}
