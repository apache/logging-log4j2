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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.apache.logging.log4j.hamcrest.FileMatchers.exists;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasFiles;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.*;

/**
 *
 */
public class RollingAppenderUncompressedTest {

    private static final String CONFIG = "log4j-rolling4.xml";
    private static final String DIR = "target/rolling4";

    org.apache.logging.log4j.Logger logger = LogManager.getLogger(RollingAppenderUncompressedTest.class.getName());

    @BeforeClass
    public static void setupClass() {
        deleteDir();
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, CONFIG);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        final Configuration config = ctx.getConfiguration();
    }

    @AfterClass
    public static void cleanupClass() {
        //deleteDir();
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        final LoggerContext ctx = (LoggerContext) LogManager.getContext();
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void testAppender() throws Exception {
        for (int i=0; i < 100; ++i) {
            logger.debug("This is test message number " + i);
        }
        final File dir = new File(DIR);
        assertThat(dir, both(exists()).and(hasFiles()));
        final File[] files = dir.listFiles();
        assertNotNull(files);
        final Matcher<File> withCorrectFileName =
            both(hasName(that(endsWith(".log")))).and(hasName(that(startsWith("test1"))));
        assertThat(files, hasItemInArray(withCorrectFileName));
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
