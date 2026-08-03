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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.sun.management.UnixOperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * with the caller. Built-in factories ({@code XmlConfiguration},
 * {@code JsonConfiguration}, {@code PropertiesConfigurationFactory}) close
 * {@code getInputStream()} when they consume it, but that is not a substitute
 * for caller-side try-with-resources when a factory path never reads the stream.
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
     * Fails without the try-with-resources around {@code getConfiguration}/{@code start}:
     * a factory that never consumes the stream leaves the {@code FileInputStream} open.
     * With the fix, the caller always closes it.
     */
    @Test
    void setConfigLocationUri_closesCallerOwnedStreamWhenFactoryDoesNotConsumeIt() throws Exception {
        final Path config = tempDir.resolve("log4j2-unconsumed.xml");
        writeConfig(config, "Unconsumed");

        ConfigurationFactory.setConfigurationFactory(new ConfigurationFactory() {
            @Override
            public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
                // Intentionally leave source.getInputStream() unconsumed and unclosed.
                return new DefaultConfiguration();
            }

            @Override
            protected String[] getSupportedTypes() {
                return new String[] {"*"};
            }
        });

        final long expectedFdCount = getOpenFileDescriptorCount();
        final LoggerContext ctx = new LoggerContext("jmx-admin-stream-close");
        final LoggerContextAdmin admin = new LoggerContextAdmin(ctx, Runnable::run);
        admin.setConfigLocationUri(config.toAbsolutePath().toString());

        // UNIX: unclosed FileInputStream would leave an extra descriptor.
        assertEquals(expectedFdCount, getOpenFileDescriptorCount());
        // Windows: an open FileInputStream locks the file against delete.
        try {
            Files.delete(config);
        } catch (final Exception e) {
            fail(e);
        }
    }

    private static void writeConfig(final Path config, final String appenderName) throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Configuration status=\"OFF\">\n"
                + "  <Appenders><Console name=\"" + appenderName + "\" target=\"SYSTEM_OUT\"/></Appenders>\n"
                + "  <Loggers><Root level=\"error\"><AppenderRef ref=\"" + appenderName + "\"/></Root></Loggers>\n"
                + "</Configuration>\n";
        Files.write(config, xml.getBytes(StandardCharsets.UTF_8));
    }

    private static long getOpenFileDescriptorCount() {
        final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof UnixOperatingSystemMXBean) {
            return ((UnixOperatingSystemMXBean) os).getOpenFileDescriptorCount();
        }
        return 0;
    }
}
