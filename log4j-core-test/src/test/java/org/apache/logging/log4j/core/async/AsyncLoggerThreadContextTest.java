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
package org.apache.logging.log4j.core.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.test.TestProperties;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.apache.logging.log4j.test.junit.UsingTestProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("AsyncLoggers")
@UsingTestProperties
@UsingStatusListener
public class AsyncLoggerThreadContextTest {

    private static TestProperties props;

    @TempLoggingDir
    private static Path loggingPath;

    @BeforeAll
    public static void beforeClass() {
        props.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
    }

    @Test
    @LoggerContextSource("AsyncLoggerThreadContextTest.xml")
    public void testAsyncLogWritesToLog() throws Exception {
        final Path file = loggingPath.resolve("AsyncLoggerTest.log");

        ThreadContext.push("stackvalue");
        ThreadContext.put("KEY", "mapvalue");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String msg = "Async logger msg";
        log.info(msg, new InternalError("this is not a real error"));
        CoreLoggerContexts.stopLoggerContext(false, file.toFile()); // stop async thread

        final BufferedReader reader = Files.newBufferedReader(file);
        final String line1 = reader.readLine();
        reader.close();
        Files.delete(file);
        assertThat(line1).contains(msg, "mapvalue", "stackvalue");
    }
}
