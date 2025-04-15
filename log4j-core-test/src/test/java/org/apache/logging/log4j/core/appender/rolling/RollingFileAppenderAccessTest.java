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
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.jupiter.api.Test;

class RollingFileAppenderAccessTest {

    /**
     * Not a real test, just make sure we can compile access to the typed manager.
     *
     * @throws IOException
     */
    @Test
    void testAccessManagerWithBuilder() throws IOException {
        try (final LoggerContext ctx = LoggerContext.getContext(false)) {
            final Configuration config = ctx.getConfiguration();
            final File file = File.createTempFile("RollingFileAppenderAccessTest", ".tmp");
            file.deleteOnExit();
            // @formatter:off
            final RollingFileAppender appender = RollingFileAppender.newBuilder()
                    .withFileName(file.getCanonicalPath())
                    .withFilePattern("FilePattern")
                    .setName("Name")
                    .withPolicy(OnStartupTriggeringPolicy.createPolicy(1))
                    .setConfiguration(config)
                    .build();
            // @formatter:on
            final RollingFileManager manager = appender.getManager();
            // Since the RolloverStrategy and TriggeringPolicy are immutable, we could also use generics to type their
            // access.
            assertNotNull(manager.getRolloverStrategy());
            assertNotNull(manager.getTriggeringPolicy());
        }
    }

    /**
     * Not a real test, just make sure we can compile access to the typed manager.
     *
     * @throws IOException
     */
    @Test
    void testAccessManagerWithStrings() throws IOException {
        try (final LoggerContext ctx = LoggerContext.getContext(false)) {
            final Configuration config = ctx.getConfiguration();
            final File file = File.createTempFile("RollingFileAppenderAccessTest", ".tmp");
            file.deleteOnExit();
            final RollingFileAppender appender = RollingFileAppender.createAppender(
                    file.getCanonicalPath(),
                    "FilePattern",
                    null,
                    "Name",
                    null,
                    null,
                    null,
                    OnStartupTriggeringPolicy.createPolicy(1),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    config);
            final RollingFileManager manager = appender.getManager();
            // Since the RolloverStrategy and TriggeringPolicy are immutable, we could also use generics to type their
            // access.
            assertNotNull(manager.getRolloverStrategy());
            assertNotNull(manager.getTriggeringPolicy());
        }
    }
}
