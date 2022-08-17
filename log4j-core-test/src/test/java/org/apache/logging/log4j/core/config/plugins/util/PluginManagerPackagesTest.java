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
package org.apache.logging.log4j.core.config.plugins.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class PluginManagerPackagesTest {
    private static final Path TEST_SOURCE = Path.of("target", "test-classes", "customplugin", "FixedStringLayout.java");
    private static LoggerContext ctx;

    @AfterAll
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        if (ctx != null) {
            ctx.reconfigure();
        }
        StatusLogger.getLogger().reset();
    }

    @AfterAll
    public static void afterClass() throws Exception {
        Files.deleteIfExists(TEST_SOURCE);
        Files.deleteIfExists(TEST_SOURCE.resolveSibling("FixedStringLayout.class"));
    }

    @Test
    public void test() throws Exception {

        // To ensure our custom plugin is NOT included in the log4j plugin metadata file,
        // we make sure the class does not exist until after the build is finished.
        // So we don't create the custom plugin class until this test is run.
        final Path orig = TEST_SOURCE.resolveSibling("FixedStringLayout.java.source");
        final Path source = Files.move(orig, TEST_SOURCE);
        compile(source);
        Files.move(source, orig);

        // load the compiled class
        Class.forName("customplugin.FixedStringLayout");

        // now that the custom plugin class exists, we load the config
        // with the packages element pointing to our custom plugin
        ctx = Configurator.initialize("Test1", "customplugin/log4j2-741.xml");
        Configuration config = ctx.getConfiguration();
        ListAppender listAppender = config.getAppender("List");

        final Logger logger = LogManager.getLogger(PluginManagerPackagesTest.class);
        logger.info("this message is ignored");

        assertThat(listAppender.getMessages()).hasSize(1).hasSameElementsAs(List.of("abc123XYZ"));
    }

    static void compile(final Path path) {
        // set up compiler
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final List<String> errors = new ArrayList<>();
        assertDoesNotThrow(() -> {
            try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
                final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromPaths(List.of(path));
                String classPath = System.getProperty("jdk.module.path");
                List<String> options = new ArrayList<>();
                if (Strings.isNotBlank(classPath)) {
                    options.add("-classpath");
                    options.add(classPath);
                }
                options.add("-proc:none");
                // compile generated source
                compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();

                // check we don't have any compilation errors
                for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        errors.add(String.format("Compile error: %s%n", diagnostic.getMessage(Locale.getDefault())));
                    }
                }
            }
        });
        assertThat(errors).isEmpty();
    }
}
