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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.ClockFactoryTest;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Confirms that if you log a {@link TimestampMessage} then there are no unnecessary calls to {@link Clock}.
 * <p>
 * See LOG4J2-744.
 * </p>
 */
@Tag(Tags.ASYNC_LOGGERS)
class AsyncLoggerTimestampMessageTest {

    @BeforeAll
    static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "AsyncLoggerTimestampMessageTest.xml");
        System.setProperty(ClockFactory.PROPERTY_NAME, PoisonClock.class.getName());
    }

    @AfterAll
    static void afterClass() throws IllegalAccessException {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
        ClockFactoryTest.resetClocks();
    }

    @Test
    void testAsyncLogWritesToLog() throws Exception {

        final File file = new File("target", "AsyncLoggerTimestampMessageTest.log");
        // System.out.println(f.getAbsolutePath());
        file.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        assertFalse(PoisonClock.called);
        log.info((Message) new TimeMsg("Async logger msg with embedded timestamp", 123456789000L));
        assertFalse(PoisonClock.called);
        CoreLoggerContexts.stopLoggerContext(false, file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        reader.close();
        file.delete();
        assertNotNull(line1);
        assertEquals("123456789000 Async logger msg with embedded timestamp", line1, "line1 correct");
    }

    public static class PoisonClock implements Clock {
        public static boolean called = false;

        @Override
        public long currentTimeMillis() {
            // throw new RuntimeException("This should not have been called");
            called = true;
            return 987654321L;
        }
    }

    static class TimeMsg extends SimpleMessage implements TimestampMessage {
        private static final long serialVersionUID = 1L;
        private final long timestamp;

        public TimeMsg(final String msg, final long timestamp) {
            super(msg);
            this.timestamp = timestamp;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }
    }
}
