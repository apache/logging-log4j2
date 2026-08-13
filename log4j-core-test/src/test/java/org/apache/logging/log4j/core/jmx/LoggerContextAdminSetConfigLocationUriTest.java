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
package org.apache.logging.log4j.core.jmx;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression for caller-owned stream cleanup in
 * {@link LoggerContextAdmin#setConfigLocationUri(String)}.
 *
 * <p>{@code ConfigurationSource(InputStream, File/URL)} leaves stream ownership
 * with the caller. Built-in factories close {@code getInputStream()} when they
 * consume it, but that is not a substitute for caller-side try-with-resources
 * when a factory path never reads the stream.
 */
class LoggerContextAdminSetConfigLocationUriTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void resetConfigurationFactory() {
        ConfigurationFactory.resetConfigurationFactory();
    }

    @Test
    void setConfigLocationUri_loadsValidFileAndReconfigures() throws Exception {
        final Path config = tempDir.resolve("log4j2-test.xml");
        writeConfig(config, "C");

        final LoggerContext ctx = new LoggerContext("jmx-admin-stream-test");
        final LoggerContextAdmin admin = new LoggerContextAdmin(ctx, Runnable::run);

        assertDoesNotThrow(
                () -> admin.setConfigLocationUri(config.toAbsolutePath().toString()));
        assertTrue(ctx.getConfiguration().getAppenders().containsKey("C"));
    }

    @Test
    void setConfigLocationUri_fileUrlFormAlsoLoads() throws Exception {
        final Path config = tempDir.resolve("log4j2-url.xml");
        writeConfig(config, "FromUrl");

        final LoggerContext ctx = new LoggerContext("jmx-admin-file-url-test");
        final LoggerContextAdmin admin = new LoggerContextAdmin(ctx, Runnable::run);
        final String fileUrl = config.toUri().toURL().toString();
        assertDoesNotThrow(() -> admin.setConfigLocationUri(fileUrl));
        assertTrue(ctx.getConfiguration().getAppenders().containsKey("FromUrl"));
    }

    @Test
    void setConfigLocationUri_rejectsBlankLocation() {
        final LoggerContext ctx = new LoggerContext("jmx-admin-blank");
        final LoggerContextAdmin admin = new LoggerContextAdmin(ctx, Runnable::run);
        assertThrows(IllegalArgumentException.class, () -> admin.setConfigLocationUri(""));
        assertThrows(IllegalArgumentException.class, () -> admin.setConfigLocationUri(null));
    }

    /**
     * Portable red-green for caller try-with-resources: install a factory that
     * returns without consuming the source stream, capture that stream, and
     * assert it is closed after {@code setConfigLocationUri} returns.
     */
    @Test
    void setConfigLocationUri_closesCallerOwnedStreamWhenFactoryDoesNotConsumeIt() throws Exception {
        final Path config = tempDir.resolve("log4j2-unconsumed.xml");
        writeConfig(config, "Unconsumed");

        final AtomicReference<InputStream> input = new AtomicReference<>();
        ConfigurationFactory.setConfigurationFactory(new ConfigurationFactory() {
            @Override
            public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
                // Capture the caller-owned stream without consuming or closing it.
                input.set(source.getInputStream());
                return new DefaultConfiguration();
            }

            @Override
            protected String[] getSupportedTypes() {
                return new String[] {"*"};
            }
        });

        final LoggerContext ctx = new LoggerContext("jmx-admin-stream-close");
        final LoggerContextAdmin admin = new LoggerContextAdmin(ctx, Runnable::run);
        admin.setConfigLocationUri(config.toAbsolutePath().toString());

        final InputStream captured = input.get();
        assertNotNull(captured);
        // Closed streams throw on read; an unclosed stream would still read.
        assertThrows(IOException.class, captured::read);
    }

    private static void writeConfig(final Path config, final String appenderName) throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Configuration status=\"OFF\">\n"
                + "  <Appenders><Console name=\"" + appenderName + "\" target=\"SYSTEM_OUT\"/></Appenders>\n"
                + "  <Loggers><Root level=\"error\"><AppenderRef ref=\"" + appenderName + "\"/></Root></Loggers>\n"
                + "</Configuration>\n";
        Files.write(config, xml.getBytes(StandardCharsets.UTF_8));
    }
}
