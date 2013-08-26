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

public class RandomAccessFileAppenderLocationTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "RandomAccessFileAppenderLocationTest.xml");
    }

    @Test
    public void testLocationIncluded() throws Exception {
        final File f = new File("target", "RandomAccessFileAppenderLocationTest.log");
        // System.out.println(f.getAbsolutePath());
        f.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String msg = "Message with location, flushed with immediate flush=false";
        log.info(msg);
        ((LifeCycle) LogManager.getContext()).stop(); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(f));
        final String line1 = reader.readLine();
        reader.close();
        f.delete();
        assertNotNull("line1", line1);
        assertTrue("line1 correct", line1.contains(msg));

        final String location = "testLocationIncluded";
        assertTrue("has location", line1.contains(location));
    }
}
