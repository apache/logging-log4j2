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
package org.apache.logging.log4j.core.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.junit.Tags;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.core.util.SystemNanoClock;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag(Tags.ASYNC_LOGGERS)
class AsyncLoggerNanoTimeTest {

    @BeforeAll
    static void beforeClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, AsyncLoggerContextSelector.class.getName());
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "NanoTimeToFileTest.xml");
    }

    @AfterAll
    static void afterClass() {
        System.setProperty(Constants.LOG4J_CONTEXT_SELECTOR, Strings.EMPTY);
    }

    @Test
    void testAsyncLogUsesNanoTimeClock() throws Exception {
        final File file = new File("target", "NanoTimeToFileTest.log");
        // System.out.println(f.getAbsolutePath());
        file.delete();
        final AsyncLogger log = (AsyncLogger) LogManager.getLogger("com.foo.Bar");
        final long before = System.nanoTime();
        log.info("Use actual System.nanoTime()");
        assertInstanceOf(SystemNanoClock.class, log.getNanoClock(), "using SystemNanoClock");

        final long DUMMYNANOTIME = -53;
        log.getContext().getConfiguration().setNanoClock(new DummyNanoClock(DUMMYNANOTIME));
        log.updateConfiguration(log.getContext().getConfiguration());

        // trigger a new nano clock lookup
        log.updateConfiguration(log.getContext().getConfiguration());

        log.info("Use dummy nano clock");
        assertInstanceOf(DummyNanoClock.class, log.getNanoClock(), "using SystemNanoClock");

        CoreLoggerContexts.stopLoggerContext(file); // stop async thread

        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        final String line2 = reader.readLine();
        // System.out.println(line1);
        // System.out.println(line2);
        reader.close();
        file.delete();

        assertNotNull(line1, "line1");
        assertNotNull(line2, "line2");
        final String[] line1Parts = line1.split(" AND ");
        assertEquals("Use actual System.nanoTime()", line1Parts[2]);
        assertEquals(line1Parts[0], line1Parts[1]);
        final long loggedNanoTime = Long.parseLong(line1Parts[0]);
        assertTrue(loggedNanoTime - before < TimeUnit.SECONDS.toNanos(1), "used system nano time");

        final String[] line2Parts = line2.split(" AND ");
        assertEquals("Use dummy nano clock", line2Parts[2]);
        assertEquals(String.valueOf(DUMMYNANOTIME), line2Parts[0]);
        assertEquals(String.valueOf(DUMMYNANOTIME), line2Parts[1]);
    }
}
