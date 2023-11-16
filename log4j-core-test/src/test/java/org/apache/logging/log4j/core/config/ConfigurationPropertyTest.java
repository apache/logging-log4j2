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
package org.apache.logging.log4j.core.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("functional")
public class ConfigurationPropertyTest {

    private LoggerContext loggerContext;

    @AfterEach
    public void afterEach() {
        if (loggerContext != null) {
            LogManager.shutdown(loggerContext);
        }
        System.clearProperty("log4j2.configurationFile");
        System.clearProperty("log4j.configurationFile");
    }

    @Test
    public void testInitializeFromSystemProperty() {
        System.setProperty("log4j2.configurationFile", "src/test/resources/log4j-list.xml");
        loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration configuration = loggerContext.getConfiguration();
        assertNotNull(configuration, "Null configuration");
        final Appender app = configuration.getAppender("List");
        assertNotNull(app, " Could not locate List Appender");
    }
}
