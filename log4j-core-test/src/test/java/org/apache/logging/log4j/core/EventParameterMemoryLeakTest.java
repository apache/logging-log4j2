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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.ref.Cleaner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("functional")
@SetSystemProperty(key = "log4j2.enable.direct.encoders", value = "true")
@SetSystemProperty(key = "log4j2.configurationFile", value = "EventParameterMemoryLeakTest.xml")
public class EventParameterMemoryLeakTest {

    @Test
    @SuppressWarnings("UnusedAssignment") // parameter set to null to allow garbage collection
    public void testParametersAreNotLeaked() throws Exception {
        final File file = new File("target", "EventParameterMemoryLeakTest.log");
        assertTrue(!file.exists() || file.delete(), "Deleted old file before test");

        final Logger log = LogManager.getLogger("com.foo.Bar");
        CountDownLatch latch = new CountDownLatch(1);
        final Cleaner cleaner = Cleaner.create();
        Object parameter = new ParameterObject("paramValue");
        cleaner.register(parameter, latch::countDown);
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
        try (GarbageCollectionHelper gcHelper = new GarbageCollectionHelper()) {
            gcHelper.run();
            assertTrue(latch.await(30, TimeUnit.SECONDS), "Parameter should have been garbage collected");
        }
    }

    private static final class ParameterObject {
        private final String value;
        ParameterObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final class ObjectThrowable extends RuntimeException {
        private final Object object;

        ObjectThrowable(Object object) {
            super(String.valueOf(object));
            this.object = object;
        }

        @Override
        public String toString() {
            return "ObjectThrowable " + object;
        }
    }
}
