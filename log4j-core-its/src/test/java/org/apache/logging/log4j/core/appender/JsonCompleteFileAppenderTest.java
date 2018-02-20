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
package org.apache.logging.log4j.core.appender;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.Layouts;
import org.apache.logging.log4j.core.impl.Log4jLogEventTest;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.selector.CoreContextSelectors;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests a "complete" JSON file.
 */
@RunWith(Parameterized.class)
@Category(Layouts.Json.class)
public class JsonCompleteFileAppenderTest {

    public JsonCompleteFileAppenderTest(final Class<ContextSelector> contextSelector) {
        this.loggerContextRule = new LoggerContextRule("JsonCompleteFileAppenderTest.xml", contextSelector);
        this.cleanFiles = new CleanFiles(logFile);
        this.ruleChain = RuleChain.outerRule(cleanFiles).around(loggerContextRule);
    }

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ClockFactory.PROPERTY_NAME, Log4jLogEventTest.FixedTimeClock.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(ClockFactory.PROPERTY_NAME);
    }

    @Parameters(name = "{0}")
    public static Class<?>[] getParameters() {
        return CoreContextSelectors.CLASSES;
    }

    private final File logFile = new File("target", "JsonCompleteFileAppenderTest.log");
    private final LoggerContextRule loggerContextRule;
    private final CleanFiles cleanFiles;

    @Rule
    public RuleChain ruleChain;

    @Test
    public void testFlushAtEndOfBatch() throws Exception {
        final Logger logger = this.loggerContextRule.getLogger("com.foo.Bar");
        final String logMsg = "Message flushed with immediate flush=true";
        logger.info(logMsg);
        logger.error(logMsg, new IllegalArgumentException("badarg"));
        this.loggerContextRule.getLoggerContext().stop(); // stops async thread

        List<String> lines = Files.readAllLines(logFile.toPath(), Charset.forName("UTF8"));

        String[] expected = {
                "[", // equals
                "{", // equals
                "  \"thread\" : \"main\",", //
                "  \"level\" : \"INFO\",", //
                "  \"loggerName\" : \"com.foo.Bar\",", //
                "  \"message\" : \"Message flushed with immediate flush=true\",", //
                "  \"endOfBatch\" : false,", //
                "  \"loggerFqcn\" : \"org.apache.logging.log4j.spi.AbstractLogger\",", //
                "  \"instant\" : {", //
                "    \"epochSecond\" : 1234567,", //
                "    \"nanoOfSecond\" : 890000000", //
                "  },", //
        };
        for (int i = 0; i < expected.length; i++) {
            String line = lines.get(i);
            assertTrue("line " + i + " incorrect: [" + line + "], does not contain: [" + expected[i] + ']', line.contains(expected[i]));
        }
        final String location = "testFlushAtEndOfBatch";
        assertTrue("no location", !lines.get(0).contains(location));
    }
}
