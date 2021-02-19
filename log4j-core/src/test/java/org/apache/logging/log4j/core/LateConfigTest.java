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
package org.apache.logging.log4j.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("functional")
public class LateConfigTest {

    private static final String CONFIG = "target/test-classes/log4j-test1.xml";
    private static LoggerContext context;

    @BeforeAll
    public static void setupClass() {
        context = LoggerContext.getContext(false);
    }

    @AfterAll
    public static void tearDownClass() {
        Configurator.shutdown(context);
        StatusLogger.getLogger().reset();
    }    

    @Test
    public void testReconfiguration() throws Exception {
        final Configuration cfg = context.getConfiguration();
        assertThat(cfg).describedAs("No configuration").isNotNull();
        assertTrue(cfg instanceof DefaultConfiguration, "Not set to default configuration");
        final File file = new File(CONFIG);
        final LoggerContext loggerContext = LoggerContext.getContext(null, false, file.toURI());
        assertThat(loggerContext).describedAs("No Logger Context").isNotNull();
        final Configuration newConfig = loggerContext.getConfiguration();
        assertThat(newConfig).describedAs("Configuration not reset").isNotSameAs(cfg);
        assertTrue(newConfig instanceof XmlConfiguration, "Reconfiguration failed");
        context = LoggerContext.getContext(false);
        final Configuration sameConfig = context.getConfiguration();
        assertThat(sameConfig).describedAs("Configuration should not have been reset").isSameAs(newConfig);
    }
}

