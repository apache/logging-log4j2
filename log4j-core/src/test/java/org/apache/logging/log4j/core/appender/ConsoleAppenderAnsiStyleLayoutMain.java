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
import org.junit.Test;

/**
 * Shows how to use ANSI escape codes to color messages. Each message is printed to the console in color, but the rest
 * of the log entry (time stamp for example) is in the default color for that console.
 * <p>
 * Running from a Windows command line from the root of the project:
 * </p>
 *
 * <pre>
 * mvn -Dtest=org.apache.logging.log4j.core.appender.ConsoleAppenderAnsiStyleLayoutMain test
 * </pre>
 * or:
 * <pre>
 * java -classpath log4j-core\target\test-classes;log4j-core\target\classes;log4j-api\target\classes;%HOME%\.m2\repository\org\fusesource\jansi\jansi\1.14\jansi-1.14.jar; org.apache.logging.log4j.core.appender.ConsoleAppenderAnsiStyleLayoutMain log4j-core/target/test-classes/log4j2-console-style-ansi.xml
 * </pre>
 * 
 */
public class ConsoleAppenderAnsiStyleLayoutMain {

    public static void main(final String[] args) {
        new ConsoleAppenderAnsiStyleLayoutMain().test(args);
    }

    /**
     * This is a @Test method to make it easy to run from a command line with {@code mvn -Dtest=FQCN test}
     */
    @Test
    public void test() {
        test(null);
    }

    public void test(final String[] args) {
        // System.out.println(System.getProperty("java.class.path"));
        final String config = args == null || args.length == 0 ? "target/test-classes/log4j2-console-style-ansi.xml"
                : args[0];
        try (final LoggerContext ctx = Configurator.initialize(ConsoleAppenderAnsiMessagesMain.class.getName(), config)) {
            final Logger logger = LogManager.getLogger(ConsoleAppenderAnsiStyleLayoutMain.class);
            logger.fatal("Fatal message.");
            logger.error("Error message.");
            logger.warn("Warning message.");
            logger.info("Information message.");
            logger.debug("Debug message.");
            logger.trace("Trace message.");
            logger.error("Error message.", new IOException("test"));
        }
    }

}
