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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression for stream ownership in {@link LoggerContextAdmin#setConfigLocationUri(String)}.
 * The method buffers configuration bytes and closes the underlying File/URL stream before
 * ConfigurationFactory runs.
 */
class LoggerContextAdminSetConfigLocationUriTest {

    @TempDir
    Path tempDir;

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

    private static void writeConfig(final Path config, final String appenderName) throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<Configuration status=\"OFF\">\n"
                + "  <Appenders><Console name=\"" + appenderName + "\" target=\"SYSTEM_OUT\"/></Appenders>\n"
                + "  <Loggers><Root level=\"error\"><AppenderRef ref=\"" + appenderName + "\"/></Root></Loggers>\n"
                + "</Configuration>\n";
        Files.write(config, xml.getBytes(StandardCharsets.UTF_8));
    }
}
