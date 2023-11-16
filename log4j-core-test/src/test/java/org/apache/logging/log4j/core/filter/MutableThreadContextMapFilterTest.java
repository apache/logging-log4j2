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
package org.apache.logging.log4j.core.filter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junitpioneer.jupiter.RetryingTest;

/**
 * Unit test for simple App.
 */
public class MutableThreadContextMapFilterTest implements MutableThreadContextMapFilter.FilterConfigUpdateListener {

    static final String CONFIG = "log4j2-mutableFilter.xml";
    static LoggerContext loggerContext = null;
    static File targetFile = new File("target/test-classes/testConfig.json");
    static Path target = targetFile.toPath();
    CountDownLatch updated = new CountDownLatch(1);

    @AfterEach
    public void after() {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ioe) {
            // Ignore this.
        }
        ThreadContext.clearMap();
        loggerContext.stop();
        loggerContext = null;
    }

    @RetryingTest(maxAttempts = 5, suspendForMs = 10)
    public void filterTest() throws Exception {
        System.setProperty("configLocation", "target/test-classes/testConfig.json");
        ThreadContext.put("loginId", "rgoers");
        Path source = new File("target/test-classes/emptyConfig.json").toPath();
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        final long fileTime = targetFile.lastModified() - 1000;
        assertTrue(targetFile.setLastModified(fileTime));
        loggerContext = Configurator.initialize(null, CONFIG);
        assertNotNull(loggerContext);
        final Appender app = loggerContext.getConfiguration().getAppender("List");
        assertNotNull(app);
        assertTrue(app instanceof ListAppender);
        final MutableThreadContextMapFilter filter =
                (MutableThreadContextMapFilter) loggerContext.getConfiguration().getFilter();
        assertNotNull(filter);
        filter.registerListener(this);
        final Logger logger = loggerContext.getLogger("Test");
        logger.debug("This is a test");
        Assertions.assertEquals(0, ((ListAppender) app).getEvents().size());
        source = new File("target/test-classes/filterConfig.json").toPath();
        String msg = null;
        boolean copied = false;
        for (int i = 0; i < 5 && !copied; ++i) {
            Thread.sleep(100 + (100 * i));
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                copied = true;
            } catch (Exception ex) {
                msg = ex.getMessage();
            }
        }
        assertTrue(copied, "File not copied: " + msg);
        assertNotEquals(fileTime, targetFile.lastModified());
        if (!updated.await(5, TimeUnit.SECONDS)) {
            fail("File update was not detected");
        }
        logger.debug("This is a test");
        Assertions.assertEquals(1, ((ListAppender) app).getEvents().size());
    }

    @Override
    public void onEvent() {
        updated.countDown();
    }
}
