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

import java.nio.file.Files;
import java.nio.file.Paths;
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
 * mvn -Dtest=org.apache.logging.log4j.core.appender.ConsoleAppenderAnsiXExceptionMain test
 * </pre>
 *
 * or, on Windows:
 *
 * <pre>
 * java -classpath log4j-core\target\test-classes;log4j-core\target\classes;log4j-api\target\classes org.apache.logging.log4j.core.appender.ConsoleAppenderAnsiXExceptionMain log4j-core/src/test/resources/log4j2-console-xex-ansi.xml
 * </pre>
 *
 */
public class ConsoleAppenderAnsiXExceptionMain {

    public static void main(final String[] args) {
        new ConsoleAppenderAnsiXExceptionMain().test(args);
    }

    /**
     * This is a @Test method to make it easy to run from a command line with {@code mvn -Dtest=FQCN test}
     */
    @Test
    public void test() {
        test(null);
    }

    public void test(final String[] args) {
        final String config =
                args == null || args.length == 0 ? "target/test-classes/log4j2-console-xex-ansi.xml" : args[0];
        final LoggerContext ctx = Configurator.initialize(ConsoleAppenderAnsiMessagesMain.class.getName(), config);
        final Logger logger = LogManager.getLogger(ConsoleAppenderAnsiXExceptionMain.class);
        try {
            Files.getFileStore(Paths.get("?BOGUS?"));
        } catch (final Exception e) {
            final IllegalArgumentException logE = new IllegalArgumentException("Bad argument foo", e);
            logger.error("Gotcha!", logE);
        } finally {
            Configurator.shutdown(ctx);
        }
    }
}
