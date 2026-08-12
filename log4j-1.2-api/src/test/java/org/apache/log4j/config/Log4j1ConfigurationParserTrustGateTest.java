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
package org.apache.log4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Log4j1ConfigurationParserTrustGateTest {

    private static final String STRICTNESS_PROPERTY = "log4j2.trustgate.strictness";
    private static final String STRICT_PROPERTY = "log4j2.trustgate.strict";

    private String previousStrictnessProperty;
    private String previousStrictProperty;

    @BeforeEach
    void setUp() {
        previousStrictnessProperty = System.getProperty(STRICTNESS_PROPERTY);
        previousStrictProperty = System.getProperty(STRICT_PROPERTY);
        System.clearProperty(STRICTNESS_PROPERTY);
        System.clearProperty(STRICT_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        restoreProperty(STRICTNESS_PROPERTY, previousStrictnessProperty);
        restoreProperty(STRICT_PROPERTY, previousStrictProperty);
    }

    @Test
    void loadsValidPropertiesThroughBridge() throws Exception {
        try (InputStream input = resourceStream("trustgate/valid-log4j.properties")) {
            final BuiltConfiguration built =
                    new Log4j1ConfigurationParser().buildConfigurationBuilder(input).build();
            built.initialize();
            final Configuration configuration = built;
            assertEquals(Level.INFO, configuration.getRootLogger().getLevel());
            final ConsoleAppender appender = configuration.getAppender("Console");
            assertNotNull(appender);
            final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example");
            assertEquals(Level.DEBUG, loggerConfig.getLevel());
            configuration.stop();
        }
    }

    @Test
    void integrationTestLoadsValidPropertiesViaFactory() throws URISyntaxException {
        final URL configLocation = ClassLoader.getSystemResource("trustgate/valid-log4j.properties");
        assertNotNull(configLocation);
        final Configuration configuration =
                new Log4j1ConfigurationFactory().getConfiguration(null, "test", configLocation.toURI());
        assertNotNull(configuration);
        try {
            configuration.start();
            assertEquals(Level.INFO, configuration.getRootLogger().getLevel());
            assertNotNull(configuration.getAppender("Console"));
        } finally {
            configuration.stop();
        }
    }

    @Test
    void rejectsMaliciousPropertyKeyAndUriValue() throws Exception {
        try (InputStream input = resourceStream("trustgate/malicious-log4j.properties")) {
            final BuiltConfiguration built =
                    new Log4j1ConfigurationParser().buildConfigurationBuilder(input).build();
            built.initialize();
            final Configuration configuration = built;
            assertNull(configuration.getProperties().get("custom%key"));
            assertNotEquals("ldap://evil.example.com/exploit", configuration.getProperties().get("deployment.url"));
            assertNotNull(configuration.getAppender("Console"));
            configuration.stop();
        }
    }

    private static InputStream resourceStream(final String resource) {
        final InputStream input = Log4j1ConfigurationParserTrustGateTest.class.getClassLoader().getResourceAsStream(resource);
        assertNotNull(input, resource);
        return input;
    }

    private static void restoreProperty(final String name, final String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }
}
