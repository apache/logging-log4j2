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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Tests https://issues.apache.org/jira/browse/LOG4J2-272
 * <p>
 * Running from a Windows command line from the root of the project:
 * </p>
 * <pre>
 * java -classpath log4j-core\target\test-classes;log4j-core\target\classes;log4j-api\target\classes;%HOME%\.m2\repository\org\fusesource\jansi\jansi\1.14\jansi-1.14.jar; org.apache.logging.log4j.core.appender.ConsoleAppenderAnsiStyleJira272Main log4j-core/target/test-classes/log4j2-272.xml
 * </pre>
 */
public class ConsoleAppenderAnsiStyleJira272Main {

    private static final Logger LOG = LogManager.getLogger(ConsoleAppenderAnsiStyleJira272Main.class);

    public static void main(final String[] args) {
        // System.out.println(System.getProperty("java.class.path"));
        final String config = args.length == 0 ? "target/test-classes/log4j2-272.xml" : args[0];
        try (final LoggerContext ctx = Configurator.initialize(ConsoleAppenderAnsiMessagesMain.class.getName(), config)) {
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
            LOG.warn("this is ok \n And all \n this have only\t\tblack colour \n and here is colour again?");
            LOG.info("Information message.");
        }
    }

}
