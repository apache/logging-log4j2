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
package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.apache.logging.log4j.message.Message;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Category(AsyncLoggers.class)
public class AsyncLoggerConfigErrorOnFormat {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "AsyncLoggerConfigErrorOnFormat.xml");
        // Log4jLogEvent.toString invokes message.toString
        System.setProperty("log4j2.logEventFactory", DefaultLogEventFactory.class.getName());
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty("log4j2.is.webapp");
        System.clearProperty("log4j2.logEventFactory");
    }

    @Test
    public void testError() throws Exception {
        final File file = new File("target", "AsyncLoggerConfigErrorOnFormat.log");
        assertTrue("Deleted old file before test", !file.exists() || file.delete());

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
        assertNull("Expected only one line", line2);
    }

    @AsynchronouslyFormattable // Shouldn't call getFormattedMessage early
    private static final class ThrowsErrorOnFormatMessage implements Message {

        @Override
        public String getFormattedMessage() {
            throw new Error("getFormattedMessage invoked on " + Thread.currentThread().getName());
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
