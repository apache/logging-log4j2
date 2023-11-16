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
package org.apache.logging.log4j.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("functional")
public class EventParameterMemoryLeakTest {

    @BeforeAll
    public static void beforeClass() {
        System.setProperty("log4j2.enable.threadlocals", "true");
        System.setProperty("log4j2.enable.direct.encoders", "true");
        System.setProperty("log4j2.is.webapp", "false");
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "EventParameterMemoryLeakTest.xml");
    }

    @Test
    @SuppressWarnings("UnusedAssignment") // parameter set to null to allow garbage collection
    public void testParametersAreNotLeaked() throws Exception {
        final File file = new File("target", "EventParameterMemoryLeakTest.log");
        assertTrue(!file.exists() || file.delete(), "Deleted old file before test");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        final CountDownLatch latch = new CountDownLatch(1);
        Object parameter = new ParameterObject("paramValue", latch);
        log.info("Message with parameter {}", parameter);
        log.info(parameter);
        log.info("test", new ObjectThrowable(parameter));
        log.info("test {}", "hello", new ObjectThrowable(parameter));
        parameter = null;
        CoreLoggerContexts.stopLoggerContext(file);
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        final String line1 = reader.readLine();
        final String line2 = reader.readLine();
        final String line3 = reader.readLine();
        final String line4 = reader.readLine();
        final String line5 = reader.readLine();
        reader.close();
        file.delete();
        assertThat(line1, containsString("Message with parameter paramValue"));
        assertThat(line2, containsString("paramValue"));
        assertThat(line3, containsString("paramValue"));
        assertThat(line4, containsString("paramValue"));
        assertNull(line5, "Expected only three lines");
        final GarbageCollectionHelper gcHelper = new GarbageCollectionHelper();
        gcHelper.run();
        try {
            assertTrue(latch.await(30, TimeUnit.SECONDS), "Parameter should have been garbage collected");
        } finally {
            gcHelper.close();
        }
    }

    private static final class ParameterObject {
        private final String value;
        private final CountDownLatch latch;

        ParameterObject(final String value, final CountDownLatch latch) {
            this.value = value;
            this.latch = latch;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        protected void finalize() throws Throwable {
            latch.countDown();
            super.finalize();
        }
    }

    private static final class ObjectThrowable extends RuntimeException {
        private final Object object;

        ObjectThrowable(final Object object) {
            super(String.valueOf(object));
            this.object = object;
        }

        @Override
        public String toString() {
            return "ObjectThrowable " + object;
        }
    }
}
