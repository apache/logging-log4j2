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

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.test.junit.LoggingTestContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Tests for LoggerConfig hierarchies.
 */
@RunWith(Parameterized.class)
public class NestedLoggerConfigTest {

    @Parameterized.Parameters(name = "{0}")
    public static List<String> data() throws IOException {
        return List.of("logger-config/LoggerConfig/", "logger-config/AsyncLoggerConfig/");
    }

    private final String prefix;

    public NestedLoggerConfigTest(String prefix) {
        this.prefix = prefix;
    }

    @Test
    public void testInheritParentDefaultLevel() {
        final LoggingTestContext testContext = loadConfiguration(prefix + "default-level.xml");
        Configuration configuration = testContext.getLoggerContext().getConfiguration();
        try {
            assertEquals(Level.ERROR, configuration.getLoggerConfig("com.foo").getLevel());
        } finally {
            testContext.close();
        }
    }

    @Test
    public void testInheritParentLevel() {
        final LoggingTestContext testContext = loadConfiguration(prefix + "inherit-level.xml");
        Configuration configuration = testContext.getLoggerContext().getConfiguration();
        try {
            assertEquals(Level.TRACE, configuration.getLoggerConfig("com.foo").getLevel());
        } finally {
            testContext.close();
        }
    }

    private LoggingTestContext loadConfiguration(String resourcePath) {
        final LoggingTestContext testContext = LoggingTestContext.configurer()
                .setContextName("test")
                .setConfigurationLocation("classpath:" + resourcePath)
                .setClassLoader(getClass().getClassLoader())
                .build();
        testContext.init(null);
        return testContext;
    }
}
