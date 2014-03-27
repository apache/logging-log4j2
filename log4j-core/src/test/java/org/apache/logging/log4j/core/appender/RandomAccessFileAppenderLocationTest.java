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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.junit.Rule;
import org.junit.Test;

public class RandomAccessFileAppenderLocationTest {

    private final File logFile = new File("target", "RandomAccessFileAppenderLocationTest.log");

    @Rule
    public InitialLoggerContext init = new InitialLoggerContext("RandomAccessFileAppenderLocationTest.xml");

    @Rule
    public CleanFiles files = new CleanFiles(logFile);

    @Test
    public void testLocationIncluded() throws Exception {
        final Logger log = init.getLogger("com.foo.Bar");
        final String msg = "Message with location, flushed with immediate flush=false";
        log.info(msg);
        init.getContext().stop(); // stop async thread

        String line1;
        final BufferedReader reader = new BufferedReader(new FileReader(logFile));
        try {
            line1 = reader.readLine();
        } finally {
            reader.close();
        }

        assertNotNull("line1", line1);
        assertTrue("line1 correct", line1.contains(msg));

        final String location = "testLocationIncluded";
        assertTrue("has location", line1.contains(location));
    }
}
