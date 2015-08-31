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

import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static org.apache.logging.log4j.hamcrest.FileMatchers.exists;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasLength;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

/**
 *
 */
public class FileOutputTest {

    private static final String CONFIG = "classpath:log4j-filetest.xml";
    private static final String STATUS_LOG = "target/status.log";

    @Rule
    public RuleChain rules = RuleChain.outerRule(new CleanFiles(STATUS_LOG)).around(new LoggerContextRule(CONFIG));

    @Test
    public void testConfig() {
        final File file = new File(STATUS_LOG);
        assertThat("Status output file does not exist", file, exists());
        assertThat("File is empty", file, hasLength(greaterThan(0L)));
    }

}
