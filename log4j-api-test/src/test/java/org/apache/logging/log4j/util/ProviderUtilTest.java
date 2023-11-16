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
package org.apache.logging.log4j.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.test.TestLoggerContext;
import org.junit.jupiter.api.Test;

public class ProviderUtilTest {

    @Test
    public void complexTest() throws Exception {
        final File file = new File("target/classes");
        final ClassLoader classLoader =
                new URLClassLoader(new URL[] {file.toURI().toURL()});
        final Worker worker = new Worker();
        worker.setContextClassLoader(classLoader);
        worker.start();
        worker.join();
        assertTrue(worker.context instanceof TestLoggerContext, "Incorrect LoggerContext");
    }

    private static class Worker extends Thread {
        LoggerContext context = null;

        @Override
        public void run() {
            context = LogManager.getContext(false);
        }
    }
}
