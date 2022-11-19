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
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.junit.ContextSelectorType;
import org.apache.logging.log4j.test.junit.CleanUpFiles;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("async")
@ContextSelectorType(AsyncLoggerContextSelector.class)
@SetSystemProperty(key = Log4jProperties.CONFIG_LOCATION, value = "AsyncLoggerLocationTest.xml")
@CleanUpFiles("target/AsyncLoggerLocationTest.log")
public class AsyncLoggerLocationTest {

    @Test
    public void testAsyncLogWritesToLog() throws Exception {
        final File file = new File("target", "AsyncLoggerLocationTest.log");
        // System.out.println(f.getAbsolutePath());
        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String msg = "Async logger msg with location";
        log.info(msg);
        CoreLoggerContexts.stopLoggerContext(false, file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        reader.close();
        file.delete();
        assertNotNull(line1, "line1");
        assertTrue(line1.contains(msg), "line1 correct");

        final String location = "testAsyncLogWritesToLog";
        assertTrue(line1.contains(location), "has location");
    }

}
