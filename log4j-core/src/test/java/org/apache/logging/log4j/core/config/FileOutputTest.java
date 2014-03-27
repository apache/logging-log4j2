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
package org.apache.logging.log4j.core.config;

import java.io.File;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.xml.XMLConfiguration;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.junit.*;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class FileOutputTest {

    private static final String CONFIG = "log4j-filetest.xml";
    private static final String STATUS_LOG = "target/status.log";

    @Rule
    public CleanFiles cleanFiles = new CleanFiles(STATUS_LOG);

    @Rule
    public InitialLoggerContext init = new InitialLoggerContext(CONFIG);

    @Before
    public void setupClass() {
        final LoggerContext ctx = this.init.getContext();
        final Configuration config = ctx.getConfiguration();
        if (config instanceof XMLConfiguration) {
            final String name = config.getName();
            if (name == null || !name.equals("XMLConfigTest")) {
                ctx.reconfigure();
            }
        } else {
            ctx.reconfigure();
        }
    }

    @Test
    public void testConfig() {
        final File file = new File(STATUS_LOG);
        assertTrue("Status output file does not exist", file.exists());
        assertTrue("File is empty", file.length() > 0);
    }

}
