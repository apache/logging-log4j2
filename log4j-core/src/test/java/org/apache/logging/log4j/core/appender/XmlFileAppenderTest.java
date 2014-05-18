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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests a "complete" XML file a.k.a. a well-formed XML file.
 */
public class XmlFileAppenderTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "XmlFileAppenderTest.xml");
    }

    @Test
    public void testFlushAtEndOfBatch() throws Exception {
        final File f = new File("target", "XmlFileAppenderTest.log");
        // System.out.println(f.getAbsolutePath());
        f.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String logMsg = "Message flushed with immediate flush=false";
        log.info(logMsg);
        ((LifeCycle) LogManager.getContext()).stop(); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(f));
        String line1;
        String line2;
        String line3;
        try {
            line1 = reader.readLine();
            line2 = reader.readLine();
            line3 = reader.readLine();
        } finally {
            reader.close();
            f.delete();
        }
        assertNotNull("line1", line1);

        assertNotNull("line1", line1);
        final String msg1 = "<Event ";
        assertTrue("line1 incorrect: [" + line1 + "], does not contain: [" + msg1 + ']', line1.contains(msg1));

        assertNotNull("line2", line2);
        final String msg2 = logMsg;
        assertTrue("line2 incorrect: [" + line2 + "], does not contain: [" + msg2 + ']', line2.contains(msg2));

        assertNotNull("line3", line3);
        final String msg3 = "</Event>";
        assertTrue("line3 incorrect: [" + line3 + "], does not contain: [" + msg3 + ']', line3.contains(msg3));

        final String location = "testFlushAtEndOfBatch";
        assertTrue("no location", !line1.contains(location));
    }
}
