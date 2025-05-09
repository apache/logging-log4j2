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
package org.apache.logging.log4j.core.appender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.CleanFiles;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests https://issues.apache.org/jira/browse/LOG4J2-1502
 */
@Tag("Layouts.Csv")
public class CsvJsonParameterLayoutFileAppenderTest {

    private static final String FILE_PATH = "target/CsvJsonParameterLayoutFileAppenderTest.log";
    private final String CONFIG = "log4j-cvs-json-parameter.xml";

    private LoggerContext loggerContext;

    @RegisterExtension
    CleanFiles cleanFiles = new CleanFiles(FILE_PATH);

    private void testNoNulCharacters(final String message, final String expected) throws IOException {
        @SuppressWarnings("resource")
        final Logger logger = loggerContext.getLogger("com.example");
        logger.error("log:", message);
        loggerContext.stop();
        final File file = new File(FILE_PATH);
        final byte[] contents = Files.toByteArray(file);
        int count0s = 0;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contents.length; i++) {
            final byte b = contents[i];
            if (b == 0) {
                sb.append(i);
                sb.append(", ");
                count0s++;
            }
        }
        assertEquals(0, count0s, "File contains " + count0s + " 0x00 byte at indices " + sb);
        final List<String> readLines = Files.readLines(file, Charset.defaultCharset());
        final String actual = readLines.get(0);
        // assertTrue(actual, actual.contains(message));
        assertEquals(expected, actual, actual);
        assertEquals(1, readLines.size());
    }

    @Test
    @LoggerContextSource(CONFIG)
    public void testNoNulCharactersDoubleQuote(LoggerContext context) throws IOException {
        this.loggerContext = context;
        // TODO This does not seem right but there is no NULs. Check Apache Commons CSV.
        testNoNulCharacters("\"", "\"\"\"\"");
    }

    @Test
    @LoggerContextSource(CONFIG)
    public void testNoNulCharactersJson(LoggerContext context) throws IOException {
        this.loggerContext = context;
        testNoNulCharacters("{\"id\":10,\"name\":\"Alice\"}", "\"{\"\"id\"\":10,\"\"name\"\":\"\"Alice\"\"}\"");
    }

    @Test
    @LoggerContextSource(CONFIG)
    public void testNoNulCharactersOneChar(LoggerContext context) throws IOException {
        this.loggerContext = context;
        testNoNulCharacters("A", "A");
    }

    @Test
    @LoggerContextSource(CONFIG)
    public void testNoNulCharactersOpenCurly(LoggerContext context) throws IOException {
        this.loggerContext = context;
        testNoNulCharacters("{", "{");
    }

    @Test
    @LoggerContextSource(CONFIG)
    public void testNoNulCharactersOpenParen(LoggerContext context) throws IOException {
        this.loggerContext = context;
        testNoNulCharacters("(", "(");
    }

    @Test
    @LoggerContextSource(CONFIG)
    public void testNoNulCharactersOpenSquare(LoggerContext context) throws IOException {
        this.loggerContext = context;
        // TODO Why is the char quoted? Check Apache Commons CSV.
        testNoNulCharacters("[", "[");
    }

    @Test
    @LoggerContextSource(CONFIG)
    public void testNoNulCharactersThreeChars(LoggerContext context) throws IOException {
        this.loggerContext = context;
        testNoNulCharacters("ABC", "ABC");
    }

    @Test
    @LoggerContextSource(CONFIG)
    public void testNoNulCharactersXml(LoggerContext context) throws IOException {
        this.loggerContext = context;
        testNoNulCharacters(
                "<test attr1='val1' attr2=\"value2\">X</test>", "\"<test attr1='val1' attr2=\"\"value2\"\">X</test>\"");
    }
}
