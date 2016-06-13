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

import java.io.ByteArrayInputStream;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class OnStartupTriggeringPolicyTest {

    private static final String TARGET_FILE = "target/testfile";
    private static final String TARGET_PATTERN = "target/rolling1/test1-%i.log";
    private static final String ROLLED_FILE = "target/rolling1/test1-1.log";
    private static final String TEST_DATA = "Hello world!";

    @Test
    public void testPolicy() throws Exception {
        Configuration configuration = new DefaultConfiguration();
        Path target = Paths.get(TARGET_FILE);
        Path rolled = Paths.get(ROLLED_FILE);
        Files.deleteIfExists(target);
        Files.deleteIfExists(rolled);
        InputStream is = new ByteArrayInputStream(TEST_DATA.getBytes("UTF-8"));
        Files.copy(is, target);
        is.close();
        long size = Files.size(target);
        assertTrue(size > 0);
        final PatternLayout layout = PatternLayout.newBuilder().withPattern("%msg")
                .withConfiguration(configuration).build();
        RolloverStrategy strategy = DefaultRolloverStrategy.createStrategy(null, null, null, "0", null, true,
                configuration);
        OnStartupTriggeringPolicy policy = OnStartupTriggeringPolicy.createPolicy();
        RollingFileManager manager = RollingFileManager.getFileManager(TARGET_FILE, TARGET_PATTERN, true, false,
                policy, strategy, null, layout, 8192, true);
        manager.initialize();
        assertTrue(Files.exists(target));
        assertTrue(Files.size(target) == 0);
        assertTrue(Files.exists(rolled));
        assertTrue(Files.size(rolled) == size);

    }

}
