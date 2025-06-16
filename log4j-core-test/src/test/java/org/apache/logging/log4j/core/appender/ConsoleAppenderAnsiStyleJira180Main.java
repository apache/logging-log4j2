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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Tests <a href="https://issues.apache.org/jira/browse/LOG4J2-180">LOG4J2-180</a>
 * <p>
 * Running from a Windows command line from the root of the project:
 * </p>
 *
 * <pre>
 * java -classpath log4j-core\target\test-classes;log4j-core\target\classes;log4j-api\target\classes org.apache.logging.log4j.core.appender.ConsoleAppenderAnsiStyleJira180Main log4j-core/target/test-classes/log4j2-180.xml
 * </pre>
 */
public class ConsoleAppenderAnsiStyleJira180Main {

    private static final Logger LOG = LogManager.getLogger(ConsoleAppenderAnsiStyleJira180Main.class);

    public static void main(final String[] args) {
        final String config = args.length == 0 ? "target/test-classes/log4j2-180.xml" : args[0];
        try (final LoggerContext ignored =
                Configurator.initialize(ConsoleAppenderAnsiMessagesMain.class.getName(), config)) {
            LOG.fatal("Fatal message.");
            LOG.error("Error message.");
            LOG.warn("Warning message.");
            LOG.info("Information message.");
            LOG.debug("Debug message.");
            LOG.trace("Trace message.");
            try {
                throw new NullPointerException();
            } catch (final Exception e) {
                LOG.error("Error message.", e);
                LOG.catching(Level.ERROR, e);
            }
        }
    }
}
