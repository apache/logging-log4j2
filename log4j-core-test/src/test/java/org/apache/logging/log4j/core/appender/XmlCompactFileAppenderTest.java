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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests a "compact" XML file, no extra spaces or end of lines.
 */
@Tag("Layouts.Xml")
class XmlCompactFileAppenderTest {

    @BeforeAll
    static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "XmlCompactFileAppenderTest.xml");
    }

    @Test
    void testFlushAtEndOfBatch() throws Exception {
        final File file = new File("target", "XmlCompactFileAppenderTest.log");
        file.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String logMsg = "Message flushed with immediate flush=false";
        log.info(logMsg);
        CoreLoggerContexts.stopLoggerContext(false, file); // stop async thread

        String line1;
        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
            line1 = reader.readLine();
        } finally {
            file.delete();
        }
        assertNotNull(line1, "line1");
        final String msg1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        assertTrue(line1.contains(msg1), "line1 incorrect: [" + line1 + "], does not contain: [" + msg1 + ']');

        final String msg2 = "<Events xmlns=\"http://logging.apache.org/log4j/2.0/events\">";
        assertTrue(line1.contains(msg2), "line1 incorrect: [" + line1 + "], does not contain: [" + msg2 + ']');

        final String msg3 = "<Event ";
        assertTrue(line1.contains(msg3), "line1 incorrect: [" + line1 + "], does not contain: [" + msg3 + ']');

        final String msg4 = logMsg;
        assertTrue(line1.contains(msg4), "line1 incorrect: [" + line1 + "], does not contain: [" + msg4 + ']');

        final String location = "testFlushAtEndOfBatch";
        assertFalse(line1.contains(location), "no location");

        assertEquals(-1, line1.indexOf('\r'));
        assertEquals(-1, line1.indexOf('\n'));
    }
}
