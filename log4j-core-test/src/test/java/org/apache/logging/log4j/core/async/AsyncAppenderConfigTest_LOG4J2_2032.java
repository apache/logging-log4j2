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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.impl.ReusableLogEventFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.apache.logging.log4j.plugins.SingletonFactory;
import org.apache.logging.log4j.test.junit.CleanUpFiles;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("async")
public class AsyncAppenderConfigTest_LOG4J2_2032 {

    @SingletonFactory
    LogEventFactory logEventFactory(final ReusableLogEventFactory factory) {
        return factory;
    }

    @SingletonFactory
    MessageFactory messageFactory(final ReusableMessageFactory factory) {
        return factory;
    }

    @Test
    @CleanUpFiles("target/AsyncAppenderConfigTest-LOG4J2-2032.log")
    @LoggerContextSource("AsyncAppenderConfigTest-LOG4J2-2032.xml")
    public void doNotProcessPlaceholdersTwice() throws Exception {
        final File file = new File("target", "AsyncAppenderConfigTest-LOG4J2-2032.log");
        assertTrue(!file.exists() || file.delete(), "Deleted old file before test");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        log.info("Text containing curly braces: {}", "Curly{}");
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final String line1 = reader.readLine();
            System.out.println(line1);
            assertTrue(line1.contains(" Text containing curly braces: Curly{} "), "line1 correct");
        }
    }

}
