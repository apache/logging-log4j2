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
package org.apache.logging.log4j.core.config.plugins;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Class Description goes here.
 */
public class LegacyPluginTest {

    private static final String CONFIG_FILE = "legacy-plugins.xml";

    @Test
    public void testLegacy() throws Exception {
        LoggerContext context = Configurator.initialize("LegacyTest", null, CONFIG_FILE);
        assertNotNull("No Logger Context", context);
        Configuration configuration = ((org.apache.logging.log4j.core.LoggerContext) context).getConfiguration();
        assertNotNull("No Configuration", configuration);
        assertTrue("Incorrect Configuration class " + configuration.getClass().getName(),
                configuration instanceof XmlConfiguration);
        for (Map.Entry<String, Appender> entry : configuration.getAppenders().entrySet()) {
            if (entry.getKey().equalsIgnoreCase("console")) {
                Layout layout = entry.getValue().getLayout();
                assertNotNull("No layout for Console Appender");
                String name = layout.getClass().getSimpleName();
                assertTrue("Incorrect Layout class. Expected LogstashLayout, Actual " + name,
                        name.equals("LogstashLayout"));
            } else if (entry.getKey().equalsIgnoreCase("customConsole")) {
                Layout layout = entry.getValue().getLayout();
                assertNotNull("No layout for CustomConsole Appender");
                String name = layout.getClass().getSimpleName();
                assertTrue("Incorrect Layout class. Expected CustomConsoleLayout, Actual " + name,
                        name.equals("CustomConsoleLayout"));
            }
        }
    }
}
