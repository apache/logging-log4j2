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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("async")
@SetSystemProperty(key = Log4jProperties.CONTEXT_SELECTOR_CLASS_NAME, value = "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
@SetSystemProperty(key = Log4jProperties.CONFIG_LOCATION, value = "AsyncLoggerConfigTest4.xml")
public class AsyncLoggerConfigWithAsyncEnabledTest {

    @Test
    public void testParametersAreAvailableToLayout() throws Exception {
        final File file = new File("target", "AsyncLoggerConfigTest4.log");
        assertTrue(!file.exists() || file.delete(), "Deleted old file before test");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        String format = "Additive logging: {} for the price of {}!";
        log.info(format, 2, 1);
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        final String line2 = reader.readLine();
        reader.close();
        file.delete();

        String expected = "Additive logging: {} for the price of {}! [2,1] Additive logging: 2 for the price of 1!";
        assertThat(line1, containsString(expected));
        assertThat(line2, containsString(expected));
    }
}
