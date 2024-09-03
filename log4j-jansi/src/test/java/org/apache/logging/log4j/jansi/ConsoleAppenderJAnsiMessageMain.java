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
package org.apache.logging.log4j.jansi;

import static org.jline.jansi.Ansi.Color.CYAN;
import static org.jline.jansi.Ansi.Color.RED;
import static org.jline.jansi.Ansi.ansi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.util.NetUtils;

/**
 * Shows how to use ANSI escape codes to color messages. Each message is printed to the console in color, but the rest
 * of the log entry (time stamp for example) is in the default color for that console.
 * <p>
 * Running from a Windows command line from the root of the project:
 * </p>
 *
 * <pre>
 * mvn exec:java -Dexec.mainClass=org.apache.logging.log4j.jansi.ConsoleAppenderJAnsiMessageMain
 * </pre>
 */
public class ConsoleAppenderJAnsiMessageMain {

    public static void main(final String[] args) throws URISyntaxException {
        new ConsoleAppenderJAnsiMessageMain().test(args);
    }

    public void test(final String[] args) throws URISyntaxException {
        final URI config = args == null || args.length == 0
                ? ConsoleAppenderJAnsiMessageMain.class
                        .getResource("/ConsoleAppenderJAnsiMessageMain.xml")
                        .toURI()
                : NetUtils.toURI(args[0]);
        try (final LoggerContext ctx =
                Configurator.initialize(getClass().getName(), getClass().getClassLoader(), config)) {
            final Logger logger = LogManager.getLogger();
            logger.info(ansi().fg(RED).a("Hello").fg(CYAN).a(" World").reset());
            // JAnsi format:
            // logger.info("@|red Hello|@ @|cyan World|@");
            for (final Entry<Object, Object> entry : System.getProperties().entrySet()) {
                logger.info("@|KeyStyle {}|@ = @|ValueStyle {}|@", entry.getKey(), entry.getValue());
            }
        }
    }
}
