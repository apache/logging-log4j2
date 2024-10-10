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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests a "complete" XML file a.k.a. a well-formed XML file.
 */
@Tag("Layouts.Xml")
public class XmlFileAppenderTest {

    @BeforeAll
    public static void beforeClass() {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "XmlFileAppenderTest.xml");
    }

    @Test
    public void testFlushAtEndOfBatch() throws Exception {
        final File file = new File("target", "XmlFileAppenderTest.log");
        // System.out.println(f.getAbsolutePath());
        file.delete();
        final Logger log = LogManager.getLogger("com.foo.Bar");
        final String logMsg = "Message flushed with immediate flush=false";
        log.info(logMsg);
        CoreLoggerContexts.stopLoggerContext(false, file); // stop async thread

        final List<String> lines = Files.readAllLines(file.toPath(), Charset.forName("UTF8"));
        file.delete();

        final String[] expect = {
            "", // ? unsure why initial empty line...
            "<Event ", //
            "<Instant epochSecond=", //
            logMsg, //
            "</Event>", //
        };

        for (int i = 0; i < expect.length; i++) {
            assertTrue(
                    lines.get(i).contains(expect[i]),
                    "Expected line " + i + " to contain " + expect[i] + " but got: " + lines.get(i));
        }

        final String location = "testFlushAtEndOfBatch";
        assertTrue(!lines.get(0).contains(location), "no location");
    }
}
