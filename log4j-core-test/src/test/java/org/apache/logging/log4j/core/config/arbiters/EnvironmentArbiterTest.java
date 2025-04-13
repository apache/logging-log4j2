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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Tests system property condition processing.
 */
class EnvironmentArbiterTest {

    private static final String CONFIG = "log4j2-environmentArbiters.xml";

    @Test
    @SetEnvironmentVariable(key = "ENV", value = "prod")
    @LoggerContextSource(CONFIG)
    void prodTest(final LoggerContext loggerContext) {
        final Appender app = loggerContext.getConfiguration().getAppender("Out");
        assertNotNull(app);
        assertInstanceOf(ListAppender.class, app);
    }

    @Test
    @SetEnvironmentVariable(key = "ENV", value = "dev")
    @LoggerContextSource(CONFIG)
    void devTest(final LoggerContext loggerContext) {
        final Appender app = loggerContext.getConfiguration().getAppender("Out");
        assertNotNull(app);
        assertInstanceOf(ConsoleAppender.class, app);
    }
}
