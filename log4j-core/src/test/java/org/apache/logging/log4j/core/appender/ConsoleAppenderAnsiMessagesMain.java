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
package org.apache.logging.log4j.core.appender;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Shows how to use ANSI escape codes to color messages. Each message is printed to the console in color, but the rest
 * of the log entry (time stamp for example) is in the default color for that console.
 * <p>
 * Running from a Windows command line from the root of the project:
 * </p>
 * 
 * <pre>
 * java -classpath log4j-core\target\test-classes;log4j-core\target\classes;log4j-api\target\classes;%HOME%\.m2\repository\org\fusesource\jansi\jansi\1.14\jansi-1.14.jar; org.apache.logging.log4j.core.appender.ConsoleAppenderAnsiMessagesMain log4j-core/target/test-classes/log4j2-console.xml
 * </pre>
 */
public class ConsoleAppenderAnsiMessagesMain {

    private static final Logger LOG = LogManager.getLogger(ConsoleAppenderAnsiMessagesMain.class);

    public static void main(final String[] args) {
        try (final LoggerContext ctx = Configurator.initialize(ConsoleAppenderAnsiMessagesMain.class.getName(),
                "target/test-classes/log4j2-console.xml")) {
            LOG.fatal("\u001b[1;35mFatal message.\u001b[0m");
            LOG.error("\u001b[1;31mError message.\u001b[0m");
            LOG.warn("\u001b[0;33mWarning message.\u001b[0m");
            LOG.info("\u001b[0;32mInformation message.\u001b[0m");
            LOG.debug("\u001b[0;36mDebug message.\u001b[0m");
            LOG.trace("\u001b[0;30mTrace message.\u001b[0m");
            LOG.error("\u001b[1;31mError message.\u001b[0m", new IOException("test"));
        }
    }

}
