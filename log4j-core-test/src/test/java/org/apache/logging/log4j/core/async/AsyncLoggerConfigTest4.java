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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.categories.AsyncLoggers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(AsyncLoggers.class)
public class AsyncLoggerConfigTest4 {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "AsyncLoggerConfigTest4.xml");
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty("log4j2.is.webapp");
    }

    @Test
    public void testParameters() throws Exception {
        final File file = new File("target", "AsyncLoggerConfigTest4.log");
        assertTrue("Deleted old file before test", !file.exists() || file.delete());

        final Logger log = LogManager.getLogger("com.foo.Bar");
        log.info("Additive logging: {} for the price of {}!", 2, 1);
        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        final String line2 = reader.readLine();
        final String line3 = reader.readLine();
        final String line4 = reader.readLine();
        final String line5 = reader.readLine();
        reader.close();
        file.delete();

        assertThat(
                line1,
                containsString(
                        "Additive logging: {} for the price of {}! [2,1] Additive logging: 2 for the price of 1!"));
        assertThat(
                line2,
                containsString(
                        "Additive logging: {} for the price of {}! [2,1] Additive logging: 2 for the price of 1!"));
        assertThat(
                line3,
                containsString(
                        "Additive logging: {} for the price of {}! [2,1] Additive logging: 2 for the price of 1!"));
        assertThat(
                line4,
                containsString(
                        "Additive logging: {} for the price of {}! [2,1] Additive logging: 2 for the price of 1!"));
        assertNull("Expected only two lines to be logged", line5);
    }
}
