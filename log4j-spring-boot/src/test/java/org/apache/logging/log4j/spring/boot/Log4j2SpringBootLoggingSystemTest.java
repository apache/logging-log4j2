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
package org.apache.logging.log4j.spring.boot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;

public class Log4j2SpringBootLoggingSystemTest {

    @Test
    public void getStandardConfigLocations() {
        final String customLog4j2Location = "classpath:my_custom_log4j2.properties";
        final LoggerContext lc =
                LogManager.getContext(); // Initialize LogManager to here to prevent a failure trying to
        // initialize it from StatusLogger.
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, customLog4j2Location);
        final Log4j2SpringBootLoggingSystem cloudLoggingSystem =
                new Log4j2SpringBootLoggingSystem(this.getClass().getClassLoader());
        final List<String> standardConfigLocations = Arrays.asList(cloudLoggingSystem.getStandardConfigLocations());
        assertTrue(standardConfigLocations.contains(customLog4j2Location));
    }

    @Test
    @SetSystemProperty(key = Log4j2SpringBootLoggingSystem.LOG4J2_DISABLE_CLOUD_CONFIG_LOGGING_SYSTEM, value = "true")
    public void testUseLog4j2LoggingSystem() {
        final LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
        assertTrue(loggingSystem.getClass().equals(Log4J2LoggingSystem.class));
    }

    @Test
    public void testLoggingSystemEnabled() {
        final LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
        assertTrue(loggingSystem.getClass().equals(Log4j2SpringBootLoggingSystem.class));
    }
}
