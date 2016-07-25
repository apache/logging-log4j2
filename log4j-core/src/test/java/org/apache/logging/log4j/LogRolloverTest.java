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
package org.apache.logging.log4j;

import java.io.File;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

/**
 *
 */
public class LogRolloverTest {

    private static final String CONFIG = "src/test/resources/rollover-test.xml";

    public static void main(final String[] args) throws Exception {
        final File file = new File(CONFIG);
        try (final LoggerContext ctx = Configurator.initialize("LogTest", LogRolloverTest.class.getClassLoader(),
                file.toURI())) {
            final Logger logger = LogManager.getLogger("TestLogger");

            for (long i = 0;; i += 1) {
                logger.debug("Sequence: " + i);
                Thread.sleep(250);
            }
        }
    }
}
