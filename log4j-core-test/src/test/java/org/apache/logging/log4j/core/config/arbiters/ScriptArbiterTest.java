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
package org.apache.logging.log4j.core.config.arbiters;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests basic condition processing.
 */
public class ScriptArbiterTest {

    static final String CONFIG = "log4j2-scriptArbiters.xml";
    static LoggerContext loggerContext = null;

    @BeforeAll
    public static void beforeClass() {
        System.setProperty(Constants.SCRIPT_LANGUAGES, "Groovy, Javascript");
    }

    @AfterEach
    public void after() {
        loggerContext.stop();
        loggerContext = null;
    }

    @Test
    public void prodTest() {
        System.setProperty("env", "prod");
        loggerContext = Configurator.initialize(null, CONFIG);
        assertNotNull(loggerContext);
        final Appender app = loggerContext.getConfiguration().getAppender("Out");
        assertNotNull(app);
        assertTrue(app instanceof ListAppender);
    }

    @Test
    public void devTest() {
        System.setProperty("env", "dev");
        loggerContext = Configurator.initialize(null, CONFIG);
        assertNotNull(loggerContext);
        final Appender app = loggerContext.getConfiguration().getAppender("Out");
        assertNotNull(app);
        assertTrue(app instanceof ConsoleAppender);
    }
}
