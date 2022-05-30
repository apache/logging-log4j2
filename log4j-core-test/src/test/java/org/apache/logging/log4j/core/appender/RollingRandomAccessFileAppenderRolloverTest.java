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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Tag("sleepy")
@SetSystemProperty(key = "log4j.configurationFile", value = "RollingRandomAccessFileAppenderTest.xml")
public class RollingRandomAccessFileAppenderRolloverTest {

    @Test
    @Disabled
    public void testRollover() throws Exception {
        final File file = new File("target", "RollingRandomAccessFileAppenderTest.log");
        // System.out.println(f.getAbsolutePath());
        final File after1 = new File("target", "afterRollover-1.log");
        file.delete();
        after1.delete();

        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String msg = "First a short message that does not trigger rollover";
        log.info(msg);
        Thread.sleep(50);

        BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        assertTrue(line1.contains(msg));
        reader.close();

        assertFalse(after1.exists(), "afterRollover-1.log not created yet");

        String exceed = "Long message that exceeds rollover size... ";
        final char[] padding = new char[250];
        Arrays.fill(padding, 'X');
        exceed += new String(padding);
        log.warn(exceed);
        assertFalse(after1.exists(), "exceeded size but afterRollover-1.log not created yet");

        final String trigger = "This message triggers rollover.";
        log.warn(trigger);
        Thread.sleep(100);
        log.warn(trigger);

        CoreLoggerContexts.stopLoggerContext(); // stop async thread
        CoreLoggerContexts.stopLoggerContext(false); // stop async thread

        final int MAX_ATTEMPTS = 50;
        int count = 0;
        while (!after1.exists() && count++ < MAX_ATTEMPTS) {
            Thread.sleep(50);
        }

        assertTrue(after1.exists(), "afterRollover-1.log created");

        reader = new BufferedReader(new FileReader(file));
        final String new1 = reader.readLine();
        assertTrue(new1.contains(trigger), "after rollover only new msg");
        assertNull(reader.readLine(), "No more lines");
        reader.close();
        file.delete();

        reader = new BufferedReader(new FileReader(after1));
        final String old1 = reader.readLine();
        assertTrue(old1.contains(msg), "renamed file line 1");
        final String old2 = reader.readLine();
        assertTrue(old2.contains(exceed), "renamed file line 2");
        String line = reader.readLine();
        if (line != null) {
            assertTrue(line.contains("This message triggers rollover."), "strange...");
            line = reader.readLine();
        }
        assertNull(line, "No more lines");
        reader.close();
        after1.delete();
    }
}
