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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.apache.logging.log4j.core.helpers.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AsyncLoggerTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR,
                AsyncLoggerContextSelector.class.getName());
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "AsyncLoggerTest.xml");
    }

    @AfterClass
    public static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, "");
    }

    @Test
    public void testAsyncLogWritesToLog() throws Exception {
        File f = new File("target", "AsyncLoggerTest.log");
        // System.out.println(f.getAbsolutePath());
        f.delete();
        Logger log = LogManager.getLogger("com.foo.Bar");
        String msg = "Async logger msg";
        log.info(msg);
        ((LifeCycle) LogManager.getContext()).stop(); // stop async thread

        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line1 = reader.readLine();
        reader.close();
        f.delete();
        assertNotNull("line1", line1);
        assertTrue("line1 correct", line1.contains(msg));

        String location = "testAsyncLogWritesToLog";
        assertTrue("no location", !line1.contains(location));
    }

}
