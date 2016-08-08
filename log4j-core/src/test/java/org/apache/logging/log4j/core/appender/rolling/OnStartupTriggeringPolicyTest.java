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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.junit.Test;

/**
 *
 */
public class OnStartupTriggeringPolicyTest {

    private static final String TARGET_FILE = "target/rollOnStartup/testfile";
    private static final String TARGET_PATTERN = "target/rollOnStartup/test1-%d{MM-dd-yyyy}-%i.log";
    private static final String ROLLED_FILE_PREFIX = "target/rollOnStartup/test1-";
    private static final String ROLLED_FILE_SUFFIX = "-1.log";
    private static final String TEST_DATA = "Hello world!";
    private static final FastDateFormat formatter = FastDateFormat.getInstance("MM-dd-yyyy");

    // @Rule
    // public CleanFolders rule = new CleanFolders("target/rollOnStartup");

    @Test
    public void testPolicy() throws Exception {
        final Configuration configuration = new DefaultConfiguration();
        final Path target = Paths.get(TARGET_FILE);
        target.toFile().getParentFile().mkdirs();
        final long timeStamp = System.currentTimeMillis() - (1000 * 60 * 60 * 24);
        final String expectedDate = formatter.format(timeStamp);
        final String rolledFileName = ROLLED_FILE_PREFIX + expectedDate + ROLLED_FILE_SUFFIX;
        final Path rolled = Paths.get(rolledFileName);
        try (final InputStream is = new ByteArrayInputStream(TEST_DATA.getBytes("UTF-8"))) {
            Files.copy(is, target);
        }
        final long size = Files.size(target);
        assertTrue(size > 0);

        target.toFile().setLastModified(timeStamp);
        final PatternLayout layout = PatternLayout.newBuilder().withPattern("%msg").withConfiguration(configuration)
                .build();
        final RolloverStrategy strategy = DefaultRolloverStrategy.createStrategy(null, null, null, "0", null, true,
                configuration);
        final OnStartupTriggeringPolicy policy = OnStartupTriggeringPolicy.createPolicy(1);
        final RollingFileManager manager = RollingFileManager.getFileManager(TARGET_FILE, TARGET_PATTERN, true, false,
                policy, strategy, null, layout, 8192, true, false);
        try {
            manager.initialize();
            assertTrue(Files.exists(target));
            assertTrue(Files.size(target) == 0);
            assertTrue(Files.exists(rolled));
            assertTrue(Files.size(rolled) == size);
        } finally {
            manager.release();
        }
    }

}
