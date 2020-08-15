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
package org.apache.logging.log4j.spring.boot;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class Log4j2CloudConfigLoggingSystemTest {

    @Test
    public void getStandardConfigLocations() {
        String customLog4j2Location = "classpath:my_custom_log4j2.properties";
        LoggerContext lc = LogManager.getContext(); // Initialize LogManager to here to prevent a failure trying to initialize it from StatusLogger.
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, customLog4j2Location);
        Log4j2CloudConfigLoggingSystem cloudLoggingSystem = new Log4j2CloudConfigLoggingSystem(this.getClass().getClassLoader());
        List<String> standardConfigLocations = Arrays.asList(cloudLoggingSystem.getStandardConfigLocations());
        assertTrue(standardConfigLocations.contains(customLog4j2Location));

    }
}
