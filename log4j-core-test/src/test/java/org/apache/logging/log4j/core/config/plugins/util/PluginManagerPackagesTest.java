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
package org.apache.logging.log4j.core.config.plugins.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.test.Compiler;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

public class PluginManagerPackagesTest {
    private static Configuration config;
    private static ListAppender listAppender;
    private static LoggerContext ctx;

    @AfterAll
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        ctx.reconfigure();
        StatusLogger.getLogger().reset();
    }

    @Test
    public void test() throws Exception {

        // To ensure our custom plugin is NOT included in the log4j plugin metadata file,
        // we make sure the class does not exist until after the build is finished.
        // So we don't create the custom plugin class until this test is run.
        final URL resource = PluginManagerPackagesTest.class.getResource("/customplugin/FixedStringLayout.java.source");
        assertThat(resource).isNotNull();
        final File orig = new File(resource.toURI());
        final File f = new File(orig.getParentFile(), "FixedStringLayout.java");
        assertDoesNotThrow(() -> FileUtils.copyFile(orig, f));
        compile(f);
        assertDoesNotThrow(() -> FileUtils.delete(f));

        // load the compiled class
        Class.forName("customplugin.FixedStringLayout");

        // now that the custom plugin class exists, we load the config
        // with the packages element pointing to our custom plugin
        ctx = Configurator.initialize("Test1", "customplugin/log4j2-741.xml");
        config = ctx.getConfiguration();
        listAppender = config.getAppender("List");

        final Logger logger = LogManager.getLogger(PluginManagerPackagesTest.class);
        logger.info("this message is ignored");

        final List<String> messages = listAppender.getMessages();
        assertEquals(1, messages.size(), messages.toString());
        assertEquals("abc123XYZ", messages.get(0));
    }

    static void compile(final File f) throws IOException {
        // compile generated source
        // (switch off annotation processing: no need to create Log4j2Plugins.dat)
        Compiler.compile(f, "-proc:none");
    }
}
