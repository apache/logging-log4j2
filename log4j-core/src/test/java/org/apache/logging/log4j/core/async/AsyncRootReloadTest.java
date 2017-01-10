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
package org.apache.logging.log4j.core.async;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.categories.AsyncLoggers;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

/**
 * Tests LOG4J2-807.
 */
@Category(AsyncLoggers.class)
public class AsyncRootReloadTest {

    private static final String ISSUE = "LOG4J2-807";
    private static final String ISSUE_CONFIG = ISSUE + ".xml";
    private static final String LOG = "target/" + ISSUE + ".log";
    private static final String RESOURCE = "classpath:" + ISSUE_CONFIG;

    @ClassRule
    public static RuleChain rules = RuleChain.outerRule(new CleanFiles(LOG)).around(new LoggerContextRule(RESOURCE));

    @Test
    public void testLog4j2_807() throws InterruptedException, URISyntaxException {
        final URL url = AsyncRootReloadTest.class.getResource("/" + ISSUE_CONFIG);
        final File configFile = FileUtils.fileFromUri(url.toURI());

        final Logger logger = LogManager.getLogger(AsyncRootReloadTest.class);
        logger.info("Log4j configured, will be reconfigured in aprox. 5 sec");

        configFile.setLastModified(System.currentTimeMillis());

        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            logger.info("Log4j waiting for reconfiguration");
        }
    }
}
