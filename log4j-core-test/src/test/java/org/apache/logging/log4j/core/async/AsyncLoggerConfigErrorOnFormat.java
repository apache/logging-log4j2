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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.message.Message;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("AsyncLoggers")
public class AsyncLoggerConfigErrorOnFormat {

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("log4j2.enableThreadlocals", "true");
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "AsyncLoggerConfigErrorOnFormat.xml");
        // Log4jLogEvent.toString invokes message.toString
        System.setProperty("log4j2.logEventFactory", DefaultLogEventFactory.class.getName());
    }

    @AfterAll
    public static void afterClass() {
        System.clearProperty("log4j2.enableThreadlocals");
        System.clearProperty("log4j2.logEventFactory");
    }

    @Test
    public void testError() throws Exception {
        final File file = new File("target", "AsyncLoggerConfigErrorOnFormat.log");
        assertTrue(!file.exists() || file.delete(), "Deleted old file before test");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        log.info(new ThrowsErrorOnFormatMessage());
        log.info("Second message");
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        final String line2 = reader.readLine();
        reader.close();
        file.delete();

        assertThat(line1, containsString("Second message"));
        assertNull(line2, "Expected only one line");
    }

    @AsynchronouslyFormattable // Shouldn't call getFormattedMessage early
    private static final class ThrowsErrorOnFormatMessage implements Message {

        @Override
        public String getFormattedMessage() {
            throw new Error(
                    "getFormattedMessage invoked on " + Thread.currentThread().getName());
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
