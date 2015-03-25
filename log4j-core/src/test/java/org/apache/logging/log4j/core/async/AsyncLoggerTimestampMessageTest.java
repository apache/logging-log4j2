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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.CoreLoggerContexts;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.ClockFactoryTest;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.message.TimestampMessage;
import org.apache.logging.log4j.util.Strings;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Confirms that if you log a {@link TimestampMessage} then there are no unnecessary calls to {@link Clock}.
 * <p>
 * See LOG4J2-744.
 * </p>
 */
public class AsyncLoggerTimestampMessageTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                AsyncLoggerContextSelector.class.getName());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "AsyncLoggerTimestampMessageTest.xml");
        System.setProperty(ClockFactory.PROPERTY_NAME,
                PoisonClock.class.getName());
    }

    @AfterClass
    public static void afterClass() throws IllegalAccessException {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
        ClockFactoryTest.resetClocks();
    }

    @Test
    public void testAsyncLogWritesToLog() throws Exception {

        final File file = new File("target", "AsyncLoggerTimestampMessageTest.log");
        // System.out.println(f.getAbsolutePath());
        file.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        log.info(new TimeMsg("Async logger msg with embedded timestamp", 123456789000L));
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        reader.close();
        file.delete();
        assertNotNull(line1);
        assertTrue("line1 correct", line1.equals("123456789000 Async logger msg with embedded timestamp"));
    }

    public static class PoisonClock implements Clock {
        @Override
        public long currentTimeMillis() {
            throw new RuntimeException("This should not have been called");
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
