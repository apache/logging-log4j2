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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.TestConstants;
import org.apache.logging.log4j.test.junit.CleanUpFiles;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("async")
@SetTestProperty(
        key = TestConstants.LOGGER_CONTEXT_LOG_EVENT_FACTORY,
        value = "org.apache.logging.log4j.core.impl.ReusableLogEventFactory")
@SetTestProperty(key = TestConstants.MESSAGE_FACTORY, value = "org.apache.logging.log4j.message.ReusableMessageFactory")
@SetTestProperty(key = TestConstants.CONFIGURATION_FILE, value = "AsyncAppenderConfigTest-LOG4J2-2032.xml")
public class AsyncAppenderConfigTest_LOG4J2_2032 {

    @Test
    @CleanUpFiles("target/AsyncAppenderConfigTest-LOG4J2-2032.log")
    public void doNotProcessPlaceholdersTwice() throws Exception {
        final File file = new File("target", "AsyncAppenderConfigTest-LOG4J2-2032.log");
        assertTrue(!file.exists() || file.delete(), "Deleted old file before test");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        log.info("Text containing curly braces: {}", "Curly{}");
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            final String line1 = reader.readLine();
            System.out.println(line1);
            assertTrue(line1.contains(" Text containing curly braces: Curly{} "), "line1 correct");
        }
    }
}
