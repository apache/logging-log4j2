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
package org.apache.logging.log4j.core.appender;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Shows how to use ANSI escape codes to color messages. Each message is printed to the console in color, but the rest
 * of the log entry (time stamp for example) is in the default color for that console.
 */
public class ConsoleAppenderAnsiStyleNameLayoutMain {

    private static final Logger LOG = LogManager.getLogger(ConsoleAppenderAnsiStyleNameLayoutMain.class);

    public static void main(final String[] args) {
        try (final LoggerContext ignored = Configurator.initialize(
                ConsoleAppenderAnsiMessagesMain.class.getName(),
                "target/test-classes/log4j2-console-style-name-ansi.xml")) {
            LOG.fatal("Fatal message.");
            LOG.error("Error message.");
            LOG.warn("Warning message.");
            LOG.info("Information message.");
            LOG.debug("Debug message.");
            LOG.trace("Trace message.");
            LOG.error("Error message.", new IOException("test"));
        }
    }
}
