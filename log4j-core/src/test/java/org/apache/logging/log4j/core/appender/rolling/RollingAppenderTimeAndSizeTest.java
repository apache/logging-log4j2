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
package org.apache.logging.log4j.core.appender.rolling;

import java.io.File;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.apache.logging.log4j.junit.FileMatchers.exists;
import static org.apache.logging.log4j.junit.FileMatchers.hasFiles;
import static org.apache.logging.log4j.junit.FileMatchers.hasName;
import static org.apache.logging.log4j.junit.FileMatchers.that;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.*;

/**
 *
 */
public class RollingAppenderTimeAndSizeTest {

    private static final String DIR = "target/rolling3/test";

    @Rule
    public InitialLoggerContext init = new InitialLoggerContext("log4j-rolling3.xml");

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        this.logger = this.init.getLogger(RollingAppenderTimeAndSizeTest.class.getName());
        deleteDir();
    }

    @After
    public void tearDown() throws Exception {
        deleteDir();
    }

    @Test
    public void testAppender() throws Exception {
        for (int i=0; i < 100; ++i) {
            logger.debug("This is test message number " + i);
        }
        Thread.sleep(50);
        final File dir = new File(DIR);
        assertThat(dir, both(exists()).and(hasFiles()));
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(".gz"))))));
    }

    private static void deleteDir() {
        final File dir = new File(DIR);
        if (dir.exists()) {
            final File[] files = dir.listFiles();
            for (final File file : files) {
                file.delete();
            }
            dir.delete();
        }
    }
}
