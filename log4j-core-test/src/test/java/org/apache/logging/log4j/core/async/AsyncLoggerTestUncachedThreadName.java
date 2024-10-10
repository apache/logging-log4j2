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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("AsyncLoggers")
public class AsyncLoggerTestUncachedThreadName {

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("AsyncLogger.ThreadNameStrategy", "UNCACHED");
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "AsyncLoggerTest.xml");
    }

    @AfterAll
    public static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
    }

    @Test
    public void testAsyncLogUsesCurrentThreadName() throws Exception {
        final File file = new File("target", "AsyncLoggerTest.log");
        // System.out.println(f.getAbsolutePath());
        file.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String msg = "Async logger msg";
        log.info(msg);
        Thread.currentThread().setName("MODIFIED-THREADNAME");
        log.info(msg);
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        final String line2 = reader.readLine();
        // System.out.println(line1);
        // System.out.println(line2);
        reader.close();
        file.delete();
        assertNotNull(line1, "line1");
        assertNotNull(line2, "line2");
        assertTrue(line1.endsWith(" INFO c.f.Bar [main]   Async logger msg "), "line1");
        assertTrue(line2.endsWith(" INFO c.f.Bar [MODIFIED-THREADNAME]   Async logger msg "), "line2");
    }
}
