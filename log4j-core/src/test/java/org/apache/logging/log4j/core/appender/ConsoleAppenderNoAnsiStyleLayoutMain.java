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
 * java -classpath log4j-core\target\test-classes;log4j-core\target\classes;log4j-api\target\classes;%HOME%\.m2\repository\org\fusesource\jansi\jansi\1.14\jansi-1.14.jar; org.apache.logging.log4j.core.appender.ConsoleAppenderNoAnsiStyleLayoutMain log4j-core/target/test-classes/log4j2-console-style-ansi.xml
 * </pre>
 */
public class ConsoleAppenderNoAnsiStyleLayoutMain {

    private static final Logger LOG = LogManager.getLogger(ConsoleAppenderNoAnsiStyleLayoutMain.class);

    private static void logThrowableFromMethod() {
        LOG.error("Error message.", new IOException("test"));
    }

    public static void main(final String[] args) {
        final String config = args.length == 0 ? "target/test-classes/log4j2-console-style-no-ansi.xml" : args[0];
        test(args, config);
    }

    static void test(final String[] args, final String config) {
        // System.out.println(System.getProperty("java.class.path"));
        try (final LoggerContext ctx = Configurator.initialize(ConsoleAppenderNoAnsiStyleLayoutMain.class.getName(),
                config)) {
            LOG.fatal("Fatal message.");
            LOG.error("Error message.");
            LOG.warn("Warning message.");
            LOG.info("Information message.");
            LOG.debug("Debug message.");
            LOG.trace("Trace message.");
            logThrowableFromMethod();
            // This will log the stack trace as well:
            final IOException ioException = new IOException("test");
            LOG.error("Error message {}", "Hi", ioException);
            final Throwable t = new IOException("test suppressed");
            t.addSuppressed(new IOException("test suppressed 2", ioException));
            LOG.error("Error message {}, suppressed?", "Hi", t);
            LOG.error("Error message {}, suppressed?", "Hi", new IOException("test", t));
        }
    }

}
