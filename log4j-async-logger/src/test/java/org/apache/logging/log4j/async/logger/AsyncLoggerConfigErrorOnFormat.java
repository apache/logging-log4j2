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
package org.apache.logging.log4j.async.logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.TestConstants;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("async")
@SetTestProperty(
        key = TestConstants.LOGGER_CONTEXT_LOG_EVENT_FACTORY,
        value = "org.apache.logging.log4j.core.impl.DefaultLogEventFactory")
public class AsyncLoggerConfigErrorOnFormat {

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    public void testError() throws Exception {
        final File file =
                loggingPath.resolve("AsyncLoggerConfigErrorOnFormat.log").toFile();
        assertTrue(!file.exists() || file.delete(), "Deleted old file before test");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        log.info(new ThrowsErrorOnFormatMessage());
        log.info("Second message");
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        final List<String> lines = Files.readAllLines(file.toPath());
        final String line1 = lines.get(0);

        assertThat(line1, containsString("Second message"));
        assertThat(lines, hasSize(1));
    }

    @AsynchronouslyFormattable // Shouldn't call getFormattedMessage early
    private static final class ThrowsErrorOnFormatMessage implements Message {

        @Override
        public String getFormattedMessage() {
            throw new Error(
                    "getFormattedMessage invoked on " + Thread.currentThread().getName());
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return null;
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }
    }
}
