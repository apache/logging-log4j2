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
package org.apache.logging.log4j.async.logger;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests LOG4J2-807.
 */
@Tag("async")
@Tag("sleepy")
public class AsyncRootReloadTest {

    private static final String ISSUE_CONFIG =
            "/" + AsyncRootReloadTest.class.getName().replace('.', '/') + ".xml";

    @TempLoggingDir
    private Path loggingPath;

    @Test
    @LoggerContextSource
    public void testLog4j2_807(final Logger logger) throws InterruptedException, URISyntaxException {
        final URL url = AsyncRootReloadTest.class.getResource(ISSUE_CONFIG);
        final File configFile = FileUtils.fileFromUri(url.toURI());

        logger.info("Log4j configured, will be reconfigured in approx. 5 sec");

        configFile.setLastModified(System.currentTimeMillis());

        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            logger.info("Log4j waiting for reconfiguration");
        }
    }
}
