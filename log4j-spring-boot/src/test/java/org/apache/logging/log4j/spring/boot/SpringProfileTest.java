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

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests basic condition processing.
 */
public class SpringProfileTest {

    static final String CONFIG = "target/test-classes/log4j2-springProfile.xml";
    static LoggerContext loggerContext;
    static MockEnvironment env;

    @BeforeClass
    public static void before() {
        loggerContext = (LoggerContext) LogManager.getContext(false);
        env = new MockEnvironment();
        loggerContext.putObject(Log4j2CloudConfigLoggingSystem.ENVIRONMENT_KEY, env);
    }


    @Test
    public void prodTest() {
        env.setActiveProfiles("prod");
        loggerContext.setConfigLocation(new File(CONFIG).toURI());
        assertNotNull(loggerContext);
        Appender app = loggerContext.getConfiguration().getAppender("Out");
        assertNotNull(app);
        assertTrue(app instanceof ListAppender);
    }

    @Test
    public void devTest() {
        env.setActiveProfiles("dev");
        loggerContext.setConfigLocation(new File(CONFIG).toURI());
        assertNotNull(loggerContext);
        Appender app = loggerContext.getConfiguration().getAppender("Out");
        assertNotNull(app);
        assertTrue(app instanceof ConsoleAppender);
    }
}
